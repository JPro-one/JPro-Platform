package one.jpro.platform.sticky;

import com.jpro.webapi.WebAPI;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import one.jpro.jmemorybuddy.CleanupDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * The scroll-aware pinning for a single {@link Node}, realised on the web side through a
 * compositor scroll-timeline override. One instance owns one node's override lifecycle: it
 * reparents the node into a per-scene overlay, leaves a layout-mirroring placeholder in the
 * node's flow slot, server-pins the node (for picking and as a no-compositor fallback), and
 * overrides the DOM visual with an {@code animation-timeline: scroll()} animation so scrolling
 * stays smooth without a JavaFX layout pass per scroll event.
 * <p>
 * It builds only on the JPro Viewport API ({@link WebAPI#browserViewport()} /
 * {@link WebAPI#documentBounds()}) and needs no core change.
 * <p>
 * When running as a desktop application the {@link WebAPI} consumer never fires, so installation
 * is a no-op and the node keeps its normal flow positioning.
 * <p>
 * <strong>Anchoring.</strong> The {@link ScrollAnchor} resolves the horizontal and vertical axes
 * independently. The vertical axis drives the scroll-timeline keyframe (pin line, ride, and, for
 * bounded sticky, release); the horizontal axis is a constant baked into the keyframe (the page does
 * not scroll horizontally). {@link ScrollAnchor.Mode#STRETCH} resizes the node to span the axis;
 * {@link ScrollPosition#FIXED} is the degenerate pin (from scroll 0, no ride and, being
 * viewport-anchored, no containment release).
 *
 * @author Tobias Horak
 */
final class ScrollOverride implements ScrollImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScrollOverride.class);

    /** Sequence for unique per-node JS registry keys. */
    private static final AtomicLong KEY_SEQ = new AtomicLong();

    /** Slack (px) below which the server pin is treated as sitting at the natural flow position. */
    private static final double STUCK_EPS = 0.5;

    private final Node node;
    private final ScrollPosition position;
    private final ScrollAnchor anchor;
    /** Explicit containment override for STICKY; {@code null} defaults to the original parent. */
    private final Node within;
    /** Pin/unpin transition sink (the node's stuck channels); {@code null} for FIXED or unobserved. */
    private final Consumer<Boolean> stuckSink;
    /** Last stuck value pushed to {@link #stuckSink}, so we only fire on change. */
    private boolean lastStuck;
    private final String jsKey = "n" + KEY_SEQ.incrementAndGet();
    /**
     * Stack key (type tier + source order): the overlay is kept sorted by it (see {@link StickyOverlay})
     * so FIXED paints above STICKY and, within a tier, the stacking/paint order follows the order
     * {@code setScrollPosition} was called rather than the async order installs complete. Assigned in
     * the constructor, once {@link #position} is known.
     */
    private final long stackOrder;

    // Resolved at install time.
    private WebAPI webapi;
    private Group overlay;
    private Region placeholder;
    private Pane originalParent;
    private int originalIndex = -1;
    private Parent root;
    /** The containing block that bounds a STICKY pin (null => document-long / unbounded). */
    private Node container;

    // Reactive plumbing. A single listener re-syncs geometry on any relevant change.
    private final InvalidationListener relayout = obs -> sync();
    private ChangeListener<Scene> sceneWaiter;
    /** Fires teardown when the placeholder (and thus the route subtree) leaves the scene. */
    private ChangeListener<Scene> placeholderSceneWaiter;
    /** Defers attach while a superseded application still has the node mounted in an overlay. */
    private ChangeListener<Parent> settleWaiter;
    private boolean installedCompositor = false;
    private String lastSig = "";
    private boolean torndown = false;

    ScrollOverride(Node node, ScrollPosition position, ScrollAnchor anchor, Node within,
                   Consumer<Boolean> stuckSink) {
        this.node = node;
        this.position = position;
        this.anchor = anchor;
        this.within = within;
        this.stuckSink = stuckSink;
        this.stackOrder = StickyOverlay.nextStackOrder(position);
    }

    /**
     * Installs the override. Only meaningful under JPro: on desktop the {@link WebAPI} consumer
     * never fires and the node keeps its normal flow positioning.
     */
    @Override
    public void install() {
        WebAPI.getWebAPI(node, this::onWebAPI);
    }

    private void onWebAPI(WebAPI webapi) {
        if (torndown) {
            return;
        }
        this.webapi = webapi;
        if (node.getScene() != null) {
            attach();
        } else {
            // The node may be positioned before it enters a scene. Attach once it does.
            sceneWaiter = (obs, old, scene) -> {
                if (scene != null) {
                    node.sceneProperty().removeListener(sceneWaiter);
                    sceneWaiter = null;
                    attach();
                }
            };
            node.sceneProperty().addListener(sceneWaiter);
        }
    }

    private void attach() {
        if (torndown) {
            return;
        }
        final Parent parent = node.getParent();
        // A superseded application (rapid re-apply or scene churn) may still have the node in an overlay
        // when this async attach fires. Attaching now would treat that overlay as the flow slot, so wait
        // until the node settles back into its real flow parent. One-shot.
        if (StickyOverlay.isOverlay(parent)) {
            if (settleWaiter == null) {
                settleWaiter = (obs, old, p) -> {
                    if (!torndown && p != null && !StickyOverlay.isOverlay(p)) {
                        node.parentProperty().removeListener(settleWaiter);
                        settleWaiter = null;
                        attach();
                    }
                };
                node.parentProperty().addListener(settleWaiter);
                LOGGER.debug("jpro-sticky[{}]: node still in an overlay; deferring attach until it settles", jsKey);
            }
            return;
        }
        if (!(parent instanceof Pane)) {
            LOGGER.warn("jpro-sticky: node's parent is {} (not a Pane); cannot pin {}. Node stays in flow.",
                    parent == null ? "null" : parent.getClass().getSimpleName(), node);
            return;
        }
        // Resolve the overlay from the node's nearest registered host (else the scene root) BEFORE
        // reparenting, while the node's real parent chain still leads up to that host.
        final Group ov = StickyOverlay.overlayForNode(node);
        if (ov == null) {
            LOGGER.warn("jpro-sticky: no overlay host for {} (outside a scene, or host not a Pane/Group)."
                    + " Node stays in flow.", node);
            return;
        }

        this.overlay = ov;
        this.originalParent = (Pane) parent;
        this.originalIndex = originalParent.getChildren().indexOf(node);
        if (originalIndex < 0) {
            return;
        }
        this.root = node.getScene().getRoot();

        // STICKY is bounded by its containing block: the explicit `within` if given, else the node's
        // original parent. FIXED is viewport-anchored, so it ignores containment.
        this.container = (position == ScrollPosition.FIXED) ? null
                : (within != null ? within : originalParent);

        placeholder = new Region();
        placeholder.setMaxWidth(Double.MAX_VALUE);
        // Mirror the node's layout constraints and width onto the placeholder so the flow slot doesn't
        // shift. Its height comes later, in sync().
        Placeholders.mirror(node, placeholder);

        // Swap node -> placeholder in flow, then mount the node into the overlay. insertSorted keeps it
        // ordered by stackOrder, so paint order follows source order, not install order. StickyOverlay
        // documents the full ordering contract.
        originalParent.getChildren().set(originalIndex, placeholder);
        node.setManaged(false);
        StickyOverlay.insertSorted(overlay, node, stackOrder);
        node.applyCss();

        // Re-sync on anything that moves the pin: the placeholder's geometry, the browser viewport
        // (moves under native scroll, and sizes end/center/stretch anchors), the document extent
        // (resizes the unbounded pin range), and for bounded sticky the container's bottom.
        placeholder.layoutBoundsProperty().addListener(relayout);
        placeholder.localToSceneTransformProperty().addListener(relayout);
        webapi.browserViewport().addListener(relayout);
        root.layoutBoundsProperty().addListener(relayout);
        if (container != null && container != root) {
            container.layoutBoundsProperty().addListener(relayout);
            container.localToSceneTransformProperty().addListener(relayout);
        }

        // The placeholder rides the flow, so it leaves the scene on route unmount. The reparented node
        // lives in the persistent overlay and never does. Tear down when the placeholder's scene goes
        // null, re-checking next pulse to skip a same-pulse detach/reattach.
        placeholderSceneWaiter = (obs, old, scene) -> {
            if (scene == null && !torndown) {
                Platform.runLater(() -> {
                    if (!torndown && placeholder != null && placeholder.getScene() == null) {
                        uninstall();
                    }
                });
            }
        };
        placeholder.sceneProperty().addListener(placeholderSceneWaiter);

        registerCleanup();
        LOGGER.debug("jpro-sticky[{}]: attached (overlay={}, parent={})", jsKey,
                overlay.getId(), originalParent.getClass().getSimpleName());
        sync();
    }

    /**
     * Recomputes the node's pinned position and (re-)emits the compositor keyframes when the
     * geometry signature changes, never per scroll event (that is the compositor's job).
     */
    private void sync() {
        if (torndown || placeholder == null) {
            return;
        }
        final double flowW = placeholder.getWidth();
        if (flowW <= 0) {
            LOGGER.debug("jpro-sticky[{}]: sync skipped, placeholder width={}", jsKey, flowW);
            return;
        }
        final boolean fixed = position == ScrollPosition.FIXED;

        final Rectangle2D vp = webapi.getBrowserViewport();
        final double viewportTop = (vp == null) ? 0.0 : vp.getMinY();
        final double viewportW = (vp == null) ? 0.0 : vp.getWidth();
        final double viewportH = (vp == null) ? 0.0 : vp.getHeight();

        // End/center/stretch anchors need a real viewport size. Skip until one is known. The
        // browserViewport listener re-syncs once it arrives.
        if (AnchorGeometry.needsAvailableSize(anchor) && (viewportW <= 0 || viewportH <= 0)) {
            LOGGER.debug("jpro-sticky[{}]: sync skipped, viewport size {}x{}", jsKey, viewportW, viewportH);
            return;
        }

        // The node's flow anchor (scroll-independent: native scroll moves the browser, not the scene).
        final Point2D flowTopLeft = placeholder.localToScene(0, 0);
        final double flowX = flowTopLeft.getX();
        final double flowTop = flowTopLeft.getY();

        // Resolve the anchor against the browser viewport (the same resolver the desktop path uses
        // against the scene), so web and desktop pin identical geometry from the same anchor.
        final AnchorGeometry g = AnchorGeometry.resolve(anchor, viewportW, viewportH,
                AnchorGeometry.naturalWidth(node), w -> AnchorGeometry.naturalHeight(node, w),
                flowX, flowW, fixed);
        final double nodeW = g.nodeW;
        final double nodeH = g.nodeH;
        final double x = g.x;
        final double y0 = g.y0;

        node.resize(nodeW, nodeH);
        // FIXED is out of flow: the placeholder reserves no vertical space. STICKY keeps its slot.
        placeholder.setPrefHeight(fixed ? 0 : nodeH);

        // natTop drives the keyframe 'from'. STICKY rides the flow from its natural top. FIXED pins
        // from the very top (natTop == y0 makes the compositor's sPin 0, so no ride).
        final double natTop = fixed ? y0 : flowTop;

        // STICKY release limit: the containing block's bottom minus the node height
        // (containerBottom - nodeH); -1 (unbounded) for FIXED or a page-spanning container.
        final double relLimitServer = releaseLimit(nodeH);

        // Server-side pin (also the no-compositor fallback, and what picking sees): clamp to the
        // viewport pin line while pinned, ride the flow before, and honour the release limit.
        double serverY = fixed ? (viewportTop + y0) : Math.max(flowTop, viewportTop + y0);
        if (!fixed && relLimitServer >= 0) {
            serverY = Math.min(serverY, relLimitServer);
        }
        final Point2D local = overlay.sceneToLocal(x, serverY);
        node.setLayoutX(local.getX());
        node.setLayoutY(local.getY());

        // The overlay may not sit at the scene/document origin: when it lives under a registered host
        // (e.g. a popup container nested in the route) its top-left is offset down the document. The
        // compositor transform is relative to the overlay's own DOM box, so its endpoints are baked in
        // host-local space (scene-y minus this offset) while the scroll-range math stays document-space.
        final double hostOffsetY = overlay.localToScene(0, 0).getY();

        // Publish the pin state (STICKY only): stuck iff the applied server pin differs from the
        // natural flow top, the same rule FXStickyImpl uses (appear != natural), so both paths agree.
        // Fidelity is the browserViewport() sync cadence, not per-frame (the compositor drives motion).
        if (!fixed && stuckSink != null) {
            final boolean nowStuck = Math.abs(serverY - flowTop) > STUCK_EPS;
            if (nowStuck != lastStuck) {
                lastStuck = nowStuck;
                stuckSink.accept(nowStuck);
            }
        }

        final double docH = root.getLayoutBounds().getHeight();
        final String sig = natTop + "|" + y0 + "|" + local.getX() + "|" + relLimitServer + "|"
                + nodeW + "|" + nodeH + "|" + docH + "|" + hostOffsetY;

        if (!installedCompositor) {
            // Install inline on the first sync with a real width. The node's DOM peer may still be
            // unregistered at this instant, but the injected script resolves it via its own retry
            // loop (see installCompositor), so no server-side deferral (a runLater pulse) is needed.
            installCompositor(local.getX(), natTop, y0, relLimitServer, hostOffsetY);
            installedCompositor = true;
            lastSig = sig;
            LOGGER.debug("jpro-sticky[{}]: compositor installed (w={}, natTop={}, y0={}, hostOffsetY={})",
                    jsKey, nodeW, natTop, y0, hostOffsetY);
        } else if (!sig.equals(lastSig)) {
            installCompositor(local.getX(), natTop, y0, relLimitServer, hostOffsetY);
            lastSig = sig;
        }
    }

    /**
     * The scene-y at which a bounded STICKY node releases (rides up out of its containing block):
     * {@code containerBottom - nodeH}. Returns {@code -1} (unbounded, document-long) for FIXED, for
     * an absent container, or when the container is the scene root (a page-spanning header).
     */
    private double releaseLimit(double nodeH) {
        if (container == null || container == root) {
            return -1.0;
        }
        final double containerBottom =
                container.localToScene(0, container.getLayoutBounds().getHeight()).getY();
        return containerBottom - nodeH;
    }

    /**
     * (Re-)installs the scroll-timeline animation that pins the node. The animation is realised
     * entirely inside an injected {@code <style>} sheet: the {@code @keyframes} plus a rule that
     * binds them to the node via its stable {@code [jpro-id]} attribute selector. The renderer
     * positions the node with inline {@code style.transform}, but a running CSS animation outranks
     * inline styles in the cascade, so the animation overrides that pin with a compositor-driven
     * pure function of scroll. Assumes an svg scale of 1 (true for native-scrolling pages).
     * <p>
     * {@code natTop} is the keyframe 'from' (the flow top for sticky, the pin line for fixed);
     * {@code y0} is the viewport pin line; {@code relLimitServer} is the scene-y release point
     * ({@code < 0} = unbounded, resolved browser-side to the document extent). All three are in
     * scene/document space; {@code hostOffsetY} is the overlay host's document-y origin, subtracted
     * from the transform endpoints only (they are relative to the overlay's own DOM box) while the
     * scroll-range math stays in document space, so a non-scene-root host shifts nothing but the pin.
     * <p>
     * <strong>Robust against the JPro readiness race.</strong> Two things make first install
     * reliable on fresh loads. First, the {@code <style>} and keyframes are written unconditionally,
     * with no dependency on the node's DOM peer existing yet. Second, the element reference
     * ({@code jpro.getValue(n)}) <em>throws</em> until JPro's render pulse has registered the node,
     * so it is resolved inside a {@code requestAnimationFrame} retry loop guarded by try/catch;
     * once resolved, its {@code jpro-id} is cached and the binding is emitted as a selector rule.
     * Because JPro re-emits {@code jpro-id} on every render of the node, that rule re-applies by
     * itself after any DOM re-render or reconnect, with nothing to re-push from the server.
     */
    private void installCompositor(double x, double natTop, double y0, double relLimitServer, double hostOffsetY) {
        final String d = webapi.getElement(node).getName();
        final String js =
                "(function(){\n" +
                "  var reg = (window.__jproStickyC = window.__jproStickyC || {});\n" +
                "  var st = reg['" + jsKey + "'] = reg['" + jsKey + "'] || {};\n" +
                "  if(!st.style){ st.style = document.createElement('style');\n" +
                "    st.style.setAttribute('data-jpro-sticky','" + jsKey + "'); document.head.appendChild(st.style); }\n" +
                "  st.key = 'jpro-sticky-" + jsKey + "';\n" +
                // Latest geometry, baked in server-side; render() reads these so a re-install (on a
                // geometry-signature change) just updates them and rewrites the sheet.
                "  st.x = " + x + "; st.natTop = " + natTop + "; st.inset = " + y0 + "; st.relServer = " + relLimitServer + "; st.hostOffsetY = " + hostOffsetY + ";\n" +
                "  st.render = function(){\n" +
                "    if(st.jid == null) return;\n" +
                // Document extent (scrollHeight), NOT the scroll max (scrollHeight - clientHeight):
                // the latter folds in viewport height, leaving the unbounded range stale on resize.
                "    var docExtent = document.documentElement.scrollHeight;\n" +
                "    var relLimit = (st.relServer < 0) ? (docExtent + st.inset) : st.relServer;\n" +
                "    var sPin = st.natTop - st.inset; if(sPin < 0) sPin = 0;\n" +
                "    var sRel = relLimit - st.inset; if(sRel < sPin + 1) sRel = sPin + 1;\n" +
                // Transform endpoints are relative to the overlay's own DOM box, so shift them into
                // host-local space; sPin/sRel above stay in document/scroll space (host-independent).
                "    var fromY = st.natTop - st.hostOffsetY;\n" +
                "    var toY = relLimit - st.hostOffsetY;\n" +
                "    st.style.textContent = '@keyframes ' + st.key +\n" +
                "      '{from{transform:translate(' + st.x + 'px,' + fromY + 'px);}to{transform:translate(' + st.x + 'px,' + toY + 'px);}}' +\n" +
                "      '[jpro-id=\"' + st.jid + '\"]{' +\n" +
                "      'animation-name:' + st.key + ';' +\n" +
                "      'animation-timing-function:linear;animation-fill-mode:both;animation-duration:auto;' +\n" +
                "      'animation-timeline:scroll(root block);' +\n" +
                "      'animation-range:' + sPin + 'px ' + sRel + 'px;}';\n" +
                "  };\n" +
                // Fast path for a re-install: the node's jpro-id is already known, so just re-render.
                "  if(st.jid != null){ st.render(); return; }\n" +
                // First install: the element ref throws until JPro registers the node's DOM peer, so
                // retry (bounded to ~5s at 60fps) until it resolves, then cache jpro-id and render.
                "  var tries = 0;\n" +
                "  (function resolve(){\n" +
                "    var el = null; try { el = " + d + "; } catch(e){ el = null; }\n" +
                "    if(el && el.getAttribute){ st.jid = el.getAttribute('jpro-id'); st.render(); }\n" +
                "    else if(tries++ < 300){ requestAnimationFrame(resolve); }\n" +
                "  })();\n" +
                "})();";
        webapi.executeScript(js);
    }

    /**
     * Reverses everything this override installed: deregisters listeners, restores the node to
     * its flow slot, and clears the compositor animation and its {@code <style>} element.
     */
    @Override
    public void uninstall() {
        torndown = true;

        if (sceneWaiter != null) {
            node.sceneProperty().removeListener(sceneWaiter);
            sceneWaiter = null;
        }
        if (settleWaiter != null) {
            node.parentProperty().removeListener(settleWaiter);
            settleWaiter = null;
        }
        if (placeholder != null) {
            placeholder.layoutBoundsProperty().removeListener(relayout);
            placeholder.localToSceneTransformProperty().removeListener(relayout);
            if (placeholderSceneWaiter != null) {
                placeholder.sceneProperty().removeListener(placeholderSceneWaiter);
                placeholderSceneWaiter = null;
            }
        }
        if (webapi != null) {
            webapi.browserViewport().removeListener(relayout);
        }
        if (root != null) {
            root.layoutBoundsProperty().removeListener(relayout);
        }
        if (container != null && container != root) {
            container.layoutBoundsProperty().removeListener(relayout);
            container.localToSceneTransformProperty().removeListener(relayout);
        }

        // Restore the node to its flow slot.
        StickyOverlay.remove(overlay, node);
        if (originalParent != null && placeholder != null) {
            final int idx = originalParent.getChildren().indexOf(placeholder);
            if (idx >= 0) {
                originalParent.getChildren().set(idx, node);
            }
            node.setManaged(true);
        }

        // The animation lives entirely in the injected <style> (bound by a [jpro-id] rule, not by
        // inline styles on the element), so dropping that sheet removes the pin without touching the
        // element ref (which may be unresolved at teardown).
        if (webapi != null && installedCompositor) {
            removeCompositorStyle(webapi, jsKey);
        }
    }

    private void registerCleanup() {
        // Runs if the node is GC'd while pinned, dropping the orphaned <style>. Captures only a
        // weak WebAPI ref and the key string, never the node or this override (would pin them).
        final WeakReference<WebAPI> weakWebApi = new WeakReference<>(webapi);
        final String key = jsKey;
        CleanupDetector.onCleanup(node, () -> {
            final WebAPI w = weakWebApi.get();
            if (w != null) {
                removeCompositorStyle(w, key);
            }
        });
    }

    private static void removeCompositorStyle(WebAPI webapi, String jsKey) {
        webapi.executeScript(
                "(function(){\n" +
                "  var reg = window.__jproStickyC; if(!reg) return;\n" +
                "  var st = reg['" + jsKey + "']; if(!st) return;\n" +
                "  if(st.style && st.style.parentNode) st.style.parentNode.removeChild(st.style);\n" +
                "  delete reg['" + jsKey + "'];\n" +
                "})();");
    }
}
