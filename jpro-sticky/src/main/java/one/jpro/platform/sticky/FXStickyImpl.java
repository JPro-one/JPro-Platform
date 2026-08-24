package one.jpro.platform.sticky;

import javafx.beans.InvalidationListener;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import one.jpro.platform.sticky.ScrollAnchor.Axis;
import one.jpro.platform.sticky.ScrollAnchor.Mode;

import java.util.function.Consumer;

/**
 * The {@link ScrollPosition#STICKY} implementation for a node inside a JavaFX {@link ScrollPane}.
 * Used on desktop, and also in the browser when the scroll is a server-driven FX {@code ScrollPane}
 * (so the pin stays in sync with the scroll by construction). Pure JavaFX: no reparenting, no
 * {@link com.jpro.webapi.WebAPI}, no core change.
 * <p>
 * The node stays in its flow slot; a {@code translate} holds it at the pin line while scrolled past,
 * clamped so it never leaves its containing block (the {@code within} override, else the node's
 * parent). The maths is the CSS sticky rule expressed in the ScrollPane's <em>content</em>
 * coordinates, where the node's natural position and the container bounds are constant and only the
 * scroll offset moves:
 * <pre>
 *   appear = natural
 *   if pinned-to-start: appear = max(appear, scrollOffset + startInset)     // can't pass the pin line
 *   if pinned-to-end:   appear = min(appear, scrollOffset + viewport - endInset - size)
 *   appear = clamp(appear, containerStart, containerEnd - size)             // stay in the block
 *   translate = appear - natural
 * </pre>
 * resolved independently on both axes. Reading the natural position subtracts the translate this
 * impl applied last pass, so re-syncs converge. While pinned the node gets a low {@code viewOrder}
 * so it paints above its scrolled siblings (picking follows {@code viewOrder} in FX).
 *
 * @author Tobias Horak
 */
final class FXStickyImpl implements ScrollImpl {

    /** viewOrder applied while stuck; lower paints in front (JPro/JavaFX sort by it first). */
    private static final double STUCK_VIEW_ORDER = -1.0;

    private final Node node;
    private final ScrollAnchor anchor;
    /** Containing block that bounds the pin; {@code null} falls back to the node's parent at attach. */
    private final Node within;
    private final ScrollPane scrollPane;
    /** Pin/unpin transition sink (the node's stuck channels); {@code null} if unobserved. */
    private final Consumer<Boolean> stuckSink;

    private Node content;
    private Node container;
    private double restingViewOrder;
    private boolean stuck;

    private final InvalidationListener relayout = obs -> sync();
    private boolean torndown;

    FXStickyImpl(Node node, ScrollAnchor anchor, Node within, ScrollPane scrollPane,
                 Consumer<Boolean> stuckSink) {
        this.node = node;
        this.anchor = anchor;
        this.within = within;
        this.scrollPane = scrollPane;
        this.stuckSink = stuckSink;
    }

    @Override
    public void install() {
        this.content = scrollPane.getContent();
        this.container = (within != null) ? within : node.getParent();
        this.restingViewOrder = node.getViewOrder();

        scrollPane.vvalueProperty().addListener(relayout);
        scrollPane.hvalueProperty().addListener(relayout);
        scrollPane.viewportBoundsProperty().addListener(relayout);
        if (content != null) {
            content.layoutBoundsProperty().addListener(relayout);
        }
        node.layoutBoundsProperty().addListener(relayout);
        if (container != null) {
            container.layoutBoundsProperty().addListener(relayout);
            container.localToSceneTransformProperty().addListener(relayout);
        }
        sync();
    }

