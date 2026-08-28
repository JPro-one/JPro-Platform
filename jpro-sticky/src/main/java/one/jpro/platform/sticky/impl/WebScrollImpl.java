package one.jpro.platform.sticky.impl;

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
import javafx.scene.layout.Region;
import one.jpro.jmemorybuddy.CleanupDetector;
import one.jpro.platform.sticky.ScrollAnchor;
import one.jpro.platform.sticky.ScrollPosition;
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
public final class WebScrollImpl implements ScrollImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebScrollImpl.class);

    /** Sequence for unique per-node JS registry keys. */
    private static final AtomicLong KEY_SEQ = new AtomicLong();

    /** Slack (px) below which the server pin is treated as sitting at the natural flow position. */
    private static final double STUCK_EPS = 0.5;

    private final Node node;
    private final ScrollPosition position;
    private final ScrollAnchor anchor;
    /** Explicit containment override for STICKY, {@code null} defaults to the original parent. */
    private final Node within;
    /** Pin/unpin transition sink (the node's stuck channels); {@code null} for FIXED or unobserved. */
    private final Consumer<Boolean> stuckSink;
    /** Called when the flow slot leaves the scene, so the dispatcher can re-pin on re-entry. */
    private final Runnable onDetach;
    /** Last stuck value pushed to {@link #stuckSink}, so we only fire on change. */
    private boolean lastStuck;
    private final String jsKey = "n" + KEY_SEQ.incrementAndGet();
    /** Stack key ordering this node in the overlay; see {@link StickyOverlay#nextStackOrder}. */
    private final long stackOrder;

    /** The shared reparent-into-overlay mechanic (flow slot, placeholder, overlay). */
    private final OverlayMount mount;

    // resolved at install time
    private WebAPI webapi;
    private Group overlay;
    private Region placeholder;
    private Parent root;
    /** The containing block that bounds a STICKY pin (null => document-long / unbounded). */
    private Node container;

    // reactive plumbing: one listener re-syncs geometry on any relevant change
    private final InvalidationListener relayout = obs -> sync();
    private ChangeListener<Scene> sceneWaiter;
    /** Fires teardown when the placeholder (and thus the route subtree) leaves the scene. */
    private ChangeListener<Scene> placeholderSceneWaiter;
    /** Defers attach while a superseded application still has the node mounted in an overlay. */
    private ChangeListener<Parent> settleWaiter;
    private boolean installedCompositor = false;
    private String lastSig = "";
    private boolean torndown = false;

    public WebScrollImpl(Node node, ScrollPosition position, ScrollAnchor anchor, Node within,
                         Consumer<Boolean> stuckSink, Runnable onDetach) {
        this.node = node;
        this.position = position;
        this.anchor = anchor;
        this.within = within;
        this.stuckSink = stuckSink;
        this.onDetach = onDetach;
        this.stackOrder = StickyOverlay.nextStackOrder(position);
        this.mount = new OverlayMount(node, stackOrder);
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
            // node not in a scene yet, attach when it enters.
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
        // a superseded app (rapid re-apply / scene churn) may still hold the node in an overlay when this
        // async attach fires, which we'd mistake for the flow slot. wait (one-shot) until it settles back.
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
        // lift the node into the overlay, leaving a mirrored placeholder in its flow slot (height set in
        // sync(): FIXED collapses to 0, STICKY reserves node height).
        final Region ph = mount.mount();
        if (ph == null) {
            return; // could not mount (no Pane parent / overlay host), node stays in flow
        }
        this.placeholder = ph;
        this.overlay = mount.overlay();
        this.root = node.getScene().getRoot();

        // STICKY bounded by its containing block (explicit within, else original parent). FIXED is viewport-anchored.
        this.container = (position == ScrollPosition.FIXED) ? null
                : (within != null ? within : mount.originalParent());

        node.applyCss();

        // re-sync on anything that moves the pin: placeholder geometry, browser viewport (native scroll +
        // sizing end/center/stretch anchors), document extent (unbounded pin range), container bottom.
        placeholder.layoutBoundsProperty().addListener(relayout);
        placeholder.localToSceneTransformProperty().addListener(relayout);
        webapi.browserViewport().addListener(relayout);
        root.layoutBoundsProperty().addListener(relayout);
        if (container != null && container != root) {
            container.layoutBoundsProperty().addListener(relayout);
            container.localToSceneTransformProperty().addListener(relayout);
        }

        // placeholder rides the flow, so it leaves the scene on route unmount (the node never does).
        // re-check next pulse to ignore a transient same-pulse detach/reattach.
        placeholderSceneWaiter = (obs, old, scene) -> {
            if (scene == null && !torndown) {
                Platform.runLater(() -> {
                    if (!torndown && placeholder != null && placeholder.getScene() == null) {
                        // hand back to the dispatcher: uninstall this delegate but stay alive to re-pin
                        // if the route returns. fall back to a direct uninstall if unwired.
                        if (onDetach != null) {
                            onDetach.run();
                        } else {
                            uninstall();
                        }
                    }
                });
            }
        };
        placeholder.sceneProperty().addListener(placeholderSceneWaiter);

        registerCleanup();
        LOGGER.debug("jpro-sticky[{}]: attached (overlay={}, parent={})", jsKey,
                overlay.getId(), mount.originalParent().getClass().getSimpleName());
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

        // end/center/stretch anchors need a real viewport size. skip until the browserViewport listener re-syncs.
        if (AnchorGeometry.needsAvailableSize(anchor) && (viewportW <= 0 || viewportH <= 0)) {
            LOGGER.debug("jpro-sticky[{}]: sync skipped, viewport size {}x{}", jsKey, viewportW, viewportH);
            return;
        }

        // node's flow anchor (scroll-independent: native scroll moves the browser, not the scene).
        final Point2D flowTopLeft = placeholder.localToScene(0, 0);
        final double flowX = flowTopLeft.getX();
        final double flowTop = flowTopLeft.getY();

        // resolve the anchor vs the browser viewport (same resolver the desktop path uses vs the scene),
        // so web and desktop pin identical geometry from one anchor.
        final AnchorGeometry g = AnchorGeometry.resolve(anchor, viewportW, viewportH,
                AnchorGeometry.naturalWidth(node), w -> AnchorGeometry.naturalHeight(node, w),
                flowX, flowW, fixed);
        final double nodeW = g.nodeW;
        final double nodeH = g.nodeH;
        final double x = g.x;
        final double y0 = g.y0;

        node.resize(nodeW, nodeH);
        // FIXED is out of flow: placeholder reserves no height. STICKY keeps its slot.
        placeholder.setPrefHeight(fixed ? 0 : nodeH);

        // natTop = keyframe 'from'. STICKY rides the flow from its natural top, FIXED pins from the very
        // top (natTop == y0 makes sPin 0, so no ride).
        final double natTop = fixed ? y0 : flowTop;

        // STICKY release limit = containerBottom - nodeH. -1 = unbounded (FIXED or page-spanning container).
        final double relLimitServer = releaseLimit(nodeH);

        // server-side pin (also the no-compositor fallback + what picking sees): clamp to pin line while
        // pinned, ride flow before, honour the release limit.
        double serverY = fixed ? (viewportTop + y0) : Math.max(flowTop, viewportTop + y0);
        if (!fixed && relLimitServer >= 0) {
            serverY = Math.min(serverY, relLimitServer);
        }
        final Point2D local = overlay.sceneToLocal(x, serverY);
        node.setLayoutX(local.getX());
        node.setLayoutY(local.getY());

        // the overlay may sit offset down the document (under a registered host, e.g. a popup nested in
        // the route). the compositor transform is relative to the overlay's own DOM box, so endpoints bake
        // in host-local space (scene-y minus this offset) while the scroll-range math stays document-space.
        final double hostOffsetY = overlay.localToScene(0, 0).getY();

        // publish pin state (STICKY only): stuck iff the server pin differs from the natural flow top (same
        // rule as ScrollPaneStickyImpl, appear != natural). fidelity = browserViewport() cadence, not per-frame.
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
            // install inline on the first sync with a real width. the node's DOM peer may still be unregistered,
            // but the injected script resolves it via its own retry loop, so no server-side deferral needed.
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
        // mirror FX mouseTransparent to pointer-events:none: unlike desktop FX picking, the reparented
        // node is a real div that would otherwise catch clicks meant for the content beneath it.
        final boolean mouseTransparent = node.isMouseTransparent();
        final String js =
                "(function(){\n" +
                "  var reg = (window.__jproStickyC = window.__jproStickyC || {});\n" +
                "  var st = reg['" + jsKey + "'] = reg['" + jsKey + "'] || {};\n" +
                "  if(!st.style){ st.style = document.createElement('style');\n" +
                "    st.style.setAttribute('data-jpro-sticky','" + jsKey + "'); document.head.appendChild(st.style); }\n" +
                "  st.key = 'jpro-sticky-" + jsKey + "';\n" +
                // latest geometry, baked in server-side. render() reads these so a re-install (on a
                // geometry-sig change) just updates them and rewrites the sheet.
                "  st.x = " + x + "; st.natTop = " + natTop + "; st.inset = " + y0 + "; st.relServer = " + relLimitServer + "; st.hostOffsetY = " + hostOffsetY + ";\n" +
                "  st.pe = " + mouseTransparent + ";\n" +
                "  st.render = function(){\n" +
                "    if(st.jid == null) return;\n" +
                // document extent (scrollHeight), NOT scroll max (scrollHeight - clientHeight): the
                // latter folds in viewport height, leaving the unbounded range stale on resize.
                "    var docExtent = document.documentElement.scrollHeight;\n" +
                "    var relLimit = (st.relServer < 0) ? (docExtent + st.inset) : st.relServer;\n" +
                "    var sPin = st.natTop - st.inset; if(sPin < 0) sPin = 0;\n" +
                "    var sRel = relLimit - st.inset; if(sRel < sPin + 1) sRel = sPin + 1;\n" +
                // transform endpoints are relative to the overlay's own DOM box, so shift into host-local
                // space. sPin/sRel above stay in document/scroll space (host-independent).
                "    var fromY = st.natTop - st.hostOffsetY;\n" +
                "    var toY = relLimit - st.hostOffsetY;\n" +
                "    st.style.textContent = '@keyframes ' + st.key +\n" +
                "      '{from{transform:translate(' + st.x + 'px,' + fromY + 'px);}to{transform:translate(' + st.x + 'px,' + toY + 'px);}}' +\n" +
                "      '[jpro-id=\"' + st.jid + '\"]{' +\n" +
                "      'animation-name:' + st.key + ';' +\n" +
                "      'animation-timing-function:linear;animation-fill-mode:both;animation-duration:auto;' +\n" +
                "      'animation-timeline:scroll(root block);' +\n" +
                "      'animation-range:' + sPin + 'px ' + sRel + 'px;' +\n" +
                "      (st.pe ? 'pointer-events:none;' : '') + '}';\n" +
                "  };\n" +
                // fast path for a re-install: jpro-id already known, just re-render.
                "  if(st.jid != null){ st.render(); return; }\n" +
                // first install: the element ref throws until JPro registers the node's DOM peer, so retry
                // (bounded ~5s at 60fps) until it resolves, then cache jpro-id and render.
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

        // restore the node to its flow slot.
        mount.unmount();

        // the animation lives entirely in the injected <style> (bound by a [jpro-id] rule, not inline styles),
        // so dropping that sheet removes the pin without touching the element ref (may be unresolved at teardown).
        if (webapi != null && installedCompositor) {
            removeCompositorStyle(webapi, jsKey);
        }
    }

    private void registerCleanup() {
        // runs if the node is GC'd while pinned, dropping the orphaned <style>. captures only a weak
        // WebAPI ref + the key string, never the node or this override (would pin them).
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
