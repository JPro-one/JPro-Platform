package one.jpro.platform.scroll;

/**
 * An immutable description of where a {@link ScrollPosition#STICKY sticky} or
 * {@link ScrollPosition#FIXED fixed} node is anchored, resolved on the horizontal and vertical
 * axes <em>independently</em>. Each axis carries exactly one {@link Mode}:
 * <ul>
 *   <li>{@link Mode#NATURAL} — keep the node's flow position/size on that axis (nothing set);</li>
 *   <li>{@link Mode#PIN_START} — pin to the top/left edge at an offset ({@link #top}/{@link #left});</li>
 *   <li>{@link Mode#PIN_END} — pin to the bottom/right edge at an offset ({@link #bottom}/{@link #right});</li>
 *   <li>{@link Mode#CENTER} — center on that axis, an offset shifts toward the end
 *       ({@link #centerX}/{@link #centerY});</li>
 *   <li>{@link Mode#STRETCH} — resize the node to span both edges of the axis (both a start and an
 *       end offset set on the same axis).</li>
 * </ul>
 * A corner is a PIN on each axis, an edge bar is a STRETCH on one axis and a PIN on the other, a
 * full-viewport overlay is a STRETCH on both, and a centered toast is a PIN on one axis and a CENTER
 * on the other. This carrier is shared by sticky and fixed positioning; they differ only in the
 * reference frame (the containing block versus the viewport), which {@link ScrollPosition} selects.
 * <p>
 * Anchors are built fluently from {@link #of()} (NATURAL on both axes); every wither returns a new
 * instance, so an anchor is safe to share. Setting two conflicting things on one axis (for example
 * {@link #centerX()} and {@link #left(double)}) throws {@link IllegalStateException}.
 * <pre>{@code
 * ScrollAnchor.of().bottom(0);                     // bottom edge (a PIN_END on the vertical axis)
 * ScrollAnchor.of().bottom(24).right(24);          // bottom-right corner
 * ScrollAnchor.of().top(0).left(0).right(0);       // top bar: pinned top, stretched horizontally
 * ScrollAnchor.of().top(16).centerX();             // toast: pinned near the top, horizontally centered
 * ScrollAnchor.of().all(0);                         // full-viewport overlay
 * }</pre>
 *
 * @author Tobias Horak
 */
public final class ScrollAnchor {

    /** The anchoring mode of a single axis. */
    enum Mode {
        /** Keep the node's flow position (and size) on this axis. */
        NATURAL,
        /** Pin to the start edge (top or left) at {@link Axis#start}. */
        PIN_START,
        /** Pin to the end edge (bottom or right) at {@link Axis#end}. */
        PIN_END,
        /** Center on this axis; {@link Axis#start} shifts the center toward the end edge. */
        CENTER,
        /** Stretch the node to span both edges, insetting {@link Axis#start} and {@link Axis#end}. */
        STRETCH
    }

    /**
     * The resolved anchoring of one axis: a {@link Mode} plus its offsets. For
     * {@link Mode#PIN_START} the inset is {@link #start}, for {@link Mode#PIN_END} it is {@link #end},
     * for {@link Mode#CENTER} the shift is {@link #start}, and for {@link Mode#STRETCH} both apply.
     */
    static final class Axis {
        static final Axis NATURAL = new Axis(Mode.NATURAL, 0, 0);

        final Mode mode;
        final double start;
        final double end;

        private Axis(Mode mode, double start, double end) {
            this.mode = mode;
            this.start = start;
            this.end = end;
        }

        /** Claims the start edge (top/left) of this axis, or throws on a conflict with CENTER. */
        Axis withStart(double off) {
            switch (mode) {
                case CENTER:
                    throw new IllegalStateException(
                            "axis is already centered; cannot also pin its start edge");
                case PIN_END:
                case STRETCH:
                    return new Axis(Mode.STRETCH, off, end);
                default: // NATURAL, PIN_START
                    return new Axis(Mode.PIN_START, off, end);
            }
        }

        /** Claims the end edge (bottom/right) of this axis, or throws on a conflict with CENTER. */
        Axis withEnd(double off) {
            switch (mode) {
                case CENTER:
                    throw new IllegalStateException(
                            "axis is already centered; cannot also pin its end edge");
                case PIN_START:
                case STRETCH:
                    return new Axis(Mode.STRETCH, start, off);
                default: // NATURAL, PIN_END
                    return new Axis(Mode.PIN_END, start, off);
            }
        }

