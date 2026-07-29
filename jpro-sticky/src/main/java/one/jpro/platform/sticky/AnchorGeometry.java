package one.jpro.platform.sticky;

import javafx.scene.Node;
import javafx.scene.layout.Region;
import one.jpro.platform.sticky.ScrollAnchor.Axis;

import java.util.function.DoubleUnaryOperator;

/**
 * Resolves a {@link ScrollAnchor} plus an available (viewport/scene) size into the pinned geometry
 * of a node: its size ({@link #nodeW}/{@link #nodeH}) and the position of its top-left within the
 * available box ({@link #x}/{@link #y0}). The two axes are resolved independently (STICKY_DESIGN.md
 * §18): each of {@code NATURAL / PIN_START / PIN_END / CENTER / STRETCH} maps to a size and offset.
 * <p>
 * This is shared by the web ({@link ScrollOverride}, resolving against the browser viewport) and the
 * desktop ({@link FXFixedImpl}, resolving against the scene) so both compute <em>identical</em>
 * geometry from the same anchor — the mechanical guarantee that the desktop/web split is invisible.
 *
 * @author Tobias Horak
 */
final class AnchorGeometry {

    /** The node's resolved top-left x within the available box. */
    final double x;
    /** The node's resolved top-left y within the available box (the vertical pin line). */
    final double y0;
    /** The node's resolved width. */
    final double nodeW;
    /** The node's resolved height. */
    final double nodeH;

    private AnchorGeometry(double x, double y0, double nodeW, double nodeH) {
        this.x = x;
        this.y0 = y0;
        this.nodeW = nodeW;
        this.nodeH = nodeH;
    }

    /**
     * Resolves {@code anchor} against an available box of {@code availW} x {@code availH}.
     *
     * @param anchor         the anchor to resolve; must not be {@code null}
     * @param availW         the available width (viewport/scene)
     * @param availH         the available height (viewport/scene)
     * @param naturalW       the node's natural (unstretched) width
     * @param naturalHForW   the node's natural height as a function of its resolved width
     * @param flowX          the node's x when left NATURAL (its in-flow left)
     * @param flowW          the node's width when left NATURAL and not {@code fixed} (its flow width)
     * @param fixed          {@code true} for FIXED (a NATURAL axis takes the natural size, not the flow width)
     * @return the resolved geometry
     */
    static AnchorGeometry resolve(ScrollAnchor anchor, double availW, double availH,
                                  double naturalW, DoubleUnaryOperator naturalHForW,
                                  double flowX, double flowW, boolean fixed) {
        final Axis hz = anchor.horizontal();
        final Axis vt = anchor.vertical();

        // --- Horizontal axis: node width and the constant x. ---
        final double nodeW;
        final double x;
        switch (hz.mode) {
            case STRETCH:
                nodeW = Math.max(0, availW - hz.start - hz.end);
                x = hz.start;
                break;
            case PIN_START:
                nodeW = naturalW;
                x = hz.start;
                break;
            case PIN_END:
                nodeW = naturalW;
                x = availW - nodeW - hz.end;
                break;
            case CENTER:
                nodeW = naturalW;
                x = (availW - nodeW) / 2.0 + hz.start;
                break;
            default: // NATURAL: sticky keeps its full flow width; fixed is a natural-width chip.
                nodeW = fixed ? naturalW : flowW;
                x = flowX;
        }

        // --- Vertical axis: node height and the pin line y0. ---
        final double nodeH;
        final double y0;
        switch (vt.mode) {
            case STRETCH:
                nodeH = Math.max(0, availH - vt.start - vt.end);
                y0 = vt.start;
                break;
            case PIN_END:
                nodeH = naturalHForW.applyAsDouble(nodeW);
                y0 = availH - nodeH - vt.end;
                break;
            case CENTER:
                nodeH = naturalHForW.applyAsDouble(nodeW);
                y0 = (availH - nodeH) / 2.0 + vt.start;
                break;
            default: // NATURAL / PIN_START: pin line is the start inset (0 for a bare NATURAL).
                nodeH = naturalHForW.applyAsDouble(nodeW);
                y0 = vt.start;
        }

        return new AnchorGeometry(x, y0, nodeW, nodeH);
    }

    /** @return whether any axis needs a real available size (end/center/stretch anchors). */
    static boolean needsAvailableSize(ScrollAnchor anchor) {
        return isSizeDependent(anchor.horizontal()) || isSizeDependent(anchor.vertical());
    }

    private static boolean isSizeDependent(Axis axis) {
        return axis.mode == ScrollAnchor.Mode.PIN_END
                || axis.mode == ScrollAnchor.Mode.CENTER
                || axis.mode == ScrollAnchor.Mode.STRETCH;
    }

    /** The node's natural (preferred, min-honouring) width. */
    static double naturalWidth(Node node) {
        if (node instanceof Region) {
            final Region r = (Region) node;
            return Math.max(r.prefWidth(-1), r.minWidth(-1));
        }
        return node.getLayoutBounds().getWidth();
    }

    /**
     * The node's natural height at {@code forWidth}. {@code prefHeight} alone ignores {@code minHeight},
     * so a min-constrained node would reserve too little — take the max of both.
     */
    static double naturalHeight(Node node, double forWidth) {
        if (node instanceof Region) {
            final Region r = (Region) node;
            return Math.max(r.prefHeight(forWidth), r.minHeight(forWidth));
        }
        return node.getLayoutBounds().getHeight();
    }
}
