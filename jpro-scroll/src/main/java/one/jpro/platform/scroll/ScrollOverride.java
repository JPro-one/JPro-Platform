package one.jpro.platform.scroll;

import com.jpro.webapi.WebAPI;
import javafx.application.Platform;
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
    private boolean installing = false;
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
            // First install is deferred: the element's DOM peer may not exist at first sync.
            if (!installing) {
                installing = true;
                final double fx = local.getX();
                final String fSig = sig;
                LOGGER.debug("jpro-scroll[{}]: scheduling first compositor install (w={}, natTop={})", jsKey, w, natTop);
                Platform.runLater(() -> {
                    if (torndown) {
                        return;
                    }
                    installCompositor(fx, natTop, inset, relLimitServer);
                    installedCompositor = true;
                    lastSig = fSig;
                    LOGGER.debug("jpro-scroll[{}]: compositor installed", jsKey);
                });
            }
        } else if (!sig.equals(lastSig)) {
            installCompositor(local.getX(), natTop, inset, relLimitServer);
            lastSig = sig;
        }
    }

    /**
     * (Re-)installs a scroll-timeline animation on the node's own div element. The renderer
     * positions the node via inline {@code style.transform}; a running CSS animation outranks
     * inline styles in the cascade, so this overrides that pin with a compositor-driven pure
     * function of scroll. Assumes an svg scale of 1 (true for native-scrolling pages).
     */
    private void installCompositor(double x, double natTop, double inset, double relLimitServer) {
        final String d = webapi.getElement(node).getName();
        final String js =
                "(function(){\n" +
                "  var d = " + d + ";\n" +
                "  var reg = (window.__jproScrollC = window.__jproScrollC || {});\n" +
                "  var st = reg['" + jsKey + "'] = reg['" + jsKey + "'] || {};\n" +
                "  if(!st.style){ st.style = document.createElement('style');\n" +
                "    st.style.setAttribute('data-jpro-scroll','" + jsKey + "'); document.head.appendChild(st.style); }\n" +
                "  st.key = 'jpro-scroll-" + jsKey + "';\n" +
                // Document extent (scrollHeight), NOT the scroll max (scrollHeight - clientHeight):
                // the latter folds in viewport height, leaving the unbounded range stale on resize.
                "  var docExtent = document.documentElement.scrollHeight;\n" +
                "  var relLimit = (" + relLimitServer + " < 0) ? (docExtent + " + inset + ") : " + relLimitServer + ";\n" +
                "  var sPin = " + natTop + " - " + inset + "; if(sPin < 0) sPin = 0;\n" +
                "  var sRel = relLimit - " + inset + "; if(sRel < sPin + 1) sRel = sPin + 1;\n" +
                "  st.style.textContent = '@keyframes ' + st.key +\n" +
                "    '{from{transform:translate(" + x + "px," + natTop + "px);}to{transform:translate(" + x + "px,' + relLimit + 'px);}}';\n" +
                "  d.style.animationName = st.key;\n" +
                "  d.style.animationTimingFunction = 'linear';\n" +
                "  d.style.animationFillMode = 'both';\n" +
                "  d.style.animationDuration = 'auto';\n" +
                "  d.style.animationTimeline = 'scroll(root block)';\n" +
                "  d.style.animationRangeStart = sPin + 'px';\n" +
                "  d.style.animationRangeEnd = sRel + 'px';\n" +
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

        // Clear the compositor animation on the element and drop its style + registry entry.
        if (webapi != null && installedCompositor) {
            final String d = webapi.getElement(node).getName();
            webapi.executeScript(
                    "(function(){\n" +
                    "  var d = " + d + ";\n" +
                    "  d.style.animationName = ''; d.style.animationTimeline = '';\n" +
                    "  d.style.animationRangeStart = ''; d.style.animationRangeEnd = '';\n" +
                    "})();");
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
