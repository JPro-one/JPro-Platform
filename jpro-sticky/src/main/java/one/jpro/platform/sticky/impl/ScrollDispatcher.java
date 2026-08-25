package one.jpro.platform.sticky.impl;

import com.jpro.webapi.WebAPI;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import one.jpro.platform.sticky.ScrollAnchor;
import one.jpro.platform.sticky.ScrollPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Selects and installs the concrete {@link ScrollImpl} for a node once it enters a scene, the point
 * at which its parent chain (and thus any {@link ScrollPane} ancestor) is realised. The choice is
 * invisible to callers: the positioning behaves the same whichever path is picked.
 *
 * <table>
 *   <caption>Selection</caption>
 *   <tr><th>Case</th><th>Implementation</th></tr>
 *   <tr><td>FIXED, browser</td><td>{@link WebScrollImpl} (compositor, viewport-anchored)</td></tr>
 *   <tr><td>FIXED, desktop</td><td>{@link DesktopFixedImpl} (scene-anchored overlay)</td></tr>
 *   <tr><td>STICKY, {@code ScrollPane} ancestor (desktop or browser)</td><td>{@link ScrollPaneStickyImpl}</td></tr>
 *   <tr><td>STICKY, browser, natively scrolled document</td><td>{@link WebScrollImpl}</td></tr>
 *   <tr><td>STICKY, desktop, no scroll ancestor</td><td>inert (nothing scrolls, so nothing pins)</td></tr>
 * </table>
 *
 * @author Tobias Horak
 */
public final class ScrollDispatcher implements ScrollImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScrollDispatcher.class);

    private final Node node;
    private final ScrollPosition position;
    private final ScrollAnchor anchor;
    private final Node within;
    /** Sink the chosen STICKY impl feeds pin/unpin transitions to; {@code null} for FIXED. */
    private final Consumer<Boolean> stuckSink;

    private ScrollImpl delegate;
    private ChangeListener<Scene> sceneWaiter;
    private boolean torndown;

    public ScrollDispatcher(Node node, ScrollPosition position, ScrollAnchor anchor, Node within,
                            Consumer<Boolean> stuckSink) {
        this.node = node;
        this.position = position;
        this.anchor = anchor;
        this.within = within;
        this.stuckSink = stuckSink;
    }

    @Override
    public void install() {
        if (node.getScene() != null) {
            choose();
        } else {
            // The parent chain is only guaranteed realised once the node is in a scene. Wait for it
            // so the ScrollPane-ancestor check (which decides FX vs web) sees the final tree.
            waitForScene();
        }
    }

    /**
     * Arms the one-shot scene listener that (re-)selects and installs the delegate the moment the
     * node enters a scene. Used both for the initial pre-scene wait and to re-pin after a detach.
     */
    private void waitForScene() {
        sceneWaiter = (obs, old, scene) -> {
            if (scene != null) {
                node.sceneProperty().removeListener(sceneWaiter);
                sceneWaiter = null;
                choose();
            }
        };
        node.sceneProperty().addListener(sceneWaiter);
    }

    /**
     * Invoked by a reparenting delegate ({@link WebScrollImpl} / {@link DesktopFixedImpl}) once its
     * flow slot (the placeholder) has left the scene, e.g. on a route navigate-away. The delegate is
     * uninstalled, which restores the node to its (now-detached) flow parent, but this dispatcher
     * stays alive: because the node is back in flow it tracks that subtree's scene again, so if the
     * route returns we re-select and re-install and the node re-pins. Without this the node would be
     * left in plain flow while {@link one.jpro.platform.sticky.Scroll#getScrollPosition} still reported
     * it as positioned.
     */
    private void onDelegateDetached() {
        if (torndown) {
            return;
        }
        if (delegate != null) {
            delegate.uninstall();
            delegate = null;
        }
        // The node left the scene, so it is no longer pinned: clear the stuck channel. The reparenting
        // teardown does not run through Scroll.setScrollPosition's central reset, so without this a node
        // that was stuck at navigate-away would keep reporting stuck. A re-pin (below, or on scene
        // re-entry) re-asserts it if the returning node is stuck again.
        if (stuckSink != null) {
            stuckSink.accept(false);
        }
        if (node.getScene() != null) {
            // Already back in a scene (a same-pulse return): re-pin now.
            choose();
        } else if (sceneWaiter == null) {
            waitForScene();
        }
    }

    private void choose() {
        if (torndown) {
            return;
        }
        delegate = select();
        if (delegate != null) {
            delegate.install();
        } else {
            LOGGER.debug("jpro-sticky: {} on {} is inert (desktop, no scroll ancestor)", position, node);
        }
    }

    private ScrollImpl select() {
        if (position == ScrollPosition.FIXED) {
            // Fixed is viewport/scene-anchored regardless of any ScrollPane ancestry. It never
            // transitions, so stuckSink is null here and no stuck state is published.
            return WebAPI.isBrowser()
                    ? new WebScrollImpl(node, position, anchor, within, stuckSink, this::onDelegateDetached)
                    : new DesktopFixedImpl(node, anchor, this::onDelegateDetached);
        }
        // STICKY: an FX ScrollPane ancestor means the scroll is server-driven (both on desktop and in
        // the browser), so the pure-FX pin stays in sync by construction.
        final ScrollPane scrollPane = nearestScrollPane(node);
        if (scrollPane != null) {
            // Stays in flow (no reparenting), so it tracks the route subtree and survives a re-mount on
            // its own; no detach callback needed.
            return new ScrollPaneStickyImpl(node, anchor, within, scrollPane, stuckSink);
        }
        if (WebAPI.isBrowser()) {
            // Natively scrolled browser document: the compositor override.
            return new WebScrollImpl(node, position, anchor, within, stuckSink, this::onDelegateDetached);
        }
        // Desktop with no scroll ancestor: nothing scrolls, so a sticky element never moves, the
        // same result CSS gives for a sticky element in a non-scrolling page.
        return null;
    }

    @Override
    public void uninstall() {
        torndown = true;
        if (sceneWaiter != null) {
            node.sceneProperty().removeListener(sceneWaiter);
            sceneWaiter = null;
        }
        if (delegate != null) {
            delegate.uninstall();
            delegate = null;
        }
    }

    /** Walks the node's parent chain and returns the nearest {@link ScrollPane} ancestor, or null. */
    private static ScrollPane nearestScrollPane(Node node) {
        Node cur = node.getParent();
        while (cur != null) {
            if (cur instanceof ScrollPane) {
                return (ScrollPane) cur;
            }
            cur = cur.getParent();
        }
        return null;
    }
}
