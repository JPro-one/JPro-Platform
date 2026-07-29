package one.jpro.platform.sticky;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The per-{@link Scene} overlay that hosts reparented sticky/fixed nodes, shared by the web
 * ({@link ScrollOverride}) and desktop ({@link FXFixedImpl}) implementations so the stacking order
 * is one consistent model regardless of which mechanism mounts a node.
 * <p>
 * Every mounted node is assigned a monotonic {@linkplain #nextStackOrder() stack order} in the
 * order {@code setScrollPosition} is called, and {@link #insertSorted} keeps the overlay's children
 * ordered by it. Paint/stacking order then follows source order (a later-declared node paints on
 * top — CSS's source-order tiebreaker) rather than the async order in which installs happen to
 * complete. {@code viewOrder} remains the explicit per-node override (JPro/JavaFX sort by it first).
 *
 * @author Tobias Horak
 */
final class StickyOverlay {

    /** {@link Scene} property key under which the shared overlay {@link Group} is cached. */
    private static final Object OVERLAY_KEY = new Object();

    /** {@link Node} property key stashing a mounted node's stack order, read by sibling mounts. */
    private static final Object STACK_ORDER_KEY = new Object();

    /**
     * Monotonic order in which mounts are created (i.e. the order {@code setScrollPosition} is
     * called), so paint/stacking order follows source order rather than install-completion order.
     */
    private static final AtomicLong STACK_SEQ = new AtomicLong();

    private StickyOverlay() {
        // utility class
    }

    /** @return the next source-order stack value; assign once per mounted node, at construction. */
    static long nextStackOrder() {
        return STACK_SEQ.getAndIncrement();
    }

    /**
     * Returns the scene's shared overlay, creating it on first use. A {@link Group} (not a
     * {@link Pane}): JPro picks server-side in the FX graph, so a Group's pick is the union of its
     * children (empty = transparent to clicks) whereas a full-document Pane would swallow them.
     * Unmanaged and left at layout origin, so it shares the scene's (document) coordinate space.
     *
     * @param scene the scene to host the overlay; must not be {@code null}
     * @return the overlay, or {@code null} if the scene root cannot host one
     */
    static Group forScene(Scene scene) {
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

    /**
     * Adds {@code node} to {@code overlay} at the index that keeps the overlay's children ordered by
     * {@code stackOrder} ascending, so a later-declared node ends up later in the list (painted on
     * top). Stashes the order on the node so sibling mounts can read it.
     *
     * @param overlay    the overlay to mount into; must not be {@code null}
     * @param node       the node to mount; must not be {@code null}
     * @param stackOrder the node's {@linkplain #nextStackOrder() stack order}
     */
    static void insertSorted(Group overlay, Node node, long stackOrder) {
        node.getProperties().put(STACK_ORDER_KEY, stackOrder);
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

    /**
     * Removes a mounted node from the overlay and drops its stashed stack order. A no-op if
     * {@code overlay} is {@code null} (the node was never mounted).
     *
     * @param overlay the overlay the node was mounted into, or {@code null}
     * @param node    the node to remove; must not be {@code null}
     */
    static void remove(Group overlay, Node node) {
        if (overlay != null) {
            overlay.getChildren().remove(node);
        }
        node.getProperties().remove(STACK_ORDER_KEY);
    }

    /** The stack order stashed on a mounted node, or {@link Long#MIN_VALUE} if absent. */
    private static long stackOrderOf(Node n) {
        final Object v = n.getProperties().get(STACK_ORDER_KEY);
        return (v instanceof Long) ? (Long) v : Long.MIN_VALUE;
    }
}