    private void sync() {
        if (torndown || content == null) {
            return;
        }
        final Bounds vp = scrollPane.getViewportBounds();
        final double viewportW = vp.getWidth();
        final double viewportH = vp.getHeight();
        final Bounds nodeBounds = node.getLayoutBounds();
        final double nodeW = nodeBounds.getWidth();
        final double nodeH = nodeBounds.getHeight();

        // The node's natural top-left in content coordinates, reentrancy-safe (subtract the translate
        // applied last pass). Both transforms fold in the scroll, which cancels, so this stays stable.
        final Point2D nodeContentTL = content.sceneToLocal(node.localToScene(0, 0));
        final double naturalX = nodeContentTL.getX() - node.getTranslateX();
        final double naturalY = nodeContentTL.getY() - node.getTranslateY();

        // The containing block in content coordinates (defaults to the whole content if unresolved).
        final Bounds containerBounds = containerBoundsInContent();

        // How far each axis is currently scrolled, in content px.
        final double scrollX = scrollOffset(scrollPane.getHvalue(), scrollPane.getHmin(), scrollPane.getHmax(),
                content.getLayoutBounds().getWidth(), viewportW);
        final double scrollY = scrollOffset(scrollPane.getVvalue(), scrollPane.getVmin(), scrollPane.getVmax(),
                content.getLayoutBounds().getHeight(), viewportH);

        final double appearX = pin(anchor.horizontal(), naturalX, scrollX, viewportW, nodeW,
                containerBounds.getMinX(), containerBounds.getMaxX());
        final double appearY = pin(anchor.vertical(), naturalY, scrollY, viewportH, nodeH,
                containerBounds.getMinY(), containerBounds.getMaxY());

        node.setTranslateX(appearX - naturalX);
        node.setTranslateY(appearY - naturalY);

        final boolean nowStuck = (appearX != naturalX) || (appearY != naturalY);
        if (nowStuck != stuck) {
            stuck = nowStuck;
            node.setViewOrder(nowStuck ? STUCK_VIEW_ORDER : restingViewOrder);
            if (stuckSink != null) {
                stuckSink.accept(nowStuck);
            }
        }
    }

    /**
     * The CSS sticky clamp for one axis, in content coordinates: hold the node at the pin line while
     * scrolled past, but never outside its containing block.
     *
     * @return the axis position at which the node should appear
     */
    private static double pin(Axis axis, double natural, double scroll, double viewport, double size,
                              double containerStart, double containerEnd) {
        double appear = natural;
        if (axis.mode == Mode.PIN_START) {
            appear = Math.max(appear, scroll + axis.start);
        } else if (axis.mode == Mode.PIN_END) {
            appear = Math.min(appear, scroll + viewport - axis.end - size);
        }
        // Stay within the containing block (its bottom/right pushes the node back out, the release).
        final double maxStart = Math.max(containerStart, containerEnd - size);
        return Math.max(containerStart, Math.min(appear, maxStart));
    }

    /** The container's bounds mapped into the ScrollPane content's coordinate space. */
    private Bounds containerBoundsInContent() {
        if (container != null && container != content) {
            return content.sceneToLocal(container.localToScene(container.getLayoutBounds()));
        }
        return content.getLayoutBounds();
    }

    /** The scroll offset in content px for a scrollbar value: fraction of the scrollable extent. */
    private static double scrollOffset(double value, double min, double max,
                                       double contentExtent, double viewportExtent) {
        if (max <= min) {
            return 0.0;
        }
        final double scrollable = Math.max(0.0, contentExtent - viewportExtent);
        return (value - min) / (max - min) * scrollable;
    }

    @Override
    public void uninstall() {
        torndown = true;
        scrollPane.vvalueProperty().removeListener(relayout);
        scrollPane.hvalueProperty().removeListener(relayout);
        scrollPane.viewportBoundsProperty().removeListener(relayout);
        if (content != null) {
            content.layoutBoundsProperty().removeListener(relayout);
        }
        node.layoutBoundsProperty().removeListener(relayout);
        if (container != null) {
            container.layoutBoundsProperty().removeListener(relayout);
            container.localToSceneTransformProperty().removeListener(relayout);
        }
        // Restore the node to its natural flow position and paint order.
        node.setTranslateX(0);
        node.setTranslateY(0);
        if (stuck) {
            node.setViewOrder(restingViewOrder);
            stuck = false;
        }
    }
}
