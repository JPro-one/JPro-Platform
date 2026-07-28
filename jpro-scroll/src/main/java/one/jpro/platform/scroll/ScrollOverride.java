package one.jpro.platform.scroll;

import com.jpro.webapi.WebAPI;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
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
 * <strong>v1 scope.</strong> Only the {@link Side#TOP} edge is realised (the proven case); other
 * sides log a warning and fall back to top pinning. The placeholder mirrors the node full-width,
 * matching the proven full-width-bar case.
 *
 * @author Tobias Horak
 */
final class ScrollOverride {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScrollOverride.class);

    /** Sequence for unique per-node JS registry keys. */
    private static final AtomicLong KEY_SEQ = new AtomicLong();

    /** Scene property key under which the shared sticky overlay {@link Group} is cached. */
    private static final Object OVERLAY_KEY = new Object();

    private final Node node;
    private final ScrollPosition position;
    private final Side side;
    private final double offset;
    private final String jsKey = "n" + KEY_SEQ.incrementAndGet();

    // Resolved at install time.
    private WebAPI webapi;
    private Group overlay;
    private Region placeholder;
    private Pane originalParent;
    private int originalIndex = -1;
    private Parent root;

    // Reactive plumbing. A single listener re-syncs geometry on any relevant change.
    private final InvalidationListener relayout = obs -> sync();
    private ChangeListener<Scene> sceneWaiter;
    private boolean installedCompositor = false;
    private String lastSig = "";
    private boolean torndown = false;

    ScrollOverride(Node node, ScrollPosition position, Side side, double offset) {
        this.node = node;
        this.position = position;
        this.side = side;
        this.offset = offset;
    }

    /**
     * Installs the override. Only meaningful under JPro: on desktop the {@link WebAPI} consumer
     * never fires and the node keeps its normal flow positioning.
     */
    void install() {
        if (side != Side.TOP) {
            LOGGER.warn("jpro-scroll: side {} is not yet supported; falling back to TOP for node {}.", side, node);
        }
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
            LOGGER.warn("jpro-scroll: node's parent is {} (not a Pane); cannot pin {}. Node stays in flow.",
                    parent == null ? "null" : parent.getClass().getSimpleName(), node);
            return;
        }
        final Group ov = overlayFor(node.getScene());
        if (ov == null) {
            LOGGER.warn("jpro-scroll: scene root is not a Pane/Group; no overlay host for {}. Node stays in flow.", node);
            return;
        }

        this.overlay = ov;
        this.originalParent = (Pane) parent;
        this.originalIndex = originalParent.getChildren().indexOf(node);
        if (originalIndex < 0) {
            return;
        }
        this.root = node.getScene().getRoot();

        placeholder = new Region();
        placeholder.setMaxWidth(Double.MAX_VALUE);

        // Swap node -> placeholder in the flow, and move the node into the overlay.
        originalParent.getChildren().set(originalIndex, placeholder);
        node.setManaged(false);
        overlay.getChildren().add(node);
        node.applyCss();

        // Signals that require a re-sync: the placeholder's geometry (flow position/size),
        // the browser viewport (the moving signal under native scroll), and the document
        // extent (grow/shrink -> the unbounded pin range must refresh).
        placeholder.layoutBoundsProperty().addListener(relayout);
        placeholder.localToSceneTransformProperty().addListener(relayout);
        webapi.browserViewport().addListener(relayout);
        root.layoutBoundsProperty().addListener(relayout);

        registerCleanup();
        LOGGER.debug("jpro-scroll[{}]: attached (overlay={}, parent={})", jsKey,
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
        final double w = placeholder.getWidth();
        if (w <= 0) {
            LOGGER.debug("jpro-scroll[{}]: sync skipped, placeholder width={}", jsKey, w);
            return;
        }
        final boolean fixed = position == ScrollPosition.FIXED;

        // Fresh (post-applyCss) height so the placeholder mirrors the node's real layout height.
        // prefHeight alone ignores minHeight, so a min-constrained node would reserve too little.
        final double h;
        if (node instanceof Region) {
            final Region region = (Region) node;
            h = Math.max(region.prefHeight(w), region.minHeight(w));
        } else {
            h = node.getLayoutBounds().getHeight();
        }
        // FIXED is out of flow: the placeholder reserves no vertical space. STICKY keeps its slot.
        placeholder.setPrefHeight(fixed ? 0 : h);
        node.resize(w, h);

        // naturalTop is scroll-independent: native scroll moves the browser, not the FX scene.
        final Point2D naturalTopLeft = placeholder.localToScene(0, 0);
        final double x = naturalTopLeft.getX();
        final double flowTop = naturalTopLeft.getY();
        final double inset = offset;

        final Rectangle2D vp = webapi.getBrowserViewport();
        final double viewportTop = (vp == null) ? 0.0 : vp.getMinY();

        // natTop drives the keyframe 'from'. STICKY rides the flow from its natural top; FIXED
        // pins from the very top (natTop == inset => the compositor's sPin becomes 0).
        final double natTop = fixed ? inset : flowTop;
        // Server-side pin (also the no-compositor fallback): clamp to the viewport inset line.
        final double targetSceneY = fixed ? (viewportTop + inset) : Math.max(flowTop, viewportTop + inset);
        final Point2D local = overlay.sceneToLocal(x, targetSceneY);
        node.setLayoutX(local.getX());
        node.setLayoutY(local.getY());

        // relLimit < 0 means unbounded (document-bounded) pin. docH is in the signature so the
        // unbounded range refreshes on document grow/shrink.
        final double relLimitServer = -1.0;
        final double docH = root.getLayoutBounds().getHeight();
        final String sig = natTop + "|" + relLimitServer + "|" + local.getX() + "|" + docH;

        if (!installedCompositor) {
            // Install inline on the first sync with a real width. The node's DOM peer may still be
            // unregistered at this instant, but the injected script resolves it via its own retry
            // loop (see installCompositor), so no server-side deferral (a runLater pulse) is needed.
            installCompositor(local.getX(), natTop, inset, relLimitServer);
            installedCompositor = true;
            lastSig = sig;
            LOGGER.debug("jpro-scroll[{}]: compositor installed (w={}, natTop={})", jsKey, w, natTop);
        } else if (!sig.equals(lastSig)) {
            installCompositor(local.getX(), natTop, inset, relLimitServer);
            lastSig = sig;
        }
    }

    /**
     * (Re-)installs the scroll-timeline animation that pins the node. The animation is realised
     * entirely inside an injected {@code <style>} sheet: the {@code @keyframes} plus a rule that
     * binds them to the node via its stable {@code [jpro-id]} attribute selector. The renderer
     * positions the node with inline {@code style.transform}, but a running CSS animation outranks
     * inline styles in the cascade, so the animation overrides that pin with a compositor-driven
     * pure function of scroll. Assumes an svg scale of 1 (true for native-scrolling pages).
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
    private void installCompositor(double x, double natTop, double inset, double relLimitServer) {
        final String d = webapi.getElement(node).getName();
        final String js =
                "(function(){\n" +
                "  var reg = (window.__jproScrollC = window.__jproScrollC || {});\n" +
                "  var st = reg['" + jsKey + "'] = reg['" + jsKey + "'] || {};\n" +
                "  if(!st.style){ st.style = document.createElement('style');\n" +
                "    st.style.setAttribute('data-jpro-scroll','" + jsKey + "'); document.head.appendChild(st.style); }\n" +
                "  st.key = 'jpro-scroll-" + jsKey + "';\n" +
                // Latest geometry, baked in server-side; render() reads these so a re-install (on a
                // geometry-signature change) just updates them and rewrites the sheet.
                "  st.x = " + x + "; st.natTop = " + natTop + "; st.inset = " + inset + "; st.relServer = " + relLimitServer + ";\n" +
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

        // Restore the node to its flow slot.
        if (overlay != null) {
            overlay.getChildren().remove(node);
        }
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

    private static void removeCompositorStyle(WebAPI webapi, String jsKey) {
        webapi.executeScript(
                "(function(){\n" +
                "  var reg = window.__jproScrollC; if(!reg) return;\n" +
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
        ov.setId("jpro-scroll-overlay");
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
