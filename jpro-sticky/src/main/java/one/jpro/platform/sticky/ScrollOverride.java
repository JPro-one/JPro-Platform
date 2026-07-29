package one.jpro.platform.sticky;

import com.jpro.webapi.WebAPI;
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
import one.jpro.platform.sticky.ScrollAnchor.Axis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The scroll-aware pinning for a single {@link Node}, realised on the web side through a
 * compositor scroll-timeline override. One instance owns one node's override lifecycle: it
 * reparents the node into a per-scene overlay, leaves a layout-mirroring placeholder in the
 * node's flow slot, server-pins the node (for picking and as a no-compositor fallback), and
 * overrides the DOM visual with an {@code animation-timeline: scroll()} animation so scrolling
 * stays smooth without a JavaFX layout pass per scroll event.
 * <p>
 * This is the JavaFX/JPro port of the FX-scene-graph sticky mechanism proven in the core
 * {@code TestStickyScenegraph} demo (STICKY_DESIGN.md §16/§17). It needs no core change beyond
 * the M1 Viewport API ({@link WebAPI#browserViewport()} / {@link WebAPI#documentBounds()}).
 * <p>
 * When running as a desktop application the {@link WebAPI} consumer never fires, so installation
 * is a no-op and the node keeps its normal flow positioning.
 * <p>
 * <strong>Anchoring.</strong> The {@link ScrollAnchor} resolves the horizontal and vertical axes
 * independently (STICKY_DESIGN.md §18). The vertical axis drives the scroll-timeline keyframe (pin
 * line, ride, and — for bounded sticky — release); the horizontal axis is a constant baked into the
 * keyframe (the page does not scroll horizontally). {@link ScrollAnchor.Mode#STRETCH} resizes the
 * node to span the axis; {@link ScrollPosition#FIXED} is the degenerate pin (from scroll 0, no ride
 * and — being viewport-anchored — no containment release).
 *
 * @author Tobias Horak
 */
final class ScrollOverride {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScrollOverride.class);

    /** Sequence for unique per-node JS registry keys. */
    private static final AtomicLong KEY_SEQ = new AtomicLong();

    /**
     * Monotonic order in which overrides are constructed (i.e. the order {@code setScrollPosition}
     * is called). The overlay is kept sorted by it so the stacking/paint order is deterministic and
     * follows source order, rather than the async order in which installs happen to complete.
     */
    private static final AtomicLong STACK_SEQ = new AtomicLong();

    /** Scene property key under which the shared sticky overlay {@link Group} is cached. */
    private static final Object OVERLAY_KEY = new Object();

    /** Node property key stashing a reparented node's {@link #stackOrder}, read by sibling overrides. */
    private static final Object STACK_ORDER_KEY = new Object();

    private final Node node;
    private final ScrollPosition position;
    private final ScrollAnchor anchor;
    /** Explicit containment override for STICKY; {@code null} defaults to the original parent. */
    private final Node within;
    private final String jsKey = "n" + KEY_SEQ.incrementAndGet();
    private final long stackOrder = STACK_SEQ.getAndIncrement();

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
    private boolean installedCompositor = false;
    private String lastSig = "";
    private boolean torndown = false;

    ScrollOverride(Node node, ScrollPosition position, ScrollAnchor anchor, Node within) {
        this.node = node;
        this.position = position;
        this.anchor = anchor;
        this.within = within;
    }

    /**
     * Installs the override. Only meaningful under JPro: on desktop the {@link WebAPI} consumer
     * never fires and the node keeps its normal flow positioning.
     */
    void install() {
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
            // Deferred until attached: the node may be positioned before it enters a scene.
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
        if (!(parent instanceof Pane)) {
            LOGGER.warn("jpro-sticky: node's parent is {} (not a Pane); cannot pin {}. Node stays in flow.",
                    parent == null ? "null" : parent.getClass().getSimpleName(), node);
            return;
        }
        final Group ov = overlayFor(node.getScene());
        if (ov == null) {
            LOGGER.warn("jpro-sticky: scene root is not a Pane/Group; no overlay host for {}. Node stays in flow.", node);
            return;
        }

        this.overlay = ov;
        this.originalParent = (Pane) parent;
        this.originalIndex = originalParent.getChildren().indexOf(node);
        if (originalIndex < 0) {
            return;
        }
        this.root = node.getScene().getRoot();

        // STICKY is bounded by its containing block (CSS-native): the explicit `within` override if
        // given, else the node's original parent. FIXED is viewport-anchored and ignores containment.
        this.container = (position == ScrollPosition.FIXED) ? null
                : (within != null ? within : originalParent);

        placeholder = new Region();
        placeholder.setMaxWidth(Double.MAX_VALUE);

        // Swap node -> placeholder in the flow, and move the node into the overlay. Insert so the
        // overlay stays sorted by stackOrder: the paint/stacking order then follows the order
        // setScrollPosition was called (source order) instead of the async order installs complete.
        // The library takes no stance on fixed-vs-sticky; a later-declared node paints on top, and
        // viewOrder remains the explicit per-node override (JPro/JavaFX sort by viewOrder first).
        originalParent.getChildren().set(originalIndex, placeholder);
        node.setManaged(false);
        node.getProperties().put(STACK_ORDER_KEY, stackOrder);
        insertIntoOverlaySorted();
        node.applyCss();

        // Signals that require a re-sync: the placeholder's geometry (flow position/size), the
        // browser viewport (the moving signal under native scroll, and the size for end/center/
        // stretch anchors), the document extent (grow/shrink -> the unbounded pin range refreshes),
        // and — for bounded sticky — the container's geometry (its bottom sets the release point).
        placeholder.layoutBoundsProperty().addListener(relayout);
        placeholder.localToSceneTransformProperty().addListener(relayout);
        webapi.browserViewport().addListener(relayout);
        root.layoutBoundsProperty().addListener(relayout);
        if (container != null && container != root) {
            container.layoutBoundsProperty().addListener(relayout);
            container.localToSceneTransformProperty().addListener(relayout);
        }

        registerCleanup();
        LOGGER.debug("jpro-sticky[{}]: attached (overlay={}, parent={})", jsKey,
                overlay.getId(), originalParent.getClass().getSimpleName());
        sync();
    }

    /**
     * Recomputes the node's pinned position and (re-)emits the compositor keyframes when the
     * geometry signature changes — never per scroll event (that is the compositor's job).
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
        final Axis hz = anchor.horizontal();
        final Axis vt = anchor.vertical();

        final Rectangle2D vp = webapi.getBrowserViewport();
        final double viewportTop = (vp == null) ? 0.0 : vp.getMinY();
        final double viewportW = (vp == null) ? 0.0 : vp.getWidth();
        final double viewportH = (vp == null) ? 0.0 : vp.getHeight();

        // End/center/stretch anchors need a real viewport size; skip until one is known (the
        // browserViewport listener re-syncs once it arrives).
        if (needsViewportSize() && (viewportW <= 0 || viewportH <= 0)) {
            LOGGER.debug("jpro-sticky[{}]: sync skipped, viewport size {}x{}", jsKey, viewportW, viewportH);
            return;
        }

        // The node's flow anchor (scroll-independent: native scroll moves the browser, not the scene).
        final Point2D flowTopLeft = placeholder.localToScene(0, 0);
        final double flowX = flowTopLeft.getX();
        final double flowTop = flowTopLeft.getY();

        // --- Horizontal axis: resolve node width and the constant viewport x. ---
        final double nodeW;
        final double x;
        switch (hz.mode) {
            case STRETCH:
                nodeW = Math.max(0, viewportW - hz.start - hz.end);
                x = hz.start;
                break;
            case PIN_START:
                nodeW = naturalWidth();
                x = hz.start;
                break;
            case PIN_END:
                nodeW = naturalWidth();
                x = viewportW - nodeW - hz.end;
                break;
            case CENTER:
                nodeW = naturalWidth();
                x = (viewportW - nodeW) / 2.0 + hz.start;
                break;
            default: // NATURAL: sticky keeps its full flow width; fixed is a natural-width chip.
                nodeW = fixed ? naturalWidth() : flowW;
                x = flowX;
        }

        // --- Vertical axis: resolve node height and the viewport pin line y0. ---
        final double nodeH;
        final double y0;
        switch (vt.mode) {
            case STRETCH:
                nodeH = Math.max(0, viewportH - vt.start - vt.end);
                y0 = vt.start;
                break;
            case PIN_END:
                nodeH = naturalHeight(nodeW);
                y0 = viewportH - nodeH - vt.end;
                break;
            case CENTER:
                nodeH = naturalHeight(nodeW);
                y0 = (viewportH - nodeH) / 2.0 + vt.start;
                break;
            default: // NATURAL / PIN_START: pin line is the start inset (0 for a bare NATURAL).
                nodeH = naturalHeight(nodeW);
                y0 = vt.start;
        }

        node.resize(nodeW, nodeH);
        // FIXED is out of flow: the placeholder reserves no vertical space. STICKY keeps its slot.
        placeholder.setPrefHeight(fixed ? 0 : nodeH);

        // natTop drives the keyframe 'from'. STICKY rides the flow from its natural top; FIXED pins
        // from the very top (natTop == y0 => the compositor's sPin becomes 0, no ride).
        final double natTop = fixed ? y0 : flowTop;

        // STICKY release limit: the containing block's bottom minus the node height (the proven
        // 'containerBottom - h'); -1 (unbounded) for FIXED or a page-spanning container.
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

        final double docH = root.getLayoutBounds().getHeight();
        final String sig = natTop + "|" + y0 + "|" + local.getX() + "|" + relLimitServer + "|"
                + nodeW + "|" + nodeH + "|" + docH;

        if (!installedCompositor) {
            // Install inline on the first sync with a real width. The node's DOM peer may still be
            // unregistered at this instant, but the injected script resolves it via its own retry
            // loop (see installCompositor), so no server-side deferral (a runLater pulse) is needed.
            installCompositor(local.getX(), natTop, y0, relLimitServer);
            installedCompositor = true;
            lastSig = sig;
            LOGGER.debug("jpro-sticky[{}]: compositor installed (w={}, natTop={}, y0={})", jsKey, nodeW, natTop, y0);
        } else if (!sig.equals(lastSig)) {
            installCompositor(local.getX(), natTop, y0, relLimitServer);
            lastSig = sig;
        }
    }

    /** @return whether any axis needs the viewport size (end/center/stretch anchors). */
    private boolean needsViewportSize() {
        return isViewportSized(anchor.horizontal()) || isViewportSized(anchor.vertical());
    }

    private static boolean isViewportSized(Axis axis) {
        return axis.mode == ScrollAnchor.Mode.PIN_END
                || axis.mode == ScrollAnchor.Mode.CENTER
                || axis.mode == ScrollAnchor.Mode.STRETCH;
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

    private double naturalWidth() {
        if (node instanceof Region) {
            final Region r = (Region) node;
            return Math.max(r.prefWidth(-1), r.minWidth(-1));
        }
        return node.getLayoutBounds().getWidth();
    }

    private double naturalHeight(double forWidth) {
        // prefHeight alone ignores minHeight, so a min-constrained node would reserve too little.
        if (node instanceof Region) {
            final Region r = (Region) node;
            return Math.max(r.prefHeight(forWidth), r.minHeight(forWidth));
        }
        return node.getLayoutBounds().getHeight();
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
     * ({@code < 0} = unbounded, resolved browser-side to the document extent).
     * <p>
     * <strong>Robust against the JPro readiness race.</strong> Two things make first install
     * reliable on fresh loads. First, the {@code <style>} and keyframes are written unconditionally,
     * with no dependency on the node's DOM peer existing yet. Second, the element reference
     * ({@code jpro.getValue(n)}) <em>throws</em> until JPro's render pulse has registered the node,
     * so it is resolved inside a {@code requestAnimationFrame} retry loop guarded by try/catch;
     * once resolved, its {@code jpro-id} is cached and the binding is emitted as a selector rule.
     * Because JPro re-emits {@code jpro-id} on every render of the node, that rule re-applies by
     * itself after any DOM re-render or reconnect — nothing to re-push from the server.
     */
    private void installCompositor(double x, double natTop, double y0, double relLimitServer) {
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
                "  st.x = " + x + "; st.natTop = " + natTop + "; st.inset = " + y0 + "; st.relServer = " + relLimitServer + ";\n" +
                "  st.render = function(){\n" +
                "    if(st.jid == null) return;\n" +
                // Document extent (scrollHeight), NOT the scroll max (scrollHeight - clientHeight):
                // the latter folds in viewport height, leaving the unbounded range stale on resize.
                "    var docExtent = document.documentElement.scrollHeight;\n" +
                "    var relLimit = (st.relServer < 0) ? (docExtent + st.inset) : st.relServer;\n" +
                "    var sPin = st.natTop - st.inset; if(sPin < 0) sPin = 0;\n" +
                "    var sRel = relLimit - st.inset; if(sRel < sPin + 1) sRel = sPin + 1;\n" +
                "    st.style.textContent = '@keyframes ' + st.key +\n" +
                "      '{from{transform:translate(' + st.x + 'px,' + st.natTop + 'px);}to{transform:translate(' + st.x + 'px,' + relLimit + 'px);}}' +\n" +
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
    void uninstall() {
        torndown = true;

        if (sceneWaiter != null) {
            node.sceneProperty().removeListener(sceneWaiter);
            sceneWaiter = null;
        }
        if (placeholder != null) {
            placeholder.layoutBoundsProperty().removeListener(relayout);
            placeholder.localToSceneTransformProperty().removeListener(relayout);
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
        if (overlay != null) {
            overlay.getChildren().remove(node);
        }
        node.getProperties().remove(STACK_ORDER_KEY);
        if (originalParent != null && placeholder != null) {
            final int idx = originalParent.getChildren().indexOf(placeholder);
            if (idx >= 0) {
                originalParent.getChildren().set(idx, node);
            }
            node.setManaged(true);
        }

        // The animation lives entirely in the injected <style> (bound by a [jpro-id] rule, not by
        // inline styles on the element), so dropping that sheet removes the pin. No need to touch
        // the element ref here — which would reintroduce the readiness race in teardown.
        if (webapi != null && installedCompositor) {
            removeCompositorStyle(webapi, jsKey);
        }
    }

    private void registerCleanup() {
        // Runs if the node is GC'd while pinned, dropping the orphaned <style>. Captures only a
        // weak WebAPI ref and the key string — never the node or this override (would pin them).
        final WeakReference<WebAPI> weakWebApi = new WeakReference<>(webapi);
        final String key = jsKey;
        CleanupDetector.onCleanup(node, () -> {
            final WebAPI w = weakWebApi.get();
            if (w != null) {
                removeCompositorStyle(w, key);
            }
        });
    }

    /**
     * Adds {@link #node} to the overlay at the index that keeps the overlay's children ordered by
     * {@link #stackOrder} ascending, so a later-declared node ends up later in the list (painted on
     * top). Every overlay child is a reparented sticky/fixed node carrying {@link #STACK_ORDER_KEY}.
     */
    private void insertIntoOverlaySorted() {
        final var kids = overlay.getChildren();
        int insertAt = kids.size();
        for (int i = 0; i < kids.size(); i++) {
            if (stackOrderOf(kids.get(i)) > stackOrder) {
                insertAt = i;
                break;
            }
        }
        kids.add(insertAt, node);
    }

    /** The {@link #stackOrder} stashed on a reparented node, or {@code MIN_VALUE} if absent. */
    private static long stackOrderOf(Node n) {
        final Object v = n.getProperties().get(STACK_ORDER_KEY);
        return (v instanceof Long) ? (Long) v : Long.MIN_VALUE;
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

    /**
     * Returns the scene's shared sticky overlay, creating it on first use. A {@link Group} (not a
     * {@link Pane}): JPro picks server-side in the FX graph, so a Group's pick is the union of its
     * children (empty = transparent to clicks) whereas a full-document Pane would swallow them.
     * Unmanaged and left at layout origin, so it shares the scene's (document) coordinate space.
     *
     * @return the overlay, or {@code null} if the scene root cannot host one
     */
    private static Group overlayFor(Scene scene) {
        final Object existing = scene.getProperties().get(OVERLAY_KEY);
        if (existing instanceof Group) {
            return (Group) existing;
        }
        final Parent sceneRoot = scene.getRoot();
        final Group ov = new Group();
        ov.setManaged(false);
        ov.setId("jpro-sticky-overlay");
        if (sceneRoot instanceof Pane) {
            ((Pane) sceneRoot).getChildren().add(ov);
        } else if (sceneRoot instanceof Group) {
            ((Group) sceneRoot).getChildren().add(ov);
        } else {
            return null;
        }
        scene.getProperties().put(OVERLAY_KEY, ov);
        return ov;
    }
}
