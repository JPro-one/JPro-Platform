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
import java.util.Locale;
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
 * <strong>Three pin tiers.</strong> The pin function is a clamp, so each branch of it is a static
 * positioning mode. The <em>affix</em> tier says so outright: page-anchored below the pin line,
 * {@code position: fixed} at the inset between the lines, page-anchored again past the release line,
 * with JS running at the two crossings only. That is smooth in every engine, because between the
 * crossings the engine scrolls the node itself. It needs {@code position: fixed} to actually mean
 * the viewport, which fails if any ancestor is a containing block for it, so it is gated on a walk
 * up the DOM. Where that gate fails, {@code animation-timeline: scroll()} pins on the compositor
 * where supported, and a {@code requestAnimationFrame} loop evaluating the same clamp per frame is
 * the floor. The compositor rule must be <em>withheld</em> rather than emitted and ignored on
 * engines that lack it, or the node is parked a document-height off screen; see {@code apply()} in
 * {@link #installCompositor}. The server-side pin stays underneath all three as what picking sees,
 * and as the last resort if no tier binds.
 * <p>
 * Set {@code -Djpro.sticky.pin} (or {@code JPRO_STICKY_PIN}) to {@code fix}, {@code css} or
 * {@code raf} to force one tier, for A/B measurement; {@code auto} is the default.
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

        // NaN/Infinity are valid JS literals, so a non-finite value here would install cleanly and then
        // fail silently: every clamp escapes its guard and the emitted transform is rejected by the CSS
        // parser, leaving a dead pin and nothing in any log. Refuse the install instead.
        if (!allFinite(local.getX(), natTop, y0, relLimitServer, hostOffsetY)) {
            LOGGER.warn("jpro-sticky[{}]: skipping install, non-finite geometry "
                            + "(x={}, natTop={}, y0={}, relLimit={}, hostOffsetY={})",
                    jsKey, local.getX(), natTop, y0, relLimitServer, hostOffsetY);
            return;
        }

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

    /** Pin tier: {@code auto} picks affix where fixed positioning holds, the compositor where
     *  scroll timelines are supported, and rAF as the floor. {@code fix}, {@code css} and
     *  {@code raf} force one tier, for A/B measurement. Forcing {@code css} on an engine that lacks
     *  scroll timelines does not park the node: {@code apply()} reads the computed style back and
     *  falls through to the floor. */
    private static final String PIN_MODE = resolvePinMode();

    /**
     * Resolves the pin mode to one of exactly {@code auto}, {@code fix}, {@code css} or {@code raf}.
     * The result is interpolated
     * into the injected script, so it is validated rather than passed through: a stray quote would
     * break the whole script (and every pin on the page) with no server-side signal, and a stray
     * newline would silently read as {@code auto}, quietly measuring the wrong tier.
     */
    private static String resolvePinMode() {
        String v = System.getProperty("jpro.sticky.pin");
        if (v == null || v.isEmpty()) {
            // the JPro server is a forked JVM, so a -D on the build does not reach it.
            v = System.getenv("JPRO_STICKY_PIN");
        }
        if (v == null) {
            return "auto";
        }
        v = v.trim().toLowerCase(Locale.ROOT);
        if (v.isEmpty() || v.equals("auto")) {
            return "auto";
        }
        if (v.equals("fix")) {
            return "fix";
        }
        if (v.equals("css")) {
            return "css";
        }
        if (v.equals("raf")) {
            return "raf";
        }
        LOGGER.warn("jpro-sticky: ignoring unknown pin mode '{}', expected 'auto', 'fix', 'css' or 'raf'", v);
        return "auto";
    }

    /**
     * (Re-)installs the pin. Two tiers share one geometry: where {@code animation-timeline: scroll()}
     * exists the pin is a compositor-driven CSS animation, otherwise a {@code requestAnimationFrame}
     * loop evaluating the same function per frame. The {@code @keyframes} live in an injected
     * {@code <style>} sheet; both tiers bind inline on the element. The renderer positions the node
     * with inline {@code style.transform}; a running animation outranks inline declarations in the
     * cascade, and the rAF tier simply rewrites the property. Assumes an svg scale of 1 (true for
     * native-scrolling pages).
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
    private void installCompositor(double x, double natTop, double y0, double relLimitServer, double hostOffsetY) {
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
                "  st.key = 'jpro-sticky-" + jsKey + "';\n" +
                // latest geometry, baked in server-side. apply() reads these so a re-install (on a
                // geometry-sig change) just updates them and re-renders.
                "  st.x = " + x + "; st.natTop = " + natTop + "; st.inset = " + y0 + "; st.relServer = " + relLimitServer + "; st.hostOffsetY = " + hostOffsetY + ";\n" +
                "  st.pe = " + mouseTransparent + ";\n" +
                // gate on every declaration the rule depends on, in the exact form emitted. testing only
                // animation-timeline:scroll() would pass on an engine that then rejects duration:auto,
                // which is what parks the node a document-height off screen.
                "  st.sda = CSS.supports('animation-timeline','scroll(root block)')\n" +
                "         && CSS.supports('animation-duration','auto')\n" +
                "         && CSS.supports('animation-range','0px 1px');\n" +
                "  st.force = '" + PIN_MODE + "';\n" +
                // position:fixed is only viewport-anchored while no ancestor is a containing block for
                // it. any transform / filter / perspective / will-change / paint containment on the way
                // up captures it, and it then scrolls with the page instead of holding still.
                "  st.fixOk = function(el){\n" +
                "    var p = el.parentElement, n = 0;\n" +
                "    while(p && p !== document.documentElement && n++ < 64){\n" +
                "      var cs = getComputedStyle(p);\n" +
                "      if(cs.transform !== 'none' || cs.perspective !== 'none' || cs.filter !== 'none'\n" +
                "         || (cs.backdropFilter && cs.backdropFilter !== 'none')\n" +
                "         || /transform|perspective|filter/.test(cs.willChange || '')\n" +
                "         || /paint|layout|strict|content/.test(cs.contain || '')) return false;\n" +
                "      p = p.parentElement;\n" +
                "    }\n" +
                "    return true; };\n" +
                // affix first: three static states cost nothing per frame and scroll on the compositor
                // in every engine. compositor keyframes next, rAF as the floor. picked per element,
                // because the gate depends on the DOM the node actually landed in.
                "  st.pick = function(el){\n" +
                "    if(st.force !== 'auto') return st.force;\n" +
                "    if(st.fixOk(el)) return 'fix';\n" +
                "    return st.sda ? 'css' : 'raf'; };\n" +
                "  st.mode = null;\n" +
                // the JS value slot is defined by a one-shot command per view, so re-bake the resolver
                // on every install: after a reconnect the previous slot is gone and this one is fresh.
                "  st.resolve = function(){ try { var e = " + d + "; return e && e.style ? e : null; } catch(e){ return null; } };\n" +
                "  st.clear = function(el){ if(!el) return;\n" +
                "    ['animation-name','animation-timing-function','animation-fill-mode','animation-duration',\n" +
                "     'animation-timeline','animation-range','pointer-events'].forEach(function(p){\n" +
                "      el.style.removeProperty(p); });\n" +
                // the rAF tier owns transform while it runs; hand it back so the renderer's own pin shows.
                "    if(st.wroteTransform){ el.style.removeProperty('transform'); st.wroteTransform = false; }\n" +
                "    el.removeAttribute('data-jpro-sticky-el'); st.state = null; st.docX = null; };\n" +

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

                // the affix tier. the pin function is a clamp, so every branch of it is a *static*
                // positioning mode: below the pin line the node rides the page (absolute at its document
                // Y), between the lines it holds still (fixed at the inset), past the release line it
                // rides again (absolute at the release Y). JS runs at the two crossings only; in between
                // the engine scrolls it on the compositor, which is why this is smooth where per-frame
                // work is not.
                //
                // the placement goes in the sheet, not inline, and carries !important: the renderer
                // writes the server pin to the same inline transform and an important author rule is the
                // only declaration that outranks it. same reason the compositor tier uses an animation.
                // double quotes on purpose: a backslash escape would have to survive the script's
                // trip over the wire, and nothing else in here needs one.
                "  st.sel = '[data-jpro-sticky-el=\"" + jsKey + "\"]';\n" +
                "  st.affix = function(){\n" +
                "    if(st.dead) return;\n" +
                "    var el = st.el; if(!el || !el.isConnected) return;\n" +
                "    var r = st.range();\n" +
                "    var y = window.scrollY;\n" +
                "    var s = (y < r.sPin) ? 'before' : (y >= r.sRel ? 'after' : 'pinned');\n" +
                "    if(s === st.state) return;\n" +
                // measured while still page-anchored, so it is the node's document X. once the node is
                // fixed its rect is viewport-relative and this would read the pinned value back.
                "    if(st.docX == null){ st.docX = el.getBoundingClientRect().left + window.scrollX; }\n" +
                "    st.state = s;\n" +
                "    var decl;\n" +
                "    if(s === 'pinned'){\n" +
                "      decl = 'position:fixed !important;left:0 !important;top:0 !important;'\n" +
                "           + 'transform:translate(' + (st.docX - window.scrollX) + 'px,' + st.inset + 'px) !important;';\n" +
                "    } else {\n" +
                "      decl = 'transform:translate(' + st.x + 'px,'\n" +
                "           + ((s === 'before') ? r.fromY : r.toY) + 'px) !important;';\n" +
                "    }\n" +
                "    st.style.textContent = st.sel + '{' + decl + '}';\n" +
                "  };\n" +
                // a passive scroll listener does not block the compositor; it only flips the state, and
                // only when the state actually changes, so an in-range scroll writes nothing at all.
                "  st.listen = function(){\n" +
                "    if(st.onscroll) return;\n" +
                "    st.onscroll = function(){ st.affix(); };\n" +
                "    st.onresize = function(){ st.docX = null; st.state = null; st.measure(); st.affix(); };\n" +
                "    window.addEventListener('scroll', st.onscroll, {passive:true});\n" +
                "    window.addEventListener('resize', st.onresize, {passive:true}); };\n" +
                "  st.unlisten = function(){\n" +
                "    if(!st.onscroll) return;\n" +
                "    window.removeEventListener('scroll', st.onscroll);\n" +
                "    window.removeEventListener('resize', st.onresize);\n" +
                "    st.onscroll = null; st.onresize = null; };\n" +

                // the rAF tier: same pin function as the keyframes, per frame instead of on the
                // compositor. reads only window.scrollY (no forced layout) and writes only on change,
                // so an idle page dirties nothing.
                "  st.tick = function(){\n" +
                "    if(st.dead){ st.raf = null; return; }\n" +
                "    st.raf = requestAnimationFrame(st.tick);\n" +
                "    var el = st.el; if(!el || !el.isConnected) return;\n" +
                "    var r = st.range();\n" +
                "    var p = (window.scrollY - r.sPin) / (r.sRel - r.sPin);\n" +
                "    if(p < 0) p = 0; else if(p > 1) p = 1;\n" +
                "    var ty = r.fromY + p * (r.toY - r.fromY);\n" +
                // compare against the element, not just the last value computed: the renderer writes the
                // same property from the server pin, and if only ty were checked an unchanged ty would
                // never repair that overwrite. reading the inline declaration forces no layout, and
                // lastWritten holds the engine's normalised serialisation so an idle page still writes
                // nothing.
                "    if(ty !== st.lastY || el.style.transform !== st.lastWritten){\n" +
                "      el.style.setProperty('transform','translate(' + st.x + 'px,' + ty + 'px)');\n" +
                "      st.lastY = ty; st.lastWritten = el.style.transform; st.wroteTransform = true;\n" +
                "    }\n" +
                "  };\n" +

                "  st.apply = function(){\n" +
                "    var el = st.el; if(!el) return;\n" +
                "    if(!st.mode) st.mode = st.pick(el);\n" +
                "    if(st.pe) el.style.setProperty('pointer-events','none');\n" +
                "    else el.style.removeProperty('pointer-events');\n" +
                "    if(st.mode === 'fix'){\n" +
                "      if(st.raf){ cancelAnimationFrame(st.raf); st.raf = null; }\n" +
                "      ['animation-name','animation-timing-function','animation-fill-mode','animation-duration',\n" +
                "       'animation-timeline','animation-range'].forEach(function(p){ el.style.removeProperty(p); });\n" +
                "      if(st.wroteTransform){ el.style.removeProperty('transform'); st.wroteTransform = false; }\n" +
                "      el.setAttribute('data-jpro-sticky-el','" + jsKey + "');\n" +
                "      st.listen(); st.state = null; st.affix();\n" +
                "      return;\n" +
                "    }\n" +
                "    st.unlisten(); el.removeAttribute('data-jpro-sticky-el'); st.state = null;\n" +
                "    if(st.mode === 'raf'){\n" +
                // drop any compositor rule from a previous tier/install, then start the loop.
                "      st.style.textContent = '';\n" +
                "      ['animation-name','animation-timing-function','animation-fill-mode','animation-duration',\n" +
                "       'animation-timeline','animation-range'].forEach(function(p){ el.style.removeProperty(p); });\n" +
                "      st.lastY = null; st.lastWritten = null;\n" +
                "      if(!st.raf){ st.raf = requestAnimationFrame(st.tick); }\n" +
                "      return;\n" +
                "    }\n" +
                // compositor tier: a running animation outranks inline declarations in the cascade, so
                // animating transform still overrides the renderer's own inline transform. The renderer
                // writes styles one property at a time (style.setProperty), so these survive its renders.
                "    if(st.raf){ cancelAnimationFrame(st.raf); st.raf = null; }\n" +
                "    if(st.wroteTransform){ el.style.removeProperty('transform'); st.wroteTransform = false; }\n" +
                "    var r = st.range();\n" +
                "    st.style.textContent = '@keyframes ' + st.key +\n" +
                "      '{from{transform:translate(' + st.x + 'px,' + r.fromY + 'px);}' +\n" +
                "      'to{transform:translate(' + st.x + 'px,' + r.toY + 'px);}}';\n" +
                "    el.style.setProperty('animation-name', st.key);\n" +
                "    el.style.setProperty('animation-timing-function','linear');\n" +
                "    el.style.setProperty('animation-fill-mode','both');\n" +
                "    el.style.setProperty('animation-duration','auto');\n" +
                "    el.style.setProperty('animation-timeline','scroll(root block)');\n" +
                "    el.style.setProperty('animation-range', r.sPin + 'px ' + r.sRel + 'px');\n" +
                // read back what the engine kept. duration 0s means it dropped duration:auto, and with
                // fill-mode:both that snaps the node to the 'to' keyframe, i.e. off screen. an unresolved
                // timeline means the animation is not scroll-driven. either way fall through to the floor
                // rather than leave a broken pin: this catches partial support we have not tested for.
                "    var cs = getComputedStyle(el);\n" +
                "    if(cs.animationDuration === '0s' || cs.animationTimeline === 'auto'\n" +
                "       || cs.animationTimeline === 'none'){\n" +
                "      st.mode = 'raf'; st.apply();\n" +
                "    }\n" +
                "  };\n" +
                // bind to the element, never to its jpro-id: that id is a per-view transport index whose
                // counter restarts on reconnect, so a cached id can retarget an unrelated node. an element
                // reference only goes stale, which isConnected detects.
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
                // heartbeat: rebinds after a reconnect rebuilds the DOM peer, and carries the
                // layout-forcing measure(). the re-install can resolve the outgoing element before the
                // rebuild replaces it, so liveness is re-checked here rather than trusted once.
                "  st.check = function(){ if(st.dead) return;\n" +
                "    var prev = st.docExtent; st.measure();\n" +
                // the affix boundaries are derived from the document extent, so a page that grew or
                // shrank moves them. re-place from the heartbeat, not from scroll, since nothing
                // scrolled.
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

        // the binding is inline on the element and there are two loops running, so teardown has to stop
        // the heartbeat and the rAF pin and strip those properties, as well as drop the keyframes sheet.
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
                "  if(st.raf){ cancelAnimationFrame(st.raf); st.raf = null; }\n" +
                "  if(st.unlisten) st.unlisten();\n" +
                // the binding is inline on the element now, so it has to be stripped there too: the node
                // survives teardown (it is restored to its flow slot) and would keep the animation.
                "  if(st.clear) st.clear(st.el);\n" +
                "  st.el = null;\n" +
                "  if(st.style && st.style.parentNode) st.style.parentNode.removeChild(st.style);\n" +
                "  delete reg['" + jsKey + "'];\n" +
                "})();");
    }
}
