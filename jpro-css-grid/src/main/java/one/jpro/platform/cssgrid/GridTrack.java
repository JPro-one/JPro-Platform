package one.jpro.platform.cssgrid;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A single entry of a CSS grid track list: a track size such as {@code 200}, {@code 1fr}, {@code auto},
 * {@code minmax(100, 1fr)}, or a {@code repeat(...)} group.
 * <p>
 * Instances are immutable; create them with the static factory methods or by parsing CSS text
 * via {@link GridTrackList#parse(String)}.
 */
public final class GridTrack {

    public enum Kind {
        /** Fixed size in pixels. */
        FIXED,
        /** Percentage of the container's content size (treated as {@code auto} when that size is indefinite). */
        PERCENT,
        /** Flexible size, a share of the remaining free space. */
        FLEX,
        AUTO,
        MIN_CONTENT,
        MAX_CONTENT,
        /** {@code minmax(min, max)}. */
        MINMAX,
        /** {@code repeat(count | auto-fill | auto-fit, tracks...)}. */
        REPEAT
    }

    public enum RepeatMode { COUNT, AUTO_FILL, AUTO_FIT }

    private static final GridTrack AUTO = new GridTrack(Kind.AUTO, 0, null, null, 0, null, Collections.emptyList());
    private static final GridTrack MIN_CONTENT = new GridTrack(Kind.MIN_CONTENT, 0, null, null, 0, null, Collections.emptyList());
    private static final GridTrack MAX_CONTENT = new GridTrack(Kind.MAX_CONTENT, 0, null, null, 0, null, Collections.emptyList());

    private final Kind kind;
    private final double value;
    private final GridTrack min;
    private final GridTrack max;
    private final int repeatCount;
    private final RepeatMode repeatMode;
    private final List<GridTrack> repeated;

    private GridTrack(Kind kind, double value, GridTrack min, GridTrack max,
                      int repeatCount, RepeatMode repeatMode, List<GridTrack> repeated) {
        this.kind = kind;
        this.value = value;
        this.min = min;
        this.max = max;
        this.repeatCount = repeatCount;
        this.repeatMode = repeatMode;
        this.repeated = repeated;
    }

    public static GridTrack px(double pixels) {
        if (pixels < 0 || Double.isNaN(pixels)) throw new IllegalArgumentException("Track size must be >= 0: " + pixels);
        return new GridTrack(Kind.FIXED, pixels, null, null, 0, null, Collections.emptyList());
    }

    public static GridTrack percent(double percent) {
        if (percent < 0 || Double.isNaN(percent)) throw new IllegalArgumentException("Percentage must be >= 0: " + percent);
        return new GridTrack(Kind.PERCENT, percent, null, null, 0, null, Collections.emptyList());
    }

    public static GridTrack fr(double factor) {
        if (factor < 0 || Double.isNaN(factor)) throw new IllegalArgumentException("Flex factor must be >= 0: " + factor);
        return new GridTrack(Kind.FLEX, factor, null, null, 0, null, Collections.emptyList());
    }

    public static GridTrack auto() { return AUTO; }

    public static GridTrack minContent() { return MIN_CONTENT; }

    public static GridTrack maxContent() { return MAX_CONTENT; }

    /**
     * {@code minmax(min, max)}. The minimum may not be flexible; neither side may be a {@code minmax} or {@code repeat}.
     */
    public static GridTrack minmax(GridTrack min, GridTrack max) {
        Objects.requireNonNull(min, "min");
        Objects.requireNonNull(max, "max");
        if (min.kind == Kind.FLEX || min.kind == Kind.MINMAX || min.kind == Kind.REPEAT) {
            throw new IllegalArgumentException("Invalid minmax() minimum: " + min);
        }
        if (max.kind == Kind.MINMAX || max.kind == Kind.REPEAT) {
            throw new IllegalArgumentException("Invalid minmax() maximum: " + max);
        }
        return new GridTrack(Kind.MINMAX, 0, min, max, 0, null, Collections.emptyList());
    }

    /** {@code repeat(count, tracks...)}. */
    public static GridTrack repeat(int count, GridTrack... tracks) {
        if (count < 1) throw new IllegalArgumentException("repeat() count must be >= 1: " + count);
        return repeat(RepeatMode.COUNT, count, Arrays.asList(tracks));
    }

    /** {@code repeat(auto-fill, tracks...)}: as many repetitions as fit into the container. */
    public static GridTrack autoFill(GridTrack... tracks) {
        return repeat(RepeatMode.AUTO_FILL, 0, Arrays.asList(tracks));
    }

    /** {@code repeat(auto-fit, tracks...)}: like auto-fill, but repetitions without items collapse to zero. */
    public static GridTrack autoFit(GridTrack... tracks) {
        return repeat(RepeatMode.AUTO_FIT, 0, Arrays.asList(tracks));
    }

    static GridTrack repeat(RepeatMode mode, int count, List<GridTrack> tracks) {
        if (tracks.isEmpty()) throw new IllegalArgumentException("repeat() needs at least one track");
        for (GridTrack t : tracks) {
            Objects.requireNonNull(t, "track");
            if (t.kind == Kind.REPEAT) throw new IllegalArgumentException("repeat() cannot be nested");
            if (mode != RepeatMode.COUNT && !t.hasDefiniteSize()) {
                throw new IllegalArgumentException("auto-fill/auto-fit tracks need a definite size: " + t);
            }
        }
        return new GridTrack(Kind.REPEAT, 0, null, null, count, mode, Collections.unmodifiableList(List.copyOf(tracks)));
    }

    public Kind getKind() { return kind; }

    /** Pixels for FIXED, percentage for PERCENT, flex factor for FLEX; 0 otherwise. */
    public double getValue() { return value; }

    /** The minimum of a MINMAX track, null otherwise. */
    public GridTrack getMin() { return min; }

    /** The maximum of a MINMAX track, null otherwise. */
    public GridTrack getMax() { return max; }

    /** Repetition count of a {@code repeat(n, ...)} track; 0 for auto-fill/auto-fit and non-repeat tracks. */
    public int getRepeatCount() { return repeatCount; }

    /** Repeat mode of a REPEAT track, null otherwise. */
    public RepeatMode getRepeatMode() { return repeatMode; }

    /** Tracks of a REPEAT group; empty otherwise. */
    public List<GridTrack> getRepeatedTracks() { return repeated; }

    public boolean isFlexible() {
        return kind == Kind.FLEX || (kind == Kind.MINMAX && max.kind == Kind.FLEX);
    }

    /** True if either sizing function is fixed or a percentage (required inside auto-fill/auto-fit). */
    boolean hasDefiniteSize() {
        if (kind == Kind.FIXED || kind == Kind.PERCENT) return true;
        if (kind == Kind.MINMAX) return min.hasDefiniteSize() || max.hasDefiniteSize();
        return false;
    }

    /** Minimum sizing function (for MINMAX its min, for FLEX {@code auto}, otherwise the track itself). */
    GridTrack minSizing() {
        if (kind == Kind.MINMAX) return min;
        if (kind == Kind.FLEX) return AUTO;
        return this;
    }

    /** Maximum sizing function (for MINMAX its max, otherwise the track itself). */
    GridTrack maxSizing() {
        return kind == Kind.MINMAX ? max : this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GridTrack)) return false;
        GridTrack other = (GridTrack) o;
        return kind == other.kind
                && Double.compare(value, other.value) == 0
                && Objects.equals(min, other.min)
                && Objects.equals(max, other.max)
                && repeatCount == other.repeatCount
                && repeatMode == other.repeatMode
                && repeated.equals(other.repeated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, value, min, max, repeatCount, repeatMode, repeated);
    }

    /** CSS representation, e.g. {@code 1fr}, {@code minmax(100, auto)}, {@code repeat(auto-fill, 200)}. */
    @Override
    public String toString() {
        switch (kind) {
            case FIXED:       return formatNumber(value);
            case PERCENT:     return formatNumber(value) + "%";
            case FLEX:        return formatNumber(value) + "fr";
            case AUTO:        return "auto";
            case MIN_CONTENT: return "min-content";
            case MAX_CONTENT: return "max-content";
            case MINMAX:      return "minmax(" + min + ", " + max + ")";
            case REPEAT: {
                String count = repeatMode == RepeatMode.COUNT ? String.valueOf(repeatCount)
                        : repeatMode == RepeatMode.AUTO_FILL ? "auto-fill" : "auto-fit";
                return "repeat(" + count + ", " + GridTrackList.join(repeated) + ")";
            }
            default: throw new IllegalStateException();
        }
    }

    static String formatNumber(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) return String.valueOf((long) d);
        return String.valueOf(d);
    }
}
