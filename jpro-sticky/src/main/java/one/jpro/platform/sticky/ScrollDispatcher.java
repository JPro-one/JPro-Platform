package one.jpro.platform.sticky;

import com.jpro.webapi.WebAPI;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
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
 *   <tr><td>FIXED, browser</td><td>{@link ScrollOverride} (compositor, viewport-anchored)</td></tr>
 *   <tr><td>FIXED, desktop</td><td>{@link FXFixedImpl} (scene-anchored overlay)</td></tr>
 *   <tr><td>STICKY, {@code ScrollPane} ancestor (desktop or browser)</td><td>{@link FXStickyImpl}</td></tr>
 *   <tr><td>STICKY, browser, natively scrolled document</td><td>{@link ScrollOverride}</td></tr>
 *   <tr><td>STICKY, desktop, no scroll ancestor</td><td>inert (nothing scrolls, so nothing pins)</td></tr>
 * </table>
 *
 * @author Tobias Horak
 */
final class ScrollDispatcher implements ScrollImpl {

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

    ScrollDispatcher(Node node, ScrollPosition position, ScrollAnchor anchor, Node within,
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
            sceneWaiter = (obs, old, scene) -> {
                if (scene != null) {
                    node.sceneProperty().removeListener(sceneWaiter);
                    sceneWaiter = null;
                    choose();
                }
            };
            node.sceneProperty().addListener(sceneWaiter);
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
                    ? new ScrollOverride(node, position, anchor, within, stuckSink)
                    : new FXFixedImpl(node, anchor);
        }
        // STICKY: an FX ScrollPane ancestor means the scroll is server-driven (both on desktop and in
        // the browser), so the pure-FX pin stays in sync by construction.
        final ScrollPane scrollPane = nearestScrollPane(node);
        if (scrollPane != null) {
            return new FXStickyImpl(node, anchor, within, scrollPane, stuckSink);
        }
        if (WebAPI.isBrowser()) {
            // Natively scrolled browser document: the compositor override.
            return new ScrollOverride(node, position, anchor, within, stuckSink);
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
