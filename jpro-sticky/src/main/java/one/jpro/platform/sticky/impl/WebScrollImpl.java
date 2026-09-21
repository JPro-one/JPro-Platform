package one.jpro.platform.sticky.impl;

import com.jpro.webapi.JSVariable;
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
import one.jpro.platform.sticky.ScrollAnchor;
import one.jpro.platform.sticky.ScrollPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * The scroll-aware pinning for a single {@link Node} on the web. One instance owns one node's
 * override lifecycle: it reparents the node into a per-scene overlay, leaves a layout-mirroring
 * placeholder in the node's flow slot, server-pins the node (which is what picking sees), and
 * positions the node's DOM peer so scrolling costs nothing on either side.
 * <p>
 * It builds only on the JPro Viewport API ({@link WebAPI#browserViewport()} /
 * {@link WebAPI#documentBounds()}) and needs no core change.
 * <p>
 * <strong>How the pin works.</strong> The browser does it. The node is mounted inside a span sized
 * to the pin's scroll range, and the injected sheet makes the node's element
 * {@code position: sticky} at the anchor's inset. Sticky clamps to its containing block, so the
 * release is the span's own end and needs no code, and the crossing is the compositor's, so nothing
 * is measured or written per scroll event. {@link ScrollPosition#FIXED} is the same pin over a span
 * the length of the document: a viewport-anchored node always fits the viewport, so that span's end
 * is out of reach and the pin never releases.
 * <p>
 * <strong>Two corrections the sheet carries.</strong> Sticky resolves in layout space, so an
 * ancestor {@code transform} displaces it; that shift is measured in the browser, not baked, and
 * subtracted from the offset. And sticky holds against the nearest scroll container, which an
 * ancestor becomes merely by having a non-visible {@code overflow} ({@code body} usually does), so
 * one that cannot scroll at all is cleared, where it changes nothing else.
 * <p>
 * The server keeps the node at the position it appears at, because picking is a scene pick and a
 * node parked at the span's origin is not where the click lands. The renderer writes that offset out
 * as a transform below the pin, which the sheet drops: the rule places the box.
 * <p>
 * When running as a desktop application the {@link WebAPI} consumer never fires, so installation
 * is a no-op and the node keeps its normal flow positioning.
 * <p>
 * <strong>Anchoring.</strong> The {@link ScrollAnchor} resolves the horizontal and vertical axes
 * independently. The vertical axis drives the pin line, the ride and, for bounded sticky, the
 * release; the horizontal axis is a constant the pin carries throughout.
 * {@link ScrollAnchor.Mode#STRETCH} resizes the node to span the axis.
 *
 * @author Tobias Horak
 */
public final class WebScrollImpl implements ScrollImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebScrollImpl.class);

    /** Sequence for unique per-node JS registry keys. */
    private static final AtomicLong KEY_SEQ = new AtomicLong();

    /** Slack (px) below which the server pin is treated as sitting at the natural flow position. */
    private static final double STUCK_EPS = 0.5;

    /** FX id given to a pin's span; JPro renders it as a DOM id under its own {@code jpro-} prefix. */
    private static final String RANGE_ID_PREFIX = "sticky-range-";

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
    /** The current {@code jpro.var_N} handle for the node's element. Held for as long as the emitted
     *  script may use it: JPro's JSVariable cleanup fires {@code jpro.var_N = undefined} once this is
     *  unreachable, which would leave the resolver (and so the rebind heartbeat) permanently dead. */
    private JSVariable elementVar;
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
    private boolean installedSticky = false;
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
        // lift the node into the overlay, leaving a placeholder that reserves the node's height in its
        // flow slot (FIXED reserves nothing). naturalHeight, not current bounds: pinning often happens
        // during scene construction, before layout, when bounds are still zero. sync() refines it later.
        final double reservedHeight = (position == ScrollPosition.FIXED) ? 0
                : AnchorGeometry.naturalHeight(node, AnchorGeometry.naturalWidth(node));
        final Region ph = mount.mount(reservedHeight, true);
        if (ph == null) {
            return; // could not mount (no Pane parent / overlay host), node stays in flow
        }
        this.placeholder = ph;
        this.overlay = mount.overlay();
        // JPro nests the node below the span, and getElement() resolves to the inner element, so the
        // sticky rule needs the span's own child. an id is how the script can tell which one that is.
        if (mount.range() != null) {
            mount.range().setId(RANGE_ID_PREFIX + jsKey);
        }
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
     * Recomputes the node's pinned position and (re-)emits the sticky rule when the geometry
     * signature changes, never per scroll event (that is the browser's job).
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

        // natTop = the span's top. STICKY rides the flow from its natural top, FIXED from the page top.
        final double natTop = fixed ? y0 : flowTop;

        // STICKY release limit = containerBottom - nodeH. -1 = unbounded (FIXED or page-spanning container).
        final double relLimitServer = releaseLimit(nodeH);

        // server-side pin (also the no-script fallback + what picking sees): clamp to pin line while
        // pinned, ride flow before, honour the release limit.
        double serverY = fixed ? (viewportTop + y0) : Math.max(flowTop, viewportTop + y0);
        if (!fixed && relLimitServer >= 0) {
            serverY = Math.min(serverY, relLimitServer);
        }
        final Point2D local = overlay.sceneToLocal(x, serverY);
        final Pane range = mount.range();
        if (range == null) {
            node.setLayoutX(local.getX());
            node.setLayoutY(local.getY());
        } else {
            // the span runs from the node's flow top to its release point, and position:sticky clamps to it,
            // so the release needs no code. unbounded pins run to the end of the document.
            // FIXED spans the document from its very top: a viewport-anchored node always fits the
            // viewport, so the span's end is out of reach and the pin never releases.
            final double docH = root.getLayoutBounds().getHeight();
            final double spanTop = fixed ? 0 : natTop;
            final double relLimit = fixed ? (docH - nodeH)
                    : (relLimitServer >= 0 ? relLimitServer : Math.max(natTop, docH - nodeH));
            // FIXED rides its span because a viewport-anchored node fits the viewport, so the span's end
            // stays out of reach. A node taller than the viewport breaks that and drifts near the end.
            if (fixed && viewportH > 0 && y0 + nodeH > viewportH + STUCK_EPS) {
                LOGGER.warn("jpro-sticky[{}]: fixed node is taller than the viewport ({} + {} > {});"
                        + " the pin will drift near the end of the document.", jsKey, y0, nodeH, viewportH);
            }
            final Point2D span = overlay.sceneToLocal(x, spanTop);
            range.setLayoutX(span.getX());
            range.setLayoutY(span.getY());
            range.resize(nodeW, Math.max(nodeH, (relLimit - spanTop) + nodeH));
            // picking is a scene pick on the server, so the node has to sit where it appears, exactly as
            // it does without a span. the sticky rule drops the transform that produces on its element.
            node.setLayoutX(local.getX() - span.getX());
            node.setLayoutY(local.getY() - span.getY());
        }

        // publish pin state (STICKY only): stuck iff the server pin differs from the natural flow top (same
        // rule as ScrollPaneStickyImpl, appear != natural). fidelity = browserViewport() cadence, not per-frame.
        if (!fixed && stuckSink != null) {
            final boolean nowStuck = Math.abs(serverY - flowTop) > STUCK_EPS;
            if (nowStuck != lastStuck) {
                lastStuck = nowStuck;
                stuckSink.accept(nowStuck);
            }
        }

        // only what the sheet bakes in: the span and the node's position are server-side layout,
        // which re-runs on its own.
        final String sig = y0 + "|" + nodeW + "|" + nodeH;

        // NaN/Infinity are valid JS literals, so a non-finite value here would install cleanly and then
        // fail silently: the emitted declaration is rejected by the CSS parser, leaving a dead pin and
        // nothing in any log. Refuse the install instead.
        if (!allFinite(y0, nodeW, nodeH)) {
            LOGGER.warn("jpro-sticky[{}]: skipping install, non-finite geometry (y0={}, w={}, h={})",
                    jsKey, y0, nodeW, nodeH);
            return;
        }

        if (!installedSticky) {
            // install inline on the first sync with a real width. the node's DOM peer may still be unregistered,
            // but the injected script resolves it via its own retry loop, so no server-side deferral needed.
            installSticky(y0, nodeW, nodeH);
            installedSticky = true;
            lastSig = sig;
            LOGGER.debug("jpro-sticky[{}]: sticky rule installed (w={}, h={}, y0={})",
                    jsKey, nodeW, nodeH, y0);
        } else if (!sig.equals(lastSig)) {
            installSticky(y0, nodeW, nodeH);
            lastSig = sig;
        }
    }

    private static boolean allFinite(double... values) {
        for (double v : values) {
            if (!Double.isFinite(v)) {
                return false;
            }
        }
        return true;
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
     * (Re-)installs the pin. The placement is one {@code !important} rule in an injected
     * {@code <style>} sheet, keyed by a {@code data-jpro-sticky-el} stamp: the renderer positions the
     * node with inline {@code style.transform}, and an important author rule is the only declaration
     * that outranks inline. Assumes an svg scale of 1 (true for native-scrolling pages).
     * <p>
     * {@code y0} is the viewport pin line, and {@code nodeW}/{@code nodeH} the resolved box. Nothing
     * else is baked: the span's extent and the node's own offset are server-side layout, and the
     * browser derives the release from the span. The scroll position never enters the sheet.
     * <p>
     * <strong>Readiness race.</strong> The element reference ({@code jpro.getValue(n)}) throws until
     * JPro's render pulse has registered the node, so it resolves inside a retry loop guarded by
     * try/catch.
     * <p>
     * <strong>Reconnect.</strong> The binding holds the element, never its {@code jpro-id}. That id is
     * a per-view transport index whose counter restarts when a reconnect builds a new view, so a cached
     * id can retarget an unrelated node rather than just go stale. An element reference cannot collide;
     * it only goes stale, which {@code isConnected} detects. A re-install can also resolve the outgoing
     * element before the DOM rebuild replaces it, so a 500ms heartbeat rebinds once that peer detaches.
     */
    private void installSticky(double y0, double nodeW, double nodeH) {
        elementVar = webapi.getElement(node);
        final String d = elementVar.getName();
        final boolean mouseTransparent = node.isMouseTransparent();
        final String js =
                "(function(){\n" +
                "  var reg = (window.__jproStickyC = window.__jproStickyC || {});\n" +
                "  var st = reg['" + jsKey + "'] = reg['" + jsKey + "'] || {};\n" +
                "  st.dead = false; st.mode = 'sticky';\n" +
                "  if(!st.style){ st.style = document.createElement('style');\n" +
                "    st.style.setAttribute('data-jpro-sticky','" + jsKey + "'); document.head.appendChild(st.style); }\n" +
                "  st.sel = '[data-jpro-sticky-el=\"" + jsKey + "\"]';\n" +
                "  st.inset = " + y0 + ";\n" +
                "  st.rangeId = 'jpro-" + RANGE_ID_PREFIX + jsKey + "';\n" +
                // sticky holds against the nearest scroll container, and an element that cannot scroll is
                // still one, so the pin would hold against a viewport that never moves. body is often one.
                "  st.unclip = function(el){\n" +
                "    var p = el.parentElement, n = 0;\n" +
                "    while(p && p !== document.documentElement && n++ < 64){\n" +
                "      var cs = getComputedStyle(p);\n" +
                "      if(/auto|scroll|hidden/.test(cs.overflowX) || /auto|scroll|hidden/.test(cs.overflowY)){\n" +
                "        if(p.scrollHeight > p.clientHeight + 1 || p.scrollWidth > p.clientWidth + 1){\n" +
                // an embedded jpro tag can sit inside a real scroller. that one owns the pin, but the
                // offset was resolved against the browser viewport, so the two disagree.
                "          if(!st.warnedScroller){ st.warnedScroller = true;\n" +
                "            console.warn('[jpro-sticky] ' + '" + jsKey + "' + ': an ancestor of this pin'\n" +
                "              + ' scrolls, so the pin holds against it and not against the page. The'\n" +
                "              + ' offset is measured from the browser viewport and may be off by the'\n" +
                "              + \" scroller's own position.\"); }\n" +
                "          return;\n" +
                "        }\n" +
                "        if(!p.hasAttribute('data-jpro-sticky-ov')){\n" +
                "          p.setAttribute('data-jpro-sticky-ov', p.style.overflow || '');\n" +
                "        }\n" +
                // both axes together: visible on one computes back to auto while the other clips.
                "        p.style.setProperty('overflow', 'visible', 'important');\n" +
                "      }\n" +
                "      p = p.parentElement;\n" +
                "    } };\n" +
                // sticky clamps to its own parent, so the rule has to land on the span's child, not on
                // the inner element getElement() resolves to (that one is only as tall as the node).
                "  st.target = function(el){\n" +
                "    var c = el, p = el.parentElement, n = 0;\n" +
                "    while(p && n++ < 64){\n" +
                "      if(p.id === st.rangeId) return c;\n" +
                "      c = p; p = p.parentElement;\n" +
                "    }\n" +
                "    return null; };\n" +
                // sticky resolves in layout space, so only an ancestor transform displaces it. measured
                // rather than baked: the fixed path rewrites those transforms to left / top as it runs.
                "  st.shift = function(el){\n" +
                "    var y = 0, p = el.parentElement, n = 0;\n" +
                "    while(p && p !== document.documentElement && n++ < 64){\n" +
                "      var m = /^matrix\\(1, 0, 0, 1, (-?[0-9.]+), (-?[0-9.]+)\\)$/\n" +
                "        .exec(getComputedStyle(p).transform);\n" +
                "      if(m) y += parseFloat(m[2]);\n" +
                "      p = p.parentElement;\n" +
                "    }\n" +
                "    return y; };\n" +
                // the range pane is the containing block, so the browser releases the pin at its bottom.
                "  st.render = function(){\n" +
                "    if(!st.el) return;\n" +
                "    st.unclip(st.el);\n" +
                "    st.lastShift = st.shift(st.el);\n" +
                "    st.style.textContent = st.sel + '{position:sticky !important;'\n" +
                "      + 'top:' + (st.inset - st.lastShift) + 'px !important;'\n" +
                "      + 'width:" + nodeW + "px !important;height:" + nodeH + "px !important;'\n" +
                "      + '" + (mouseTransparent ? "pointer-events:none !important;" : "") + "}'\n" +
                // the server keeps the node at the pinned position so the scene pick lands on it, and the
                // renderer writes that out below the pin. the rule places the box, so the offset has to go.
                "      + st.sel + ',' + st.sel + ' > *{transform:none !important;}'; };\n" +
                "  st.resolve = function(){ try { var e = " + d + "; return e && e.style ? e : null; } catch(e){ return null; } };\n" +
                "  st.clear = function(el){ if(el) el.removeAttribute('data-jpro-sticky-el'); };\n" +
                "  st.bind = function(){\n" +
                "    var inner = st.resolve(); if(!inner) return false;\n" +
                // the span is mounted with the node, so a missing one means the DOM is mid-rebuild.
                "    var el = st.target(inner); if(!el) return false;\n" +
                "    if(st.el && st.el !== el) st.clear(st.el);\n" +
                "    el.setAttribute('data-jpro-sticky-el','" + jsKey + "');\n" +
                "    st.el = el; st.render(); return true; };\n" +
                "  st.bind();\n" +
                // the peer can be replaced by a DOM rebuild, and only a re-bind retargets the rule.
                "  if(!st.timer) st.timer = setInterval(function(){\n" +
                "    if(st.dead) return;\n" +
                "    if(!st.el || !st.el.isConnected){ st.bind(); return; }\n" +
                // a fixed pin installing later rewrites the shared ancestors, which moves the pin line.
                "    if(st.shift(st.el) !== st.lastShift) st.render(); }, 500);\n" +
                "})();";
        webapi.executeScript(js);
    }

    /**
     * Reverses everything this override installed: deregisters listeners, restores the node to
     * its flow slot, and drops the sticky rule and its {@code <style>} element.
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

        // release the element handle; its cleanup nulls the now-unused slot browser-side.
        elementVar = null;

        // restore the node to its flow slot.
        mount.unmount();

        // the rule stamps the element and the heartbeat is still running, so teardown has to stop it,
        // strip the stamp, and drop the sheet.
        if (webapi != null && installedSticky) {
            removeStickyStyle(webapi, jsKey);
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
                removeStickyStyle(w, key);
            }
        });
    }

    private static void removeStickyStyle(WebAPI webapi, String jsKey) {
        webapi.executeScript(
                "(function(){\n" +
                "  var reg = window.__jproStickyC; if(!reg) return;\n" +
                "  var st = reg['" + jsKey + "']; if(!st) return;\n" +
                // stop the heartbeat and any in-flight resolve before dropping the entry, or they keep
                // running against a torn-down pin (and the interval would outlive the page's use of it).
                "  st.dead = true;\n" +
                "  if(st.timer){ clearInterval(st.timer); st.timer = null; }\n" +
                // the binding is an attribute on the element, and the node survives teardown (it goes
                // back to its flow slot), so it has to be stripped there too.
                "  if(st.clear) st.clear(st.el);\n" +
                "  st.el = null;\n" +
                "  if(st.style && st.style.parentNode) st.style.parentNode.removeChild(st.style);\n" +
                "  delete reg['" + jsKey + "'];\n" +
                // shared ancestors, so this can only go back once the last pin is gone. found by attribute
                // rather than from a list, which would retain the ancestors of every route already left.
                "  if(Object.keys(reg).length === 0){\n" +
                "    document.querySelectorAll('[data-jpro-sticky-ov]').forEach(function(p){\n" +
                "      var was = p.getAttribute('data-jpro-sticky-ov');\n" +
                "      if(was) p.style.overflow = was; else p.style.removeProperty('overflow');\n" +
                "      p.removeAttribute('data-jpro-sticky-ov');\n" +
                "    });\n" +
                "  }\n" +
                "})();");
    }
}
