package one.jpro.platform.cssgrid;

import javafx.geometry.Insets;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CSS Grid item placement algorithm (CSS Grid Layout Level 1, §8.5).
 * Resolves every item to 0-based track indices in the implicit grid.
 */
final class GridPlacement {

    static final class Item {
        final Node node;
        final Insets margin;
        int columnStart, columnSpan, rowStart, rowSpan;

        Item(Node node, Insets margin) {
            this.node = node;
            this.margin = margin;
        }

        int columnEnd() { return columnStart + columnSpan; }
        int rowEnd() { return rowStart + rowSpan; }
    }

    final List<Item> items;
    final int columnCount, rowCount;
    /** Index of the first explicit track; implicit tracks may exist before it (negative line numbers). */
    final int explicitColumnOffset, explicitRowOffset;

    private GridPlacement(List<Item> items, int columnCount, int rowCount, int explicitColumnOffset, int explicitRowOffset) {
        this.items = items;
        this.columnCount = columnCount;
        this.rowCount = rowCount;
        this.explicitColumnOffset = explicitColumnOffset;
        this.explicitRowOffset = explicitRowOffset;
    }

    // ── Per-axis placement spec ───────────────────────────────────────

    /** One axis of an item's placement in 1-based line numbers; start == AUTO when auto-placed. */
    private static final class AxisSpec {
        static final int AUTO = Integer.MIN_VALUE;
        int start;
        int span;

        boolean definite() { return start != AUTO; }
        int end() { return start + span; }
    }

    private static AxisSpec resolveAxis(GridLine startLine, GridLine endLine, int explicitTracks,
                                        GridTemplateAreas areas, boolean columns) {
        int s = lineNumber(startLine, explicitTracks, areas, columns, true);
        int e = lineNumber(endLine, explicitTracks, areas, columns, false);
        int sSpan = startLine.isSpan() ? startLine.getSpan() : 0;
        int eSpan = endLine.isSpan() ? endLine.getSpan() : 0;

        AxisSpec spec = new AxisSpec();
        if (s != NO_LINE && e != NO_LINE) {
            if (e < s) { int t = s; s = e; e = t; }
            if (e == s) e = s + 1;
            spec.start = s;
            spec.span = e - s;
        } else if (s != NO_LINE) {
            spec.start = s;
            spec.span = eSpan > 0 ? eSpan : 1;
        } else if (e != NO_LINE) {
            spec.span = sSpan > 0 ? sSpan : 1;
            spec.start = e - spec.span;
        } else {
            spec.start = AxisSpec.AUTO;
            spec.span = sSpan > 0 ? sSpan : (eSpan > 0 ? eSpan : 1);
        }
        return spec;
    }

    private static final int NO_LINE = Integer.MIN_VALUE;

    /**
     * Returns the 1-based line number, or {@link #NO_LINE} for auto and span. An unknown name resolves to the first
     * implicit line after the explicit grid, as every implicit line is assumed to carry every name (§8.3).
     */
    private static int lineNumber(GridLine line, int explicitTracks, GridTemplateAreas areas, boolean columns, boolean start) {
        if (line.isLine()) {
            int n = line.getLine();
            return n > 0 ? n : explicitTracks + 2 + n;
        }
        if (line.isNamed()) {
            int resolved = areas.resolveLine(line.getName(), columns, start);
            return resolved != 0 ? resolved : explicitTracks + 1;
        }
        return NO_LINE;
    }

    // ── Algorithm ─────────────────────────────────────────────────────