        /** Centers this axis, or throws if an edge is already pinned/stretched. */
        Axis withCenter(double shift) {
            if (mode == Mode.PIN_START || mode == Mode.PIN_END || mode == Mode.STRETCH) {
                throw new IllegalStateException(
                        "axis already pins an edge; cannot also center it");
            }
            return new Axis(Mode.CENTER, shift, 0);
        }
    }

    private final Axis horizontal;
    private final Axis vertical;

    private ScrollAnchor(Axis horizontal, Axis vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    /**
     * Returns an anchor that is {@link Mode#NATURAL} on both axes — the starting point for the
     * fluent withers.
     *
     * @return a fresh anchor with no axis claimed
     */
    public static ScrollAnchor of() {
        return new ScrollAnchor(Axis.NATURAL, Axis.NATURAL);
    }

    /**
     * Pins the top edge at {@code px}. Combined with {@link #bottom(double)} this stretches the
     * node vertically.
     *
     * @param px the inset (px) from the top edge
     * @return a new anchor with the vertical axis updated
     * @throws IllegalStateException if the vertical axis is already centered
     */
    public ScrollAnchor top(double px) {
        return new ScrollAnchor(horizontal, vertical.withStart(px));
    }

    /**
     * Pins the bottom edge at {@code px}. Combined with {@link #top(double)} this stretches the
     * node vertically.
     *
     * @param px the inset (px) from the bottom edge
     * @return a new anchor with the vertical axis updated
     * @throws IllegalStateException if the vertical axis is already centered
     */
    public ScrollAnchor bottom(double px) {
        return new ScrollAnchor(horizontal, vertical.withEnd(px));
    }

    /**
     * Pins the left edge at {@code px}. Combined with {@link #right(double)} this stretches the
     * node horizontally.
     *
     * @param px the inset (px) from the left edge
     * @return a new anchor with the horizontal axis updated
     * @throws IllegalStateException if the horizontal axis is already centered
     */
    public ScrollAnchor left(double px) {
        return new ScrollAnchor(horizontal.withStart(px), vertical);
    }

    /**
     * Pins the right edge at {@code px}. Combined with {@link #left(double)} this stretches the
     * node horizontally.
     *
     * @param px the inset (px) from the right edge
     * @return a new anchor with the horizontal axis updated
     * @throws IllegalStateException if the horizontal axis is already centered
     */
    public ScrollAnchor right(double px) {
        return new ScrollAnchor(horizontal.withEnd(px), vertical);
    }

    /**
     * Centers the node horizontally.
     *
     * @return a new anchor with the horizontal axis centered
     * @throws IllegalStateException if the horizontal axis already pins an edge
     */
    public ScrollAnchor centerX() {
        return centerX(0);
    }

    /**
     * Centers the node horizontally, shifted by {@code shift} toward the right edge.
     *
     * @param shift px to shift the center toward the right (negative shifts left)
     * @return a new anchor with the horizontal axis centered
     * @throws IllegalStateException if the horizontal axis already pins an edge
     */
    public ScrollAnchor centerX(double shift) {
        return new ScrollAnchor(horizontal.withCenter(shift), vertical);
    }

    /**
     * Centers the node vertically.
     *
     * @return a new anchor with the vertical axis centered
     * @throws IllegalStateException if the vertical axis already pins an edge
     */
    public ScrollAnchor centerY() {
        return centerY(0);
    }

    /**
     * Centers the node vertically, shifted by {@code shift} toward the bottom edge.
     *
     * @param shift px to shift the center toward the bottom (negative shifts up)
     * @return a new anchor with the vertical axis centered
     * @throws IllegalStateException if the vertical axis already pins an edge
     */
    public ScrollAnchor centerY(double shift) {
        return new ScrollAnchor(horizontal, vertical.withCenter(shift));
    }

    /**
     * Stretches the node to fill both axes, insetting {@code px} from every edge — the
     * full-viewport overlay case. Shorthand for {@code top(px).right(px).bottom(px).left(px)}.
     *
     * @param px the uniform inset (px) from all four edges
     * @return a new anchor stretched on both axes
     */
    public ScrollAnchor all(double px) {
        return top(px).right(px).bottom(px).left(px);
    }

    /** @return the resolved horizontal axis (package-private, for the override impl). */
    Axis horizontal() {
        return horizontal;
    }

    /** @return the resolved vertical axis (package-private, for the override impl). */
    Axis vertical() {
        return vertical;
    }
}
