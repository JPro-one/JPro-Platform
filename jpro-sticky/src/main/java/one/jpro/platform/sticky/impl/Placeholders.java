package one.jpro.platform.sticky.impl;

import javafx.scene.Node;
import javafx.scene.layout.Region;

/**
 * Builds the layout-mirroring placeholder that both the web ({@link WebScrollImpl}) and desktop
 * ({@link DesktopFixedImpl}) paths leave in a pinned node's flow slot when they reparent it into the
 * {@link StickyOverlay}.
 * <p>
 * Reparenting moves the node out of its flow parent, so the placeholder has to stand in for it or the
 * surrounding layout shifts: without the node's immediate-parent constraints the slot loses its
 * {@code HBox}/{@code VBox} grow + margins, its {@code GridPane} row/column/span and alignment, etc.,
 * and without its footprint the siblings reclaim the freed space. {@link #mirror} copies both onto the
 * placeholder so the slot behaves as it did with the node in it.
 * <p>
 * <strong>Width vs height.</strong> This mirrors the constraint keys and the horizontal footprint.
 * The vertical footprint stays the caller's concern via {@code prefHeight}, because it differs by mode:
 * STICKY keeps the node height (the slot stays reserved), FIXED collapses to 0 (it is out of flow), so
 * pinning a {@code minHeight} here would fight the FIXED collapse. The caller keeps
 * {@code maxWidth = MAX_VALUE} for fill.
 *
 * @author Tobias Horak
 */
final class Placeholders {

    /**
     * Property-key prefixes JavaFX's standard layout panes use to stash a child's per-parent
     * constraints (e.g. {@code hbox-hgrow}, {@code gridpane-column-span}, {@code borderpane-alignment}).
     * A node's constraints live in its {@code getProperties()} under a key with one of these prefixes;
     * copying those entries carries the constraints to the placeholder. Custom panes with their own
     * key scheme are not covered (documented in the module README).
     */
    private static final String[] CONSTRAINT_PREFIXES = {
            "hbox-", "vbox-", "gridpane-", "stackpane-", "borderpane-",
            "anchorpane-", "flowpane-", "tilepane-", "pane-"
    };

    private Placeholders() {
        // utility class
    }

    /**
     * Copies {@code node}'s immediate-parent layout constraints and horizontal footprint onto
     * {@code placeholder}, so the flow slot the placeholder holds keeps the node's grow/margin/span/
     * alignment and reserves its width. Call before adding the placeholder to the parent, so the parent
     * reads the constraints on its first layout pass. Vertical footprint is left to the caller
     * ({@code prefHeight}); see the class note.
     *
     * @param node        the node being pinned (still in its flow slot); must not be {@code null}
     * @param placeholder the placeholder standing in for it; must not be {@code null}
     */
    static void mirror(Node node, Region placeholder) {
        node.getProperties().forEach((key, value) -> {
            if (key instanceof String && hasConstraintPrefix((String) key)) {
                placeholder.getProperties().put(key, value);
            }
        });

        // Reserve the node's on-screen width so siblings don't reclaim the slot. prefWidth (a
        // preference, not a floor) lets a fill slot still shrink with its container. An explicit
        // minWidth the node itself enforced is carried through as a real floor.
        final double width = node.getLayoutBounds().getWidth();
        if (width > 0) {
            placeholder.setPrefWidth(width);
        }
        if (node instanceof Region && ((Region) node).getMinWidth() > 0) {
            placeholder.setMinWidth(((Region) node).getMinWidth());
        }
    }

    private static boolean hasConstraintPrefix(String key) {
        for (String prefix : CONSTRAINT_PREFIXES) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
