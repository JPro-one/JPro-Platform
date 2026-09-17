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
 * overrides the DOM position so scrolling stays smooth without a JavaFX layout pass per scroll
 * event.
 * <p>
 * It builds only on the JPro Viewport API ({@link WebAPI#browserViewport()} /
 * {@link WebAPI#documentBounds()}) and needs no core change.
 * <p>
 * <strong>How the pin works.</strong> The pin function is a clamp, so each branch of it is a static
 * positioning mode, and the pin is written as exactly that: page-anchored below the pin line,
 * {@code position: fixed} at the inset between the lines, page-anchored again past the release line.
 * JS runs at the two crossings only, so between them the engine scrolls the node itself. A scroll
 * event arms a frame loop that watches for the crossing and stops once the page stops moving; the
 * loop only reads the scroll position and writes at a crossing. Watching from a frame callback
 * rather than from the event matters on Firefox, where the compositor scrolls ahead of the main
 * thread.
 * <p>
 * <strong>What can stop it.</strong> It needs {@code position: fixed} to resolve to the viewport,
 * which any ancestor with {@code transform}, {@code filter}, {@code perspective},
 * {@code contain: paint} or a {@code will-change} naming one of those defeats, so it is gated on a
 * walk up the DOM. A {@code will-change} hint is cleared (it costs a compositing layer and nothing
 * else); the rest are left alone and the node stays on the server-side pin, which is what picking
 * sees anyway. That is correct but only as current as the last viewport update, so it trails a fast
 * scroll.
 * <p>
 * When running as a desktop application the {@link WebAPI} consumer never fires, so installation
 * is a no-op and the node keeps its normal flow positioning.
 * <p>
 * <strong>Anchoring.</strong> The {@link ScrollAnchor} resolves the horizontal and vertical axes
 * independently. The vertical axis drives the pin line, the ride and, for bounded sticky, the
 * release; the horizontal axis is a constant the pin carries through all three states. {@link ScrollAnchor.Mode#STRETCH} resizes the node to span the axis;
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
        // lift the node into the overlay, leaving a placeholder that reserves the node's height in its
        // flow slot (FIXED reserves nothing). naturalHeight, not current bounds: pinning often happens
        // during scene construction, before layout, when bounds are still zero. sync() refines it later.
        final double reservedHeight = (position == ScrollPosition.FIXED) ? 0
                : AnchorGeometry.naturalHeight(node, AnchorGeometry.naturalWidth(node));
        final Region ph = mount.mount(reservedHeight);
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
        final Point2D hostOrigin = overlay.localToScene(0, 0);
        final double hostOffsetY = hostOrigin.getY();
        final double hostOffsetX = hostOrigin.getX();

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
                + nodeW + "|" + nodeH + "|" + docH + "|" + hostOffsetY + "|" + hostOffsetX;

        // NaN/Infinity are valid JS literals, so a non-finite value here would install cleanly and then
        // fail silently: every clamp escapes its guard and the emitted transform is rejected by the CSS
        // parser, leaving a dead pin and nothing in any log. Refuse the install instead.
        if (!allFinite(local.getX(), natTop, y0, relLimitServer, hostOffsetY, hostOffsetX)) {
            LOGGER.warn("jpro-sticky[{}]: skipping install, non-finite geometry "
                            + "(x={}, natTop={}, y0={}, relLimit={}, hostOffsetY={}, hostOffsetX={})",
                    jsKey, local.getX(), natTop, y0, relLimitServer, hostOffsetY, hostOffsetX);
            return;
        }

        if (!installedCompositor) {
            // install inline on the first sync with a real width. the node's DOM peer may still be unregistered,
            // but the injected script resolves it via its own retry loop, so no server-side deferral needed.
            installCompositor(local.getX(), natTop, y0, relLimitServer, hostOffsetY, hostOffsetX);
            installedCompositor = true;
            lastSig = sig;
            LOGGER.debug("jpro-sticky[{}]: compositor installed (w={}, natTop={}, y0={}, hostOffsetY={})",
                    jsKey, nodeW, natTop, y0, hostOffsetY);
        } else if (!sig.equals(lastSig)) {
            installCompositor(local.getX(), natTop, y0, relLimitServer, hostOffsetY, hostOffsetX);
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
     * {@code natTop} is the keyframe 'from' (the flow top for sticky, the pin line for fixed);
     * {@code y0} is the viewport pin line; {@code relLimitServer} is the scene-y release point
     * ({@code < 0} = unbounded, resolved browser-side to the document extent). All three are in
     * scene/document space; {@code hostOffsetY} is the overlay host's document-y origin, subtracted
     * from the transform endpoints only (they are relative to the overlay's own DOM box) while the
     * scroll-range math stays in document space, so a non-scene-root host shifts nothing but the pin.
     * <p>
     * <strong>Readiness race.</strong> The element reference ({@code jpro.getValue(n)}) throws until
     * JPro's render pulse has registered the node, so it resolves inside a {@code requestAnimationFrame}
     * retry loop guarded by try/catch.
     * <p>
     * <strong>Reconnect.</strong> The binding holds the element, never its {@code jpro-id}. That id is
     * a per-view transport index whose counter restarts when a reconnect builds a new view, so a cached
     * id can retarget an unrelated node rather than just go stale. An element reference cannot collide;
     * it only goes stale, which {@code isConnected} detects. A re-install can also resolve the outgoing
     * element before the DOM rebuild replaces it, so a 500ms heartbeat rebinds once that peer detaches.
     */
    private void installCompositor(double x, double natTop, double y0, double relLimitServer,
                                   double hostOffsetY, double hostOffsetX) {
        // a fresh slot per install: the defining command is one-shot per view, so a reconnect needs a
        // new one. reassigning drops the previous handle, whose cleanup only nulls the slot we left.
        elementVar = webapi.getElement(node);
        final String d = elementVar.getName();
        // mirror FX mouseTransparent to pointer-events:none: unlike desktop FX picking, the reparented
        // node is a real div that would otherwise catch clicks meant for the content beneath it.
        final boolean mouseTransparent = node.isMouseTransparent();
        final String js =
                "(function(){\n" +
                "  var reg = (window.__jproStickyC = window.__jproStickyC || {});\n" +
                "  var st = reg['" + jsKey + "'] = reg['" + jsKey + "'] || {};\n" +
                "  st.dead = false;\n" +
                // the sheet carries only @keyframes (compositor tier); the binding is inline on the element.
                "  if(!st.style){ st.style = document.createElement('style');\n" +
                "    st.style.setAttribute('data-jpro-sticky','" + jsKey + "'); document.head.appendChild(st.style); }\n" +
                // latest geometry, baked in server-side. apply() reads these so a re-install (on a
                // geometry-sig change) just updates them and re-renders.
                "  st.x = " + x + "; st.natTop = " + natTop + "; st.inset = " + y0 + "; st.relServer = " + relLimitServer + ";\n" +
                "  st.hostOffsetY = " + hostOffsetY + "; st.hostOffsetX = " + hostOffsetX + ";\n" +
                "  st.pe = " + mouseTransparent + ";\n" +
                "  st.fixBlocker = function(el){\n" +
                "    var p = el.parentElement, n = 0;\n" +
                "    while(p && p !== document.documentElement && n++ < 64){\n" +
                "      var cs = getComputedStyle(p), hit = null;\n" +
                "      if(cs.transform !== 'none') hit = 'transform: ' + cs.transform;\n" +
                "      else if(cs.perspective !== 'none') hit = 'perspective: ' + cs.perspective;\n" +
                "      else if(cs.filter !== 'none') hit = 'filter: ' + cs.filter;\n" +
                "      else if(cs.backdropFilter && cs.backdropFilter !== 'none') hit = 'backdrop-filter';\n" +
                "      else if(/transform|perspective|filter/.test(cs.willChange || '')) hit = 'will-change: ' + cs.willChange;\n" +
                "      else if(/paint|layout|strict|content/.test(cs.contain || '')) hit = 'contain: ' + cs.contain;\n" +
                "      if(hit) return '<' + p.tagName.toLowerCase() + (p.id ? '#' + p.id : '')\n" +
                "                   + '> ' + n + ' up, ' + hit;\n" +
                "      p = p.parentElement;\n" +
                "    }\n" +
                "    return null; };\n" +
                // clears the only blocker that is safe to clear: will-change is a hint, transform / filter /
                // contain are not. undoes JPro 412c150b on these ancestors; the pinned node keeps its layer.
                "  st.unblock = function(el){\n" +
                "    var p = el.parentElement, n = 0;\n" +
                "    while(p && p !== document.documentElement && n++ < 64){\n" +
                "      var cs = getComputedStyle(p);\n" +
                "      if(/transform|perspective|filter/.test(cs.willChange || '')\n" +
                "         && cs.transform === 'none' && cs.perspective === 'none' && cs.filter === 'none'){\n" +
                "        if(!p.hasAttribute('data-jpro-sticky-wc')){\n" +
                "          p.setAttribute('data-jpro-sticky-wc', p.style.willChange || '');\n" +
                "        }\n" +
                "        p.style.willChange = 'auto';\n" +
                "      }\n" +
                "      p = p.parentElement;\n" +
                "    }\n" +
                "    return st.fixBlocker(el); };\n" +
                // the geometry is scene coordinates but the pin compares them against document scroll, so the
                // two have to coincide. they do for a full-page app and not for an embedded tag.
                "  st.embedded = function(el){\n" +
                "    var sc = el.closest && el.closest('.jpro-scene'); if(!sc) return null;\n" +
                "    var r = sc.getBoundingClientRect();\n" +
                "    var dx = r.left + window.scrollX, dy = r.top + window.scrollY;\n" +
                "    return (Math.abs(dx) < 0.5 && Math.abs(dy) < 0.5) ? null : (dx + ',' + dy); };\n" +
                "  st.pick = function(el){\n" +
                "    var off = st.embedded(el);\n" +
                "    if(off){ console.warn('[jpro-sticky] ' + '" + jsKey + "' + ': the jpro tag is at ' + off\n" +
                "      + ' in the document, so scene and document coordinates differ; pinning stays on the'\n" +
                "      + ' server cadence.'); return 'none'; }\n" +
                "    var b = st.unblock(el);\n" +
                "    if(b) console.warn('[jpro-sticky] ' + '" + jsKey + "' + ': position:fixed is captured by '\n" +
                "      + b + ', so this pin falls back to the server cadence and will trail a fast scroll.');\n" +
                "    return b === null ? 'fix' : 'none'; };\n" +
                "  st.mode = null;\n" +
                // the JS value slot is defined by a one-shot command per view, so re-bake the resolver
                // on every install: after a reconnect the previous slot is gone and this one is fresh.
                "  st.resolve = function(){ try { var e = " + d + "; return e && e.style ? e : null; } catch(e){ return null; } };\n" +
                "  st.clear = function(el){ if(!el) return;\n" +
                "    el.style.removeProperty('pointer-events');\n" +
                "    el.removeAttribute('data-jpro-sticky-el'); st.state = null; };\n" +

                // geometry, shared by both tiers.
                // scrollHeight forces layout, so it is read on the heartbeat (and at install), never per frame.
                "  st.measure = function(){ st.docExtent = document.documentElement.scrollHeight; };\n" +
                "  st.range = function(){\n" +
                // document extent, NOT scroll max (scrollHeight - clientHeight): the latter folds in
                // viewport height, leaving the unbounded range stale on resize.
                "    var relLimit = (st.relServer < 0) ? (st.docExtent + st.inset) : st.relServer;\n" +
                "    var sPin = st.natTop - st.inset; if(sPin < 0) sPin = 0;\n" +
                "    var sRel = relLimit - st.inset; if(sRel < sPin + 1) sRel = sPin + 1;\n" +
                // transform endpoints are relative to the overlay's own DOM box, so shift into host-local
                // space. sPin/sRel stay in document/scroll space (host-independent).
                "    return { sPin: sPin, sRel: sRel,\n" +
                "             fromY: st.natTop - st.hostOffsetY, toY: relLimit - st.hostOffsetY }; };\n" +

                // double quotes on purpose: a backslash escape would have to survive the trip over the wire.
                "  st.sel = '[data-jpro-sticky-el=\"" + jsKey + "\"]';\n" +
                // three static states, JS at the crossings only; see the class doc. the placement goes in the
                // sheet with !important because the renderer owns the same inline transform.
                "  st.affix = function(){\n" +
                "    if(st.dead) return;\n" +
                "    var el = st.el; if(!el || !el.isConnected) return;\n" +
                "    var r = st.range();\n" +
                "    var y = window.scrollY, xs = window.scrollX;\n" +
                "    var s = (y < r.sPin) ? 'before' : (y >= r.sRel ? 'after' : 'pinned');\n" +
                "    if(s === st.state && !(s === 'pinned' && xs !== st.scrollX)) return;\n" +
                "    st.state = s; st.scrollX = xs;\n" +
                "    var decl;\n" +
                // fixed makes the viewport the containing block, so X is document-space here and overlay-local
                // in the other two states. always from the server geometry, never read back off the element.
                "    if(s === 'pinned'){\n" +
                "      decl = 'position:fixed !important;left:0 !important;top:0 !important;'\n" +
                "           + 'transform:translate(' + (st.x + st.hostOffsetX - xs) + 'px,'\n" +
                "           + st.inset + 'px) !important;';\n" +
                "    } else {\n" +
                "      decl = 'transform:translate(' + st.x + 'px,'\n" +
                "           + ((s === 'before') ? r.fromY : r.toY) + 'px) !important;';\n" +
                "    }\n" +
                "    st.style.textContent = st.sel + '{' + decl + '}';\n" +
                "  };\n" +
                // the scroll event arms this rather than doing the work: Firefox delivers it late enough to
                // miss the crossing by a few frames. it only reads, writes at a crossing, and stops when idle.
                "  st.pump = function(){\n" +
                "    if(st.dead || st.mode !== 'fix'){ st.pumping = 0; return; }\n" +
                "    st.affix();\n" +
                "    var y = window.scrollY + window.scrollX;\n" +
                "    if(y !== st.lastPumpY){ st.lastPumpY = y; st.idle = 0; }\n" +
                "    else if(++st.idle > 20){ st.pumping = 0; return; }\n" +
                "    requestAnimationFrame(st.pump); };\n" +
                // a passive listener does not block the compositor, and all it does is arm the loop.
                "  st.listen = function(){\n" +
                "    if(st.onscroll) return;\n" +
                "    st.onscroll = function(){ st.idle = 0;\n" +
                "      if(!st.pumping){ st.pumping = 1; st.lastPumpY = -1; requestAnimationFrame(st.pump); } };\n" +
                "    st.onresize = function(){ st.state = null; st.measure(); st.affix(); };\n" +
                "    st.pumping = 0; st.idle = 0;\n" +
                "    window.addEventListener('scroll', st.onscroll, {passive:true});\n" +
                "    window.addEventListener('resize', st.onresize, {passive:true}); };\n" +
                "  st.unlisten = function(){\n" +
                "    if(!st.onscroll) return;\n" +
                "    window.removeEventListener('scroll', st.onscroll);\n" +
                "    window.removeEventListener('resize', st.onresize);\n" +
                "    st.onscroll = null; st.onresize = null; };\n" +

                // the tier an engine lands on is otherwise invisible from the outside.
                "  st.report = function(){\n" +
                "    if(st.mode === st.reported) return;\n" +
                "    st.reported = st.mode;\n" +
                "    console.log('[jpro-sticky] ' + '" + jsKey + "' + ' tier=' + st.mode\n" +
                "      + ' fixedBlockedBy=' + (st.fixBlocker(st.el) || 'nothing')); };\n" +
                "  st.apply = function(){\n" +
                "    var el = st.el; if(!el) return;\n" +
                "    if(!st.mode) st.mode = st.pick(el);\n" +
                "    st.report();\n" +
                "    if(st.pe) el.style.setProperty('pointer-events','none');\n" +
                "    else el.style.removeProperty('pointer-events');\n" +
                "    if(st.mode === 'fix'){\n" +
                "      el.setAttribute('data-jpro-sticky-el','" + jsKey + "');\n" +
                "      st.listen(); st.state = null; st.affix();\n" +
                "      return;\n" +
                "    }\n" +
                // blocked: leave the node on the server pin, which is correct but only as current as the
                // last viewport update.
                "    st.unlisten(); el.removeAttribute('data-jpro-sticky-el');\n" +
                "    st.state = null; st.style.textContent = '';\n" +
                "  };\n" +
                // bind to the element, never its jpro-id: that counter restarts on reconnect, so a cached id
                // can retarget an unrelated node. an element reference only goes stale, which isConnected sees.
                "  st.bind = function(){\n" +
                "    if(st.dead || st.resolving) return;\n" +
                "    st.resolving = true; var tries = 0;\n" +
                "    (function step(){\n" +
                "      if(st.dead){ st.resolving = false; return; }\n" +
                "      var el = st.resolve();\n" +
                "      if(el && el.isConnected){\n" +
                "        if(el !== st.el){ st.clear(st.el); st.el = el; }\n" +
                "        st.resolving = false; st.apply(); return;\n" +
                "      }\n" +
                // the element ref throws until JPro's render pulse has registered the node's DOM peer,
                // so retry (bounded, ~5s at 60fps) rather than giving up on the first miss.
                "      if(tries++ < 300){ requestAnimationFrame(step); } else { st.resolving = false; }\n" +
                "    })();\n" +
                "  };\n" +
                // heartbeat: rebinds after a reconnect rebuilds the peer, and carries the layout-forcing
                // measure(). a re-install can resolve the outgoing element, so liveness is re-checked here.
                "  st.check = function(){ if(st.dead) return;\n" +
                "    var prev = st.docExtent; st.measure();\n" +
                // the affix boundaries follow the document extent, and nothing scrolled, so re-place here.
                "    if(st.mode === 'fix' && st.docExtent !== prev){ st.state = null; st.affix(); }\n" +
                "    if(!st.el || !st.el.isConnected) st.bind(); };\n" +
                "  st.measure();\n" +
                "  if(!st.timer){ st.timer = setInterval(st.check, 500); }\n" +
                "  if(st.el && st.el.isConnected){ st.apply(); } else { st.bind(); }\n" +
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

        // release the element handle; its cleanup nulls the now-unused slot browser-side.
        elementVar = null;

        // restore the node to its flow slot.
        mount.unmount();

        // the compositor tier binds inline on the element and the heartbeat is still running, so teardown
        // has to stop it, strip those properties, and drop the sheet.
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
                // stop the heartbeat and any in-flight resolve before dropping the entry, or they keep
                // running against a torn-down pin (and the interval would outlive the page's use of it).
                "  st.dead = true;\n" +
                "  if(st.timer){ clearInterval(st.timer); st.timer = null; }\n" +
                "  if(st.unlisten) st.unlisten();\n" +
                // the binding is inline on the element now, so it has to be stripped there too: the node
                // survives teardown (it is restored to its flow slot) and would keep the animation.
                "  if(st.clear) st.clear(st.el);\n" +
                "  st.el = null;\n" +
                "  if(st.style && st.style.parentNode) st.style.parentNode.removeChild(st.style);\n" +
                "  delete reg['" + jsKey + "'];\n" +
                // shared ancestors, so this can only go back once the last pin is gone. found by attribute
                // rather than from a list, which would retain the ancestors of every route already left.
                "  if(Object.keys(reg).length === 0){\n" +
                "    document.querySelectorAll('[data-jpro-sticky-wc]').forEach(function(p){\n" +
                "      var was = p.getAttribute('data-jpro-sticky-wc');\n" +
                "      if(was) p.style.willChange = was; else p.style.removeProperty('will-change');\n" +
                "      p.removeAttribute('data-jpro-sticky-wc');\n" +
                "    });\n" +
                "  }\n" +
                "})();");
    }
}
