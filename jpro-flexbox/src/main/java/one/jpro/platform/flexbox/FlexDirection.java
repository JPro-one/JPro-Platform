package one.jpro.platform.flexbox;

/**
 * Specifies the main axis direction for laying out children in a FlexBox.
 */
public enum FlexDirection {
    ROW,
    ROW_REVERSE,
    COLUMN,
    COLUMN_REVERSE;

    /**
     * Returns true if this direction lays out along the horizontal axis.
     */
    public boolean isRow() {
        return this == ROW || this == ROW_REVERSE;
    }

    /**
     * Returns true if this direction is reversed.
     */
    public boolean isReverse() {
        return this == ROW_REVERSE || this == COLUMN_REVERSE;
    }
}
