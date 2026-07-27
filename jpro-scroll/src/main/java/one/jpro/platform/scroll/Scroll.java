package one.jpro.platform.scroll;

import javafx.geometry.Side;
import javafx.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for applying scroll-aware positioning ({@link ScrollPosition#STICKY sticky}
 * and {@link ScrollPosition#FIXED fixed}) to JavaFX nodes rendered by JPro.
 * <p>
 * The positioning mode is attached to a node and, when running inside JPro, is realised
 * on the web side through a compositor override so that the node is pinned without a
 * round-trip to the JavaFX layout pass on every scroll event. When running as a desktop
 * application the mode is a no-op and the node keeps its normal flow positioning.
 * <p>
 * The API is layered. {@link #setScrollPosition(Node, ScrollPosition, Side, double)} is the
 * canonical method and is convenient for programmatic or data-driven callers (for example
 * applying a deserialized {@link ScrollPosition}). The {@link #setStickyPosition} and
 * {@link #setFixedPosition} methods are thin convenience delegators intended for everyday,
 * hand-written call sites:
 * <pre>{@code
 * Scroll.setStickyPosition(header);                 // sticky, pinned to the top
 * Scroll.setFixedPosition(fab, Side.BOTTOM, 24);    // fixed, 24px up from the bottom
 * Scroll.clearScrollPosition(header);               // back to normal flow
 * }</pre>
 *
 * @author Tobias Horak
 */
public final class Scroll {

    private static final Logger LOGGER = LoggerFactory.getLogger(Scroll.class);

    /** Default edge a node pins to when no side is supplied. */
    private static final Side DEFAULT_SIDE = Side.TOP;

    /** Default inset from the edge when no offset is supplied. */
    private static final double DEFAULT_OFFSET = 0.0;

    /** Property key under which the {@link ScrollPosition} is stored on a node. */
    private static final Object POSITION_KEY = new Object();

    /** Property key under which the pinning {@link Side} is stored on a node. */
    private static final Object SIDE_KEY = new Object();

    /** Property key under which the pinning offset (px) is stored on a node. */
    private static final Object OFFSET_KEY = new Object();

    /** Property key under which the active {@link ScrollOverride} is stashed on a node. */
    private static final Object OVERRIDE_KEY = new Object();

    private Scroll() {
        // utility class
    }

    // ---------------------------------------------------------------------
    // Convenience delegators — the everyday, hand-written API
    // ---------------------------------------------------------------------

    /**
     * Pins the node with {@link ScrollPosition#STICKY sticky} positioning to the top edge.
     * Convenience for {@code setScrollPosition(node, STICKY, Side.TOP, 0)}.
     *
     * @param node the node to position; must not be {@code null}
     */
    public static void setStickyPosition(Node node) {
        setScrollPosition(node, ScrollPosition.STICKY, DEFAULT_SIDE, DEFAULT_OFFSET);
    }

    /**
     * Pins the node with {@link ScrollPosition#STICKY sticky} positioning to the given edge.
     * Convenience for {@code setScrollPosition(node, STICKY, side, offset)}.
     *
     * @param node   the node to position; must not be {@code null}
     * @param side   the edge to pin to; must not be {@code null}
     * @param offset the inset (px) from that edge
     */
    public static void setStickyPosition(Node node, Side side, double offset) {
        setScrollPosition(node, ScrollPosition.STICKY, side, offset);
    }

    /**
     * Pins the node with {@link ScrollPosition#FIXED fixed} positioning to the top edge.
     * Convenience for {@code setScrollPosition(node, FIXED, Side.TOP, 0)}.
     *
     * @param node the node to position; must not be {@code null}
     */
    public static void setFixedPosition(Node node) {
        setScrollPosition(node, ScrollPosition.FIXED, DEFAULT_SIDE, DEFAULT_OFFSET);
    }

    /**
     * Pins the node with {@link ScrollPosition#FIXED fixed} positioning to the given edge.
     * Convenience for {@code setScrollPosition(node, FIXED, side, offset)}.
     *
     * @param node   the node to position; must not be {@code null}
     * @param side   the edge to pin to; must not be {@code null}
     * @param offset the inset (px) from that edge
     */
    public static void setFixedPosition(Node node, Side side, double offset) {
        setScrollPosition(node, ScrollPosition.FIXED, side, offset);
    }

    /**
     * Clears any scroll positioning, returning the node to normal flow
     * ({@link ScrollPosition#STATIC}). Convenience for
     * {@code setScrollPosition(node, STATIC, ...)}.
     *
     * @param node the node to reset; must not be {@code null}
     */
    public static void clearScrollPosition(Node node) {
        setScrollPosition(node, ScrollPosition.STATIC, DEFAULT_SIDE, DEFAULT_OFFSET);
    }

    // ---------------------------------------------------------------------
    // Canonical API — programmatic / data-driven callers
    // ---------------------------------------------------------------------

    /**
     * Applies a scroll positioning mode to the node, pinned to the top edge with no offset.
     * Convenience for {@code setScrollPosition(node, position, Side.TOP, 0)}.
     *
     * @param node     the node to position; must not be {@code null}
     * @param position the positioning mode; must not be {@code null}
     */
    public static void setScrollPosition(Node node, ScrollPosition position) {
        setScrollPosition(node, position, DEFAULT_SIDE, DEFAULT_OFFSET);
    }

    /**
     * Applies a scroll positioning mode to the node. This is the canonical setter that all
     * other convenience methods delegate to.
     *
     * @param node     the node to position; must not be {@code null}
     * @param position the positioning mode; must not be {@code null}
     * @param side     the edge to pin to; must not be {@code null} (ignored for {@link ScrollPosition#STATIC})
     * @param offset   the inset (px) from that edge (ignored for {@link ScrollPosition#STATIC})
     */
    public static void setScrollPosition(Node node, ScrollPosition position, Side side, double offset) {
        if (node == null) {
            throw new NullPointerException("node must not be null");
        }
        if (position == null) {
            throw new NullPointerException("position must not be null");
        }
        if (side == null) {
            throw new NullPointerException("side must not be null");
        }

        // Applying any mode must first tear down whatever was installed before, so
        // switching sticky <-> fixed (or clearing) never leaks the previous override/listeners.
        teardown(node);

        if (position == ScrollPosition.STATIC) {
            node.getProperties().remove(POSITION_KEY);
            node.getProperties().remove(SIDE_KEY);
            node.getProperties().remove(OFFSET_KEY);
            LOGGER.debug("Scroll position cleared for node {}", node);
            return;
        }

        node.getProperties().put(POSITION_KEY, position);
        node.getProperties().put(SIDE_KEY, side);
        node.getProperties().put(OFFSET_KEY, offset);

        // Install the compositor override and stash it so teardown(node) can reverse it.
        final ScrollOverride override = new ScrollOverride(node, position, side, offset);
        node.getProperties().put(OVERRIDE_KEY, override);
        override.install();
        LOGGER.debug("Scroll position {} ({} +{}) applied to node {}", position, side, offset, node);
    }

    /**
     * Returns the scroll positioning mode currently applied to the node.
     *
     * @param node the node to query; must not be {@code null}
     * @return the current {@link ScrollPosition}, or {@link ScrollPosition#STATIC} if none was set
     */
    public static ScrollPosition getScrollPosition(Node node) {
        if (node == null) {
            throw new NullPointerException("node must not be null");
        }
        final Object value = node.getProperties().get(POSITION_KEY);
        return (value instanceof ScrollPosition) ? (ScrollPosition) value : ScrollPosition.STATIC;
    }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    /**
     * Reverses any positioning currently installed on the node: uninstalls the compositor
     * override, deregisters listeners, and drops the teardown handle. A no-op when the node
     * is in normal flow.
     */
    private static void teardown(Node node) {
        final Object override = node.getProperties().remove(OVERRIDE_KEY);
        if (override instanceof ScrollOverride) {
            ((ScrollOverride) override).uninstall();
        }
    }
}
