package one.jpro.platform.cssgrid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Named grid areas (CSS {@code grid-template-areas}). Each row is a string of area names separated by
 * whitespace; a {@code .} marks an empty cell. Areas must be rectangular.
 * <pre>
 * GridTemplateAreas.of("header header", "sidebar main", "footer footer")
 * </pre>
 * In CSS the rows are written as single-quoted groups inside one string:
 * <pre>
 * grid-template-areas: "'header header' 'sidebar main' 'footer footer'";
 * </pre>
 * Each area contributes the line names {@code <name>-start} and {@code <name>-end} on both axes, and items
 * can be placed into it with {@code grid-area: <name>} or {@link CssGrid#setArea(javafx.scene.Node, String)}.
 */
public final class GridTemplateAreas {

    /** 1-based, end-exclusive lines of a named area. */
    public record Area(String name, int rowStart, int rowEnd, int columnStart, int columnEnd) {
        public int rowSpan() { return rowEnd - rowStart; }
        public int columnSpan() { return columnEnd - columnStart; }
    }

    public static final GridTemplateAreas NONE = new GridTemplateAreas(Collections.emptyList(), 0, Collections.emptyMap());

    private static final Pattern QUOTED = Pattern.compile("'([^']*)'|\"([^\"]*)\"");

    private final List<String> rows;
    private final int columnCount;
    private final Map<String, Area> areas;

    private GridTemplateAreas(List<String> rows, int columnCount, Map<String, Area> areas) {
        this.rows = rows;
        this.columnCount = columnCount;
        this.areas = areas;
    }

    public static GridTemplateAreas of(String... rows) {
        return of(Arrays.asList(rows));
    }

    public static GridTemplateAreas of(List<String> rows) {
        Objects.requireNonNull(rows, "rows");
        if (rows.isEmpty()) return NONE;
        List<String[]> cells = new ArrayList<>();
        int columns = -1;
        for (String row : rows) {
            String[] names = row.trim().split("\\s+");
            if (names.length == 1 && names[0].isEmpty()) throw new IllegalArgumentException("Empty row in grid-template-areas");
            if (columns == -1) columns = names.length;
            else if (columns != names.length) {
                throw new IllegalArgumentException("All rows of grid-template-areas must have the same number of cells");
            }
            cells.add(names);
        }
        Map<String, Area> areas = new LinkedHashMap<>();
        for (int r = 0; r < cells.size(); r++) {
            for (int c = 0; c < columns; c++) {
                String name = cells.get(r)[c];
                if (name.matches("\\.+")) continue;
                if (!name.matches("[A-Za-z_][A-Za-z0-9_-]*")) {
                    throw new IllegalArgumentException("Invalid area name '" + name + "'");
                }
                Area area = areas.get(name);
                if (area == null) {
                    areas.put(name, new Area(name, r + 1, r + 2, c + 1, c + 2));
                } else {
                    areas.put(name, new Area(name, area.rowStart(), Math.max(area.rowEnd(), r + 2),
                            area.columnStart(), Math.max(area.columnEnd(), c + 2)));
                }
            }
        }
        for (Area area : areas.values()) {
            for (int r = area.rowStart() - 1; r < area.rowEnd() - 1; r++) {
                for (int c = area.columnStart() - 1; c < area.columnEnd() - 1; c++) {
                    if (!area.name().equals(cells.get(r)[c])) {
                        throw new IllegalArgumentException("Area '" + area.name() + "' is not rectangular");
                    }
                }
            }
        }
        List<String> normalized = new ArrayList<>();
        for (String[] names : cells) normalized.add(String.join(" ", names));
        return new GridTemplateAreas(Collections.unmodifiableList(normalized), columns, Collections.unmodifiableMap(areas));
    }

    /**
     * Parses rows given as quoted groups ({@code 'a b' 'c d'}), or one row per line when no quotes are present.
     */
    public static GridTemplateAreas parse(String css) {
        Objects.requireNonNull(css, "css");
        String s = css.trim();
        if (s.isEmpty() || s.equalsIgnoreCase("none")) return NONE;
        List<String> rows = new ArrayList<>();
        Matcher m = QUOTED.matcher(s);
        if (m.find()) {
            do {
                rows.add(m.group(1) != null ? m.group(1) : m.group(2));
            } while (m.find());
        } else {
            rows.addAll(Arrays.asList(s.split("\\R")));
        }
        return of(rows);
    }

    public boolean isEmpty() { return rows.isEmpty(); }

    public int getRowCount() { return rows.size(); }

    public int getColumnCount() { return columnCount; }

    /** The normalized rows, one string of cell names per row. */
    public List<String> getRows() { return rows; }

    /** Named areas in order of first appearance. */
    public Map<String, Area> getAreas() { return areas; }

    public Area getArea(String name) { return areas.get(name); }

    /**
     * Resolves a line name on one axis. Accepts an area name (start line for {@code start}, end line
     * otherwise) as well as explicit {@code <name>-start} / {@code <name>-end}. Returns 0 if unknown.
     */
    int resolveLine(String name, boolean columns, boolean start) {
        Area area = areas.get(name);
        if (area == null) {
            if (name.endsWith("-start")) {
                Area a = areas.get(name.substring(0, name.length() - 6));
                return a == null ? 0 : (columns ? a.columnStart() : a.rowStart());
            }
            if (name.endsWith("-end")) {
                Area a = areas.get(name.substring(0, name.length() - 4));
                return a == null ? 0 : (columns ? a.columnEnd() : a.rowEnd());
            }
            return 0;
        }
        if (columns) return start ? area.columnStart() : area.columnEnd();
        return start ? area.rowStart() : area.rowEnd();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof GridTemplateAreas && rows.equals(((GridTemplateAreas) o).rows));
    }

    @Override
    public int hashCode() { return rows.hashCode(); }

    /** CSS representation: {@code 'header header' 'sidebar main'}, or {@code none}. */
    @Override
    public String toString() {
        if (rows.isEmpty()) return "none";
        StringBuilder sb = new StringBuilder();
        for (String row : rows) {
            if (sb.length() > 0) sb.append(' ');
            sb.append('\'').append(row).append('\'');
        }
        return sb.toString();
    }
}
