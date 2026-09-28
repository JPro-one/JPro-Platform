package one.jpro.platform.cssgrid;

/**
 * Controls how auto-placed items are inserted into the grid (CSS {@code grid-auto-flow}).
 */
public enum GridAutoFlow {
    ROW,
    COLUMN,
    ROW_DENSE,
    COLUMN_DENSE;

    /** Returns true if items are placed row by row (filling columns first). */
    public boolean isRow() {
        return this == ROW || this == ROW_DENSE;
    }

    /** Returns true if the dense packing algorithm is used (holes may be filled by later items). */
    public boolean isDense() {
        return this == ROW_DENSE || this == COLUMN_DENSE;
    }
}