    /**
     * @param nodes          managed children, already sorted by {@code order}
     * @param explicitColumns number of explicit column tracks
     * @param explicitRows    number of explicit row tracks
     */
    static GridPlacement place(List<Node> nodes, int explicitColumns, int explicitRows,
                               GridAutoFlow flow, GridTemplateAreas areas) {
        boolean rowFlow = flow.isRow();
        boolean dense = flow.isDense();

        List<Item> items = new ArrayList<>(nodes.size());
        List<AxisSpec> majorSpecs = new ArrayList<>(nodes.size());
        List<AxisSpec> minorSpecs = new ArrayList<>(nodes.size());
        for (Node node : nodes) {
            Item item = new Item(node, CssGrid.getMargin(node));
            AxisSpec col = resolveAxis(CssGrid.getColumnStart(node), CssGrid.getColumnEnd(node), explicitColumns, areas, true);
            AxisSpec row = resolveAxis(CssGrid.getRowStart(node), CssGrid.getRowEnd(node), explicitRows, areas, false);
            items.add(item);
            majorSpecs.add(rowFlow ? row : col);
            minorSpecs.add(rowFlow ? col : row);
        }
        int explicitMinor = rowFlow ? explicitColumns : explicitRows;

        Occupancy occupied = new Occupancy();
        int minMinor = 1;
        int maxMinorLine = explicitMinor + 1;   // exclusive end line of the implicit grid on the minor axis
        int minMajor = 1;
        int maxMinorSpan = 1;

        // Step 1: items with a definite position on both axes
        for (int i = 0; i < items.size(); i++) {
            AxisSpec ma = majorSpecs.get(i), mi = minorSpecs.get(i);
            if (mi.definite()) {
                minMinor = Math.min(minMinor, mi.start);
                maxMinorLine = Math.max(maxMinorLine, mi.end());
            } else {
                maxMinorSpan = Math.max(maxMinorSpan, mi.span);
            }
            if (ma.definite()) minMajor = Math.min(minMajor, ma.start);
            if (ma.definite() && mi.definite()) {
                occupied.add(ma.start, ma.span, mi.start, mi.span);
            }
        }
        maxMinorLine = Math.max(maxMinorLine, minMinor + maxMinorSpan);

        // Step 2: items locked to a definite major line (e.g. a row in row flow)
        Map<Integer, Integer> lastEndByMajorLine = new HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            AxisSpec ma = majorSpecs.get(i), mi = minorSpecs.get(i);
            if (!ma.definite() || mi.definite()) continue;
            int from = minMinor;
            if (!dense) {
                for (int line = ma.start; line < ma.end(); line++) {
                    from = Math.max(from, lastEndByMajorLine.getOrDefault(line, minMinor));
                }
            }
            int m = from;
            while (!occupied.fits(ma.start, ma.span, m, mi.span)) m++;
            mi.start = m;
            occupied.add(ma.start, ma.span, mi.start, mi.span);
            maxMinorLine = Math.max(maxMinorLine, mi.end());
            for (int line = ma.start; line < ma.end(); line++) {
                lastEndByMajorLine.put(line, mi.end());
            }
        }

        // Step 3: auto-placement cursor
        int cursorMajor = minMajor;
        int cursorMinor = minMinor;
        for (int i = 0; i < items.size(); i++) {
            AxisSpec ma = majorSpecs.get(i), mi = minorSpecs.get(i);
            if (ma.definite()) continue;
            if (mi.definite()) {
                if (dense) {
                    cursorMajor = minMajor;
                } else if (mi.start < cursorMinor) {
                    cursorMajor++;
                }
                cursorMinor = mi.start;
                while (!occupied.fits(cursorMajor, ma.span, cursorMinor, mi.span)) cursorMajor++;
            } else {
                if (dense) {
                    cursorMajor = minMajor;
                    cursorMinor = minMinor;
                }
                while (true) {
                    if (cursorMinor + mi.span > maxMinorLine) {
                        cursorMajor++;
                        cursorMinor = minMinor;
                    }
                    if (occupied.fits(cursorMajor, ma.span, cursorMinor, mi.span)) break;
                    cursorMinor++;
                }
            }
            ma.start = cursorMajor;
            mi.start = cursorMinor;
            occupied.add(ma.start, ma.span, mi.start, mi.span);
        }

        // Normalize to 0-based track indices
        int minColLine = 1, maxColLine = explicitColumns + 1;
        int minRowLine = 1, maxRowLine = explicitRows + 1;
        for (int i = 0; i < items.size(); i++) {
            AxisSpec col = rowFlow ? minorSpecs.get(i) : majorSpecs.get(i);
            AxisSpec row = rowFlow ? majorSpecs.get(i) : minorSpecs.get(i);
            minColLine = Math.min(minColLine, col.start);
            maxColLine = Math.max(maxColLine, col.end());
            minRowLine = Math.min(minRowLine, row.start);
            maxRowLine = Math.max(maxRowLine, row.end());
        }
        int colOffset = 1 - minColLine;
        int rowOffset = 1 - minRowLine;
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            AxisSpec col = rowFlow ? minorSpecs.get(i) : majorSpecs.get(i);
            AxisSpec row = rowFlow ? majorSpecs.get(i) : minorSpecs.get(i);
            item.columnStart = col.start - 1 + colOffset;
            item.columnSpan = col.span;
            item.rowStart = row.start - 1 + rowOffset;
            item.rowSpan = row.span;
        }
        return new GridPlacement(items, maxColLine - minColLine, maxRowLine - minRowLine, colOffset, rowOffset);
    }

    /** Set of occupied cells keyed by (major, minor) line numbers, which may be negative. */
    private static final class Occupancy {
        private final Set<Long> cells = new HashSet<>();

        private static long key(int major, int minor) {
            return (((long) major) << 32) ^ (minor & 0xffffffffL);
        }

        void add(int major, int majorSpan, int minor, int minorSpan) {
            for (int a = major; a < major + majorSpan; a++) {
                for (int b = minor; b < minor + minorSpan; b++) {
                    cells.add(key(a, b));
                }
            }
        }

        boolean fits(int major, int majorSpan, int minor, int minorSpan) {
            for (int a = major; a < major + majorSpan; a++) {
                for (int b = minor; b < minor + minorSpan; b++) {
                    if (cells.contains(key(a, b))) return false;
                }
            }
            return true;
        }
    }
}
