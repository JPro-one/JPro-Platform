package one.jpro.platform.scroll;

import javafx.geometry.Side;
import javafx.scene.Node;
import one.jpro.platform.scroll.ScrollAnchor.Axis;
import one.jpro.platform.scroll.ScrollAnchor.Mode;
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
 * The API is layered. The canonical carrier is
 * {@link #setScrollPosition(Node, ScrollPosition, ScrollAnchor)}, taking a {@link ScrollAnchor}
 * that resolves the horizontal and vertical axes independently (corners, edge bars, centers,
 * full-viewport stretch). The {@code (Side, double)} and bare-position overloads are the
 * single-edge convenience over it, and the {@link #setStickyPosition} / {@link #setFixedPosition}
 * methods are thin delegators for everyday, hand-written call sites:
 * <pre>{@code
 * Scroll.setStickyPosition(header);                          // sticky, pinned to the top
 * Scroll.setFixedPosition(fab, javafx.geometry.Pos.BOTTOM_RIGHT, 24);  // fixed bottom-right corner
 * Scroll.setFixedBar(banner, Side.BOTTOM);                   // fixed full-width bottom bar
 * Scroll.clearScrollPosition(header);                        // back to normal flow
 * }</pre>
 * <p>
 * STICKY and FIXED share the {@link ScrollAnchor} carrier but STICKY only ever pins an edge (CSS
 * sticky neither centers nor stretches), so {@code setScrollPosition(node, STICKY, anchor)} rejects
 * {@link ScrollAnchor.Mode#CENTER} and {@link ScrollAnchor.Mode#STRETCH} axes.
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

    /** Property key under which the resolved {@link ScrollAnchor} is stored on a node. */
    private static final Object ANCHOR_KEY = new Object();

    /** Property key under which the active {@link ScrollOverride} is stashed on a node. */
    private static final Object OVERRIDE_KEY = new Object();

    private Scroll() {
        // utility class
    }

    // ---------------------------------------------------------------------
    // Sticky convenience — the everyday, hand-written API (edge-based)
    // ---------------------------------------------------------------------

    /**
     * Pins the node with {@link ScrollPosition#STICKY sticky} positioning to the top edge,
     * bounded by its containing block (its parent). Convenience for
     * {@code setScrollPosition(node, STICKY, Side.TOP, 0)}.
     *
     * @param node the node to position; must not be {@code null}
     */
    public static void setStickyPosition(Node node) {
        setStickyPosition(node, DEFAULT_SIDE, DEFAULT_OFFSET);
    }

    /**
     * Pins the node with {@link ScrollPosition#STICKY sticky} positioning to the given edge,
     * bounded by its containing block (its parent).
     *
     * @param node   the node to position; must not be {@code null}
     * @param side   the edge to pin to; must not be {@code null}
     * @param offset the inset (px) from that edge
     */
    public static void setStickyPosition(Node node, Side side, double offset) {
        setScrollPosition(node, ScrollPosition.STICKY, anchorForEdge(side, offset), null);
    }

    /**
     * Pins the node with {@link ScrollPosition#STICKY sticky} positioning to the given edge,
     * bounded by an explicit containing block (it rides up and releases at {@code within}'s bottom).
     *
     * @param node   the node to position; must not be {@code null}
     * @param side   the edge to pin to; must not be {@code null}
     * @param offset the inset (px) from that edge
     * @param within the containing block that bounds the pin; must not be {@code null}
     */
    public static void setStickyPosition(Node node, Side side, double offset, Node within) {
        if (within == null) {
            throw new NullPointerException("within must not be null");
        }
        setScrollPosition(node, ScrollPosition.STICKY, anchorForEdge(side, offset), within);
    }

    // ---------------------------------------------------------------------
    // Fixed convenience — corner / edge / stretch / center
    // ---------------------------------------------------------------------

    /**
     * Pins the node with {@link ScrollPosition#FIXED fixed} positioning to the top edge.
     *
     * @param node the node to position; must not be {@code null}
     */
    public static void setFixedPosition(Node node) {
        setScrollPosition(node, ScrollPosition.FIXED, anchorForEdge(DEFAULT_SIDE, DEFAULT_OFFSET), null);
    }

    /**
     * Pins the node with {@link ScrollPosition#FIXED fixed} positioning to the given edge, leaving
     * the perpendicular axis at its natural size (a chip, not a bar — use {@link #setFixedBar} for a
     * full-span bar).
     *
     * @param node   the node to position; must not be {@code null}
     * @param side   the edge to pin to; must not be {@code null}
     * @param offset the inset (px) from that edge
     */
    public static void setFixedPosition(Node node, Side side, double offset) {
        setScrollPosition(node, ScrollPosition.FIXED, anchorForEdge(side, offset), null);
    }

    /**
     * Pins the node with {@link ScrollPosition#FIXED fixed} positioning at an explicit
     * {@link ScrollAnchor} (corner, edge, center, or stretch).
     *
     * @param node   the node to position; must not be {@code null}
     * @param anchor the anchor; must not be {@code null}
     */
    public static void setFixedPosition(Node node, ScrollAnchor anchor) {
        setScrollPosition(node, ScrollPosition.FIXED, anchor, null);
    }

    /**
     * Pins the node with {@link ScrollPosition#FIXED fixed} positioning at one of the nine
     * {@link javafx.geometry.Pos} anchors (corners, edge centers, dead center), with no inset.
     *
     * @param node the node to position; must not be {@code null}
     * @param pos  the anchor position; must not be {@code null}
     */
    public static void setFixedPosition(Node node, javafx.geometry.Pos pos) {
        setFixedPosition(node, pos, DEFAULT_OFFSET);
    }

    /**
     * Pins the node with {@link ScrollPosition#FIXED fixed} positioning at one of the nine
     * {@link javafx.geometry.Pos} anchors, inset uniformly by {@code inset} from whichever edges the
     * position pins (centered axes are unaffected).
     *
     * @param node  the node to position; must not be {@code null}
     * @param pos   the anchor position; must not be {@code null}
     * @param inset the inset (px) from the pinned edges
     */
    public static void setFixedPosition(Node node, javafx.geometry.Pos pos, double inset) {
        if (pos == null) {
            throw new NullPointerException("pos must not be null");
        }
        setScrollPosition(node, ScrollPosition.FIXED, anchorForPos(pos, inset), null);
    }

    /**
     * Pins the node with {@link ScrollPosition#FIXED fixed} positioning as a full-span bar: pinned
     * to {@code side} at the edge and stretched across the perpendicular axis.
     *
     * @param node the node to position; must not be {@code null}
     * @param side the edge to pin the bar to; must not be {@code null}
     */
    public static void setFixedBar(Node node, Side side) {
        setFixedBar(node, side, DEFAULT_OFFSET);
    }

    /**
     * Pins the node with {@link ScrollPosition#FIXED fixed} positioning as a full-span bar, inset
     * {@code offset} from the pinned edge.
     *
     * @param node   the node to position; must not be {@code null}
     * @param side   the edge to pin the bar to; must not be {@code null}
     * @param offset the inset (px) from the pinned edge
     */
    public static void setFixedBar(Node node, Side side, double offset) {
        if (side == null) {
            throw new NullPointerException("side must not be null");
        }
        final ScrollAnchor anchor;
        switch (side) {
            case TOP:    anchor = ScrollAnchor.of().left(0).right(0).top(offset); break;
            case BOTTOM: anchor = ScrollAnchor.of().left(0).right(0).bottom(offset); break;
            case LEFT:   anchor = ScrollAnchor.of().top(0).bottom(0).left(offset); break;
            case RIGHT:  anchor = ScrollAnchor.of().top(0).bottom(0).right(offset); break;
            default:     anchor = ScrollAnchor.of().left(0).right(0).top(offset);
        }
        setScrollPosition(node, ScrollPosition.FIXED, anchor, null);
    }

    /**
     * Pins the node with {@link ScrollPosition#FIXED fixed} positioning stretched to fill the whole
     * viewport (a full-screen overlay). Shorthand for {@code setFixedPosition(node, of().all(0))}.
     *
     * @param node the node to position; must not be {@code null}
     */
    public static void setFixedFullscreen(Node node) {
        setScrollPosition(node, ScrollPosition.FIXED, ScrollAnchor.of().all(0), null);
    }

    /**
     * Clears any scroll positioning, returning the node to normal flow
     * ({@link ScrollPosition#STATIC}).
     *
     * @param node the node to reset; must not be {@code null}
     */
    public static void clearScrollPosition(Node node) {
        setScrollPosition(node, ScrollPosition.STATIC);
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
        setScrollPosition(node, position, anchorForEdge(DEFAULT_SIDE, DEFAULT_OFFSET), null);
    }

    /**
     * Applies a scroll positioning mode to the node, pinned to a single edge.
     *
     * @param node     the node to position; must not be {@code null}
     * @param position the positioning mode; must not be {@code null}
     * @param side     the edge to pin to; must not be {@code null} (ignored for {@link ScrollPosition#STATIC})
     * @param offset   the inset (px) from that edge (ignored for {@link ScrollPosition#STATIC})
     */
    public static void setScrollPosition(Node node, ScrollPosition position, Side side, double offset) {
        setScrollPosition(node, position, anchorForEdge(side, offset), null);
    }

    /**
     * Applies a scroll positioning mode to the node at an explicit {@link ScrollAnchor}.
     * Convenience for {@link #setScrollPosition(Node, ScrollPosition, ScrollAnchor, Node)} with no
     * explicit containment.
     *
     * @param node     the node to position; must not be {@code null}
     * @param position the positioning mode; must not be {@code null}
     * @param anchor   the anchor; must not be {@code null} (ignored for {@link ScrollPosition#STATIC})
     */
    public static void setScrollPosition(Node node, ScrollPosition position, ScrollAnchor anchor) {
        setScrollPosition(node, position, anchor, null);
    }

    /**
     * Applies a scroll positioning mode to the node at an explicit {@link ScrollAnchor}. This is the
     * canonical setter that every other method delegates to.
     * <p>
     * For {@link ScrollPosition#STICKY} the anchor must only pin edges — {@link ScrollAnchor.Mode#CENTER}
     * or {@link ScrollAnchor.Mode#STRETCH} axes are rejected — and {@code within} bounds the pin (the
     * node rides up and releases at the container's bottom); {@code null} defaults to the node's parent.
     * For {@link ScrollPosition#FIXED} the anchor may use the full model and {@code within} is ignored
     * (fixed is viewport-anchored).
     *
     * @param node     the node to position; must not be {@code null}
     * @param position the positioning mode; must not be {@code null}
     * @param anchor   the anchor; must not be {@code null} (ignored for {@link ScrollPosition#STATIC})
     * @param within   the STICKY containing block, or {@code null} to default to the node's parent
     * @throws IllegalArgumentException if {@code position} is STICKY and {@code anchor} centers or
     *                                  stretches an axis
     */
    public static void setScrollPosition(Node node, ScrollPosition position, ScrollAnchor anchor, Node within) {
        if (node == null) {
            throw new NullPointerException("node must not be null");
        }
        if (position == null) {
            throw new NullPointerException("position must not be null");
        }
        if (anchor == null) {
            throw new NullPointerException("anchor must not be null");
        }

        // Applying any mode must first tear down whatever was installed before, so
        // switching sticky <-> fixed (or clearing) never leaks the previous override/listeners.
        teardown(node);

        if (position == ScrollPosition.STATIC) {
            node.getProperties().remove(POSITION_KEY);
            node.getProperties().remove(ANCHOR_KEY);
            LOGGER.debug("Scroll position cleared for node {}", node);
            return;
        }

        if (position == ScrollPosition.STICKY) {
            rejectNonEdgeAnchor(anchor);
        }

        node.getProperties().put(POSITION_KEY, position);
        node.getProperties().put(ANCHOR_KEY, anchor);

        // Install the compositor override and stash it so teardown(node) can reverse it.
        final ScrollOverride override = new ScrollOverride(node, position, anchor, within);
        node.getProperties().put(OVERRIDE_KEY, override);
        override.install();
        LOGGER.debug("Scroll position {} applied to node {}", position, node);
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

    /** Builds a single-edge {@link ScrollAnchor} from a {@link Side} and offset. */
    private static ScrollAnchor anchorForEdge(Side side, double offset) {
        if (side == null) {
            throw new NullPointerException("side must not be null");
        }
        switch (side) {
            case TOP:    return ScrollAnchor.of().top(offset);
            case BOTTOM: return ScrollAnchor.of().bottom(offset);
            case LEFT:   return ScrollAnchor.of().left(offset);
            case RIGHT:  return ScrollAnchor.of().right(offset);
            default:     return ScrollAnchor.of().top(offset);
        }
    }

    /** Maps a {@link javafx.geometry.Pos} to a {@link ScrollAnchor}, insetting the pinned edges. */
    private static ScrollAnchor anchorForPos(javafx.geometry.Pos pos, double inset) {
        ScrollAnchor anchor = ScrollAnchor.of();
        switch (pos.getVpos()) {
            case TOP:    anchor = anchor.top(inset); break;
            case BOTTOM: anchor = anchor.bottom(inset); break;
            case CENTER: anchor = anchor.centerY(); break;
            default: break;
        }
        switch (pos.getHpos()) {
            case LEFT:   anchor = anchor.left(inset); break;
            case RIGHT:  anchor = anchor.right(inset); break;
            case CENTER: anchor = anchor.centerX(); break;
            default: break;
        }
        return anchor;
    }

    /**
     * Rejects a {@link ScrollAnchor} that centers or stretches either axis — STICKY pins edges only
     * (CSS sticky neither centers nor stretches).
     */
    private static void rejectNonEdgeAnchor(ScrollAnchor anchor) {
        if (isNonEdge(anchor.horizontal()) || isNonEdge(anchor.vertical())) {
            throw new IllegalArgumentException(
                    "STICKY supports edge pins only (top/bottom/left/right); "
                            + "CENTER and STRETCH anchors are FIXED-only");
        }
    }

    private static boolean isNonEdge(Axis axis) {
        return axis.mode == Mode.CENTER || axis.mode == Mode.STRETCH;
    }

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
