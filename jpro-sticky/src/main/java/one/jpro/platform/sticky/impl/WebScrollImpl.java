package one.jpro.platform.sticky.impl;

import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;
import javafx.beans.InvalidationListener;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
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
 * <strong>The correction the sheet carries.</strong> The renderer writes every node's layout as a
 * CSS {@code transform}, and sticky resolves in layout space, before transforms, so the ancestors'
 * translates displace it. Their sum is the span's scene y, which the server subtracts from the inset
 * when it writes the rule.
 * <p>
 * Sticky also holds against the nearest scroll container, which an ancestor becomes merely by having
 * a non-visible {@code overflow}. That is the host page's business, not this class's, so a pin only
 * reports the one it found on the console. {@code body} is the case worth knowing: it hands its
 * overflow to the viewport while {@code html} is {@code visible}, and becomes a scroll container in
 * its own right only once {@code html} clips too.
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

    private final Node node;
    private final ScrollPosition position;
    private final ScrollAnchor anchor;
    /** Explicit containment override for STICKY, {@code null} defaults to the original parent. */
    private final Node within;
    /** Pin/unpin transition sink (the node's stuck channels); {@code null} for FIXED or unobserved. */
    private final Consumer<Boolean> stuckSink;
    /** Called when the flow slot leaves the scene, so the dispatcher can re-pin on re-entry. */
    private final Runnable onDetach;
    private final String jsKey = "n" + KEY_SEQ.incrementAndGet();
    /** Stack key ordering this node in the overlay; see {@link StickyOverlay#nextStackOrder}. */
    private final long stackOrder;

    /** The shared reparent-into-overlay mechanic (flow slot, placeholder, overlay). */
    private final OverlayMount mount;

    // resolved at install time
    private WebAPI webapi;
    /** The current {@code jpro.var_N} handles for the node's and the span's elements. Held for as long
     *  as the emitted script may use them: JPro's JSVariable cleanup fires {@code jpro.var_N = undefined}
     *  once one is unreachable, which would leave the resolver (and so the rebind heartbeat) dead. */
    private JSVariable elementVar;
    private JSVariable rangeVar;
    private Group overlay;
    private Region placeholder;
    private Parent root;
    /** The containing block that bounds a STICKY pin (null => document-long / unbounded). */
    private Node container;

    // reactive plumbing: one listener re-syncs geometry on any relevant change
    private final InvalidationListener relayout = obs -> sync();
    /** Signature of the last emitted rule; {@code null} until the first install. */
    private String lastSig;
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
     * Installs the override. Only meaningful under JPro: the {@link WebAPI} consumer fires once JPro
     * has rendered the node in a scene with a window, and on desktop it never fires, so the node keeps
     * its normal flow positioning.
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
        attach();
    }

    private void attach() {
        // lift the node into the overlay, leaving a placeholder that reserves the node's height in its
        // flow slot (FIXED reserves nothing). naturalHeight, not current bounds: pinning often happens
        // during scene construction, before layout, when bounds are still zero. sync() refines it later.
        final double reservedHeight = (position == ScrollPosition.FIXED) ? 0
                : AnchorGeometry.naturalHeight(node, AnchorGeometry.naturalWidth(node));
        final Region ph = mount.mount(reservedHeight, true, onDetach);
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
        // FIXED spans the document from its top, STICKY from the node's flow top.
        final double spanTop = fixed ? 0 : natTop;
        // the span runs from the node's flow top to its release point. unbounded pins, FIXED
        // included, run to the end of the document.
        final double docH = root.getLayoutBounds().getHeight();
        final double relLimit = fixed ? (docH - nodeH)
                : (relLimitServer >= 0 ? relLimitServer : Math.max(natTop, docH - nodeH));
        // a fixed node taller than the viewport can reach its span's end, so it drifts there.
        if (fixed && viewportH > 0 && y0 + nodeH > viewportH + STUCK_EPS) {
            LOGGER.warn("jpro-sticky[{}]: fixed node is taller than the viewport ({} + {} > {});"
                    + " the pin will drift near the end of the document.", jsKey, y0, nodeH, viewportH);
        }
        final Point2D span = overlay.sceneToLocal(x, spanTop);
        range.setLayoutX(span.getX());
        range.setLayoutY(span.getY());
        range.resize(nodeW, Math.max(nodeH, (relLimit - spanTop) + nodeH));
        // picking is a scene pick, so the node sits where it appears. the sheet drops the
        // transform that produces.
        node.setLayoutX(local.getX() - span.getX());
        node.setLayoutY(local.getY() - span.getY());

        // publish pin state (STICKY only): stuck iff the server pin differs from the natural flow top (same
        // rule as ScrollPaneStickyImpl, appear != natural). fidelity = browserViewport() cadence, not per-frame.
        if (!fixed && stuckSink != null) {
            stuckSink.accept(Math.abs(serverY - flowTop) > STUCK_EPS);
        }

        // what the sheet contains, plus the span's top: moving the span moves the very ancestor
        // transform the script measures, so a stale offset outlives the change without it.
        final String sig = y0 + "|" + nodeW + "|" + nodeH + "|" + spanTop;

        // NaN/Infinity are valid JS literals, so a non-finite value installs cleanly and then dies in
        // the CSS parser, leaving a dead pin and nothing in any log. refuse the install instead.
        if (!allFinite(y0, nodeW, nodeH, spanTop)) {
            LOGGER.warn("jpro-sticky[{}]: skipping install, non-finite geometry (y0={}, w={}, h={}, spanTop={})",
                    jsKey, y0, nodeW, nodeH, spanTop);
            return;
        }

        if (!sig.equals(lastSig)) {
            // the node's DOM peer may still be unregistered on the first install; the injected script
            // resolves it via its own retry, so no server-side deferral is needed.
            installSticky(y0, spanTop, nodeW, nodeH);
            LOGGER.debug("jpro-sticky[{}]: sticky rule {} (w={}, h={}, y0={}, spanTop={})",
                    jsKey, lastSig == null ? "installed" : "updated", nodeW, nodeH, y0, spanTop);
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
     * {@code y0} is the viewport pin line, {@code spanTop} the span's scene y (the sum of the ancestor
     * layout transforms sticky ignores), and {@code nodeW}/{@code nodeH} the resolved box. Nothing
     * else is written into the sheet: the span's extent and the node's own offset are server-side
     * layout, and the browser derives the release from the span. The scroll position never enters it.
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
    private void installSticky(double y0, double spanTop, double nodeW, double nodeH) {
        elementVar = webapi.getElement(node);
        rangeVar = webapi.getElement(mount.range());
        final String d = elementVar.getName();
        final String r = rangeVar.getName();
        final boolean mouseTransparent = node.isMouseTransparent();
        final String js =
                "(function(){\n" +
                "  var reg = (window.__jproStickyC = window.__jproStickyC || {});\n" +
                "  var st = reg['" + jsKey + "'] = reg['" + jsKey + "'] || {};\n" +
                "  if(!st.style){ st.style = document.createElement('style');\n" +
                "    st.style.setAttribute('data-jpro-sticky','" + jsKey + "'); document.head.appendChild(st.style); }\n" +
                "  st.sel = '[data-jpro-sticky-el=\"" + jsKey + "\"]';\n" +
                "  st.top = " + (y0 - spanTop) + ";\n" +
                // sticky holds against the nearest scroll container, and an element is one merely by
                // having a non-visible overflow. report it rather than touch the host page's styles.
                "  st.scrollport = function(el){\n" +
                "    var de = document.documentElement, dcs = getComputedStyle(de);\n" +
                "    var rootVisible = dcs.overflowX === 'visible' && dcs.overflowY === 'visible';\n" +
                "    var p = el.parentElement, n = 0;\n" +
                "    while(p && p !== de && n++ < 64){\n" +
                "      var cs = getComputedStyle(p);\n" +
                "      if(/auto|scroll|hidden/.test(cs.overflowX) || /auto|scroll|hidden/.test(cs.overflowY)){\n" +
                // body hands its overflow to the viewport while html is visible, so it is not the
                // scrollport then. only a clipping html stops that and makes body a real one.
                "        if(p !== document.body || !rootVisible) return p;\n" +
                "      }\n" +
                "      p = p.parentElement;\n" +
                "    }\n" +
                "    return null; };\n" +
                "  st.checkPort = function(el){\n" +
                "    if(st.warnedPort) return;\n" +
                "    var p = st.scrollport(el); if(!p) return;\n" +
                "    st.warnedPort = true;\n" +
                "    var name = p.tagName.toLowerCase() + (p.id ? '#' + p.id : '');\n" +
                "    console.warn('[jpro-sticky] ' + '" + jsKey + "' + ': ' + name + ' is the nearest'\n" +
                "      + ' scroll container, so it owns this pin. '\n" +
                "      + (p.scrollHeight > p.clientHeight + 1\n" +
                "         ? 'The offset is measured from the browser viewport, not from it, so the pin'\n" +
                "           + ' line may be off by that element\\'s own position.'\n" +
                "         : 'It cannot scroll, so the pin will not move. Give it overflow:visible, or put'\n" +
                "           + ' the clip on <html> so <body> keeps handing its overflow to the viewport.')); };\n" +
                // sticky clamps to its own parent, so the rule has to land on the span's child, not on
                // the inner element getElement() resolves to (that one is only as tall as the node).
                "  st.target = function(el, range){\n" +
                "    var c = el, p = el.parentElement, n = 0;\n" +
                "    while(p && n++ < 64){\n" +
                "      if(p === range) return c;\n" +
                "      c = p; p = p.parentElement;\n" +
                "    }\n" +
                "    return null; };\n" +
                // the range pane is the containing block, so the browser releases the pin at its bottom.
                "  st.render = function(){\n" +
                "    if(!st.el) return;\n" +
                "    st.checkPort(st.el);\n" +
                "    st.style.textContent = st.sel + '{position:sticky !important;'\n" +
                "      + 'top:' + st.top + 'px !important;'\n" +
                "      + 'width:" + nodeW + "px !important;height:" + nodeH + "px !important;'\n" +
                "      + '" + (mouseTransparent ? "pointer-events:none !important;" : "") + "}'\n" +
                // the server keeps the node at the pinned position so the scene pick lands on it, and the
                // renderer writes that out below the pin. the rule places the box, so the offset has to go.
                "      + st.sel + ',' + st.sel + ' > *{transform:none !important;}'; };\n" +
                "  st.resolve = function(){ try { var e = " + d + ", r = " + r + ";\n" +
                "    return e && r && e.style && r.style ? {el: e, range: r} : null; } catch(x){ return null; } };\n" +
                "  st.clear = function(el){ if(el) el.removeAttribute('data-jpro-sticky-el'); };\n" +
                "  st.bind = function(){\n" +
                "    var h = st.resolve(); if(!h) return false;\n" +
                // the span is mounted with the node, so a missing one means the DOM is mid-rebuild.
                "    var el = st.target(h.el, h.range); if(!el) return false;\n" +
                "    if(st.el && st.el !== el) st.clear(st.el);\n" +
                "    el.setAttribute('data-jpro-sticky-el','" + jsKey + "');\n" +
                "    st.el = el; st.render(); return true; };\n" +
                // a re-install whose bind fails still has to reach the element bound last time.
                "  if(!st.bind() && st.el) st.render();\n" +
                // the peer can be replaced by a DOM rebuild, and only a re-bind retargets the rule.
                "  if(!st.timer) st.timer = setInterval(function(){\n" +
                "    if(!st.el || !st.el.isConnected) st.bind(); }, 500);\n" +
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

        // release the element handles; their cleanup nulls the now-unused slots browser-side.
        elementVar = null;
        rangeVar = null;

        // restore the node to its flow slot.
        mount.unmount();

        // the rule stamps the element and the heartbeat is still running, so teardown has to stop it,
        // strip the stamp, and drop the sheet.
        if (webapi != null && lastSig != null) {
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
                // stop the heartbeat before dropping the entry, or the interval outlives the pin.
                "  if(st.timer){ clearInterval(st.timer); st.timer = null; }\n" +
                // the binding is an attribute on the element, and the node survives teardown (it goes
                // back to its flow slot), so it has to be stripped there too.
                "  st.clear(st.el);\n" +
                "  st.el = null;\n" +
                "  if(st.style && st.style.parentNode) st.style.parentNode.removeChild(st.style);\n" +
                "  delete reg['" + jsKey + "'];\n" +
                "})();");
    }
}
