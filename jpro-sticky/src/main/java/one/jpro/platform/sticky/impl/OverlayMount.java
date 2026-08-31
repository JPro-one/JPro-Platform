package one.jpro.platform.sticky.impl;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The shared reparent-into-overlay mechanic for the out-of-flow pinning strategies
 * ({@link WebScrollImpl} and {@link DesktopFixedImpl}). It lifts a node out of its flow parent into
 * the node's {@link StickyOverlay} (kept sorted by stack order) and leaves a layout-mirroring
 * {@link Placeholders placeholder} in the vacated slot so the surrounding flow does not shift;
 * {@link #unmount()} reverses it exactly, restoring the node to its original slot and paint state.
 * <p>
 * It owns only the mount bookkeeping (flow slot, placeholder, overlay). Each strategy keeps its own
 * geometry sync, reactive wiring, and the vertical footprint of the placeholder (STICKY reserves the
 * node height, FIXED collapses to 0): this class positions and sizes nothing.
 *
 * @author Tobias Horak
 */
final class OverlayMount {

    private static final Logger LOGGER = LoggerFactory.getLogger(OverlayMount.class);

    private final Node node;
    private final long stackOrder;

    private Pane originalParent;
    private int originalIndex = -1;
    private Group overlay;
    private Region placeholder;
    private boolean mounted;

    OverlayMount(Node node, long stackOrder) {
        this.node = node;
        this.stackOrder = stackOrder;
    }

    /**
     * Lifts the node into its overlay, leaving a mirrored placeholder in its flow slot. The placeholder
     * carries the node's constraints and width; its height is the caller's concern (see
     * {@link Placeholders}).
     *
     * @param reservedHeight height the placeholder holds in the flow slot (the node's height for STICKY,
     *                       {@code 0} for FIXED). Set before insertion so the first layout honours it; a
     *                       {@code setPrefHeight} after insertion only requests a re-layout the pulse can drop.
     * @return the placeholder now holding the node's flow slot, or {@code null} if the node could not
     *         be mounted (its parent is not a {@link Pane}, no overlay host resolved, or the node is
     *         not in its parent's children); on {@code null} the node is left untouched in flow
     */
    Region mount(double reservedHeight) {
        final Parent parent = node.getParent();
        if (!(parent instanceof Pane)) {
            LOGGER.warn("jpro-sticky: node's parent is {} (not a Pane); cannot pin {}. Node stays in flow.",
                    parent == null ? "null" : parent.getClass().getSimpleName(), node);
            return null;
        }
        final Group ov = StickyOverlay.overlayForNode(node);
        if (ov == null) {
            LOGGER.warn("jpro-sticky: no overlay host for {} (outside a scene, or host not a Pane/Group)."
                    + " Node stays in flow.", node);
            return null;
        }
        final Pane pane = (Pane) parent;
        final int index = pane.getChildren().indexOf(node);
        if (index < 0) {
            return null;
        }

        this.originalParent = pane;
        this.originalIndex = index;
        this.overlay = ov;

        // mirror the node's constraints/width onto the placeholder so the flow slot does not shift, swap
        // node -> placeholder, and mount into the overlay (insertSorted keeps it stack-ordered).
        final Region ph = new Region();
        ph.setMaxWidth(Double.MAX_VALUE);
        // a rigid floor, not just a preference: the slot stands in for an out-of-flow node, so a
        // space-tight parent must not shrink it (a plain Region's min height is 0, the first to collapse).
        ph.setMinHeight(reservedHeight);
        ph.setPrefHeight(reservedHeight);
        Placeholders.mirror(node, ph);
        pane.getChildren().set(index, ph);
        node.setManaged(false);
        StickyOverlay.insertSorted(overlay, node, stackOrder);

        this.placeholder = ph;
        this.mounted = true;
        return ph;
    }

    /**
     * Reverses {@link #mount()}: pulls the node out of the overlay and back into its original flow slot,
     * restoring its managed state. A no-op if the node is not currently mounted.
     */
    void unmount() {
        if (!mounted) {
            return;
        }
        StickyOverlay.remove(overlay, node);
        if (originalParent != null && placeholder != null) {
            final int idx = originalParent.getChildren().indexOf(placeholder);
            if (idx >= 0) {
                originalParent.getChildren().set(idx, node);
            }
            node.setManaged(true);
        }
        mounted = false;
    }

    /** The overlay the node is mounted into; {@code null} until a successful {@link #mount()}. */
    Group overlay() {
        return overlay;
    }

    /** The placeholder holding the node's flow slot; {@code null} until a successful {@link #mount()}. */
    Region placeholder() {
        return placeholder;
    }

    /** The node's flow parent captured at {@link #mount()}; {@code null} until a successful mount. */
    Pane originalParent() {
        return originalParent;
    }
}
