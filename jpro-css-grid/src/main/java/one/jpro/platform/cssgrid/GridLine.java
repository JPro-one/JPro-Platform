package one.jpro.platform.cssgrid;

import java.util.Objects;

/**
 * A grid line reference used for item placement (CSS {@code grid-column-start}, {@code grid-row-end}, ...).
 * <p>
 * A line is one of:
 * <ul>
 *   <li>{@link #AUTO} – auto placement</li>
 *   <li>{@link #at(int)} – a 1-based line number; negative numbers count from the end of the explicit grid
 *       ({@code -1} is the last explicit line)</li>
 *   <li>{@link #span(int)} – a span of that many tracks from the opposite line</li>
 *   <li>{@link #named(String)} – a named area line ({@code header} or {@code header-start}), see
 *       {@link GridTemplateAreas}</li>
 * </ul>
 */
public final class GridLine {

    public static final GridLine AUTO = new GridLine(0, 0, null);

    private final int line;
    private final int span;
    private final String name;

    private GridLine(int line, int span, String name) {
        this.line = line;
        this.span = span;
        this.name = name;
    }

    /** A 1-based line number, negative numbers count from the end of the explicit grid. Zero is invalid. */
    public static GridLine at(int line) {
        if (line == 0) throw new IllegalArgumentException("Grid line 0 does not exist");
        return new GridLine(line, 0, null);
    }

    public static GridLine span(int tracks) {
        if (tracks < 1) throw new IllegalArgumentException("span must be >= 1: " + tracks);
        return new GridLine(0, tracks, null);
    }

    public static GridLine named(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isEmpty()) throw new IllegalArgumentException("Line name must not be empty");
        return new GridLine(0, 0, name);
    }

    /**
     * Parses {@code auto}, an integer, {@code span <n>} or a line/area name.
     */
    public static GridLine parse(String css) {
        String s = css.trim();
        if (s.isEmpty() || s.equalsIgnoreCase("auto")) return AUTO;
        String lower = s.toLowerCase();
        if (lower.equals("span") || lower.startsWith("span ") || lower.startsWith("span\t")) {
            String rest = s.substring(4).trim();
            if (rest.isEmpty()) throw new IllegalArgumentException("Invalid grid line '" + css + "': span needs a count");
            try {
                return span(Integer.parseInt(rest));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid grid line '" + css + "': span count must be an integer");
            }
        }
        if (s.matches("[+-]?\\d+")) {
            return at(Integer.parseInt(s.startsWith("+") ? s.substring(1) : s));
        }
        if (s.matches("[A-Za-z_][A-Za-z0-9_-]*")) {
            return named(s);
        }
        throw new IllegalArgumentException("Invalid grid line '" + css + "'");
    }

    public boolean isAuto() { return line == 0 && span == 0 && name == null; }

    public boolean isLine() { return line != 0; }

    public boolean isSpan() { return span != 0; }

    public boolean isNamed() { return name != null; }

    /** The line number (nonzero only if {@link #isLine()}). */
    public int getLine() { return line; }

    /** The span count (nonzero only if {@link #isSpan()}). */
    public int getSpan() { return span; }

    /** The line name (non-null only if {@link #isNamed()}). */
    public String getName() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GridLine)) return false;
        GridLine other = (GridLine) o;
        return line == other.line && span == other.span && Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() { return Objects.hash(line, span, name); }

    @Override
    public String toString() {
        if (isLine()) return String.valueOf(line);
        if (isSpan()) return "span " + span;
        if (isNamed()) return name;
        return "auto";
    }
}
