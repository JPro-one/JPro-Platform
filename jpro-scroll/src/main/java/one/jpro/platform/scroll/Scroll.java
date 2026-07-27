package one.jpro.platform.scroll;

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
 * Usage:
 * <pre>{@code
 * Node header = ...;
 * Scroll.setPosition(header, ScrollPosition.STICKY);
 * }</pre>
 *
 * @author Tobias Horak
 */
public final class Scroll {

    private static final Logger LOGGER = LoggerFactory.getLogger(Scroll.class);

    /** Property key under which the {@link ScrollPosition} is stored on a node. */
    private static final Object POSITION_KEY = new Object();

    private Scroll() {
        // utility class
    }

    /**
     * Applies a scroll positioning mode to the given node.
     *
     * @param node     the node to position; must not be {@code null}
     * @param position the positioning mode; must not be {@code null}
     */
    public static void setPosition(Node node, ScrollPosition position) {
        if (node == null) {
            throw new NullPointerException("node must not be null");
        }
        if (position == null) {
            throw new NullPointerException("position must not be null");
        }
        node.getProperties().put(POSITION_KEY, position);

        // TODO(M3): install/refresh the compositor override for STICKY and FIXED
        //  (ported from the jpro-scenegraph compositor override proven in M1) and
        //  tear it down when the mode returns to STATIC.
        LOGGER.debug("Scroll position {} requested for node {}", position, node);
    }

    /**
     * Returns the scroll positioning mode currently applied to the given node.
     *
     * @param node the node to query; must not be {@code null}
     * @return the current {@link ScrollPosition}, or {@link ScrollPosition#STATIC} if none was set
     */
    public static ScrollPosition getPosition(Node node) {
        if (node == null) {
            throw new NullPointerException("node must not be null");
        }
        final Object value = node.getProperties().get(POSITION_KEY);
        return (value instanceof ScrollPosition) ? (ScrollPosition) value : ScrollPosition.STATIC;
    }
}
