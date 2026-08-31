package one.jpro.platform.sticky;

/**
 * The positioning mode of a node relative to the scrolling viewport, mirroring the
 * CSS {@code position} property. The mode drives how the node behaves as the page
 * scrolls and is applied via
 * {@link Scroll#setScrollPosition(javafx.scene.Node, ScrollPosition, ScrollAnchor)}.
 *
 * @author Tobias Horak
 */
public enum ScrollPosition {

    /**
     * Default flow positioning. The node scrolls with the content and is not
     * pinned to the viewport. Equivalent to CSS {@code position: static}.
     */
    STATIC,

    /**
     * Sticky positioning. The node scrolls with the content until it reaches the
     * configured offset from the edge of the scrolling container, after which it
     * stays pinned within its containing block. Equivalent to CSS
     * {@code position: sticky}.
     */
    STICKY,

    /**
     * Fixed positioning. The node is pinned to the viewport at the configured
     * offset and does not move while the page scrolls. Equivalent to CSS
     * {@code position: fixed}.
     */
    FIXED
}
