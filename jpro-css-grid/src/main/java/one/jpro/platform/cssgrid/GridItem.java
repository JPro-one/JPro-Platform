package one.jpro.platform.cssgrid;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.StyleConverter;
import javafx.css.StyleOrigin;
import javafx.css.Styleable;
import javafx.css.StyleableIntegerProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A StackPane wrapper that exposes {@link CssGrid} child placement as CSS-styleable properties.
 * <p>
 * Usage in Java:
 * <pre>
 * GridItem item = new GridItem(myButton);
 * item.setColumn(1, 3);
 * grid.getChildren().add(item);
 * </pre>
 * Usage in CSS (values with a {@code /} or {@code span} must be quoted):
 * <pre>
 * .my-item {
 *     grid-column: "1 / 3";
 *     grid-row: "span 2";
 *     grid-area: header;
 *     grid-column-start: 2;
 *     grid-row-end: "span 2";
 *     justify-self: center;
 *     align-self: end;
 *     order: 1;
 * }
 * </pre>
 * The shorthands {@code grid-column}, {@code grid-row} and {@code grid-area} set the corresponding longhand
 * properties; a longhand declared in the same stylesheet always wins over a shorthand. Like any JavaFX CSS value,
 * author and inline styles override values set from Java, user-agent styles do not.
 */
public class GridItem extends StackPane {

    // ── CSS metadata ──────────────────────────────────────────────────

    private static final CssMetaData<GridItem, String> GRID_AREA_META = shorthandMeta("grid-area");
    private static final CssMetaData<GridItem, String> GRID_COLUMN_META = shorthandMeta("grid-column");
    private static final CssMetaData<GridItem, String> GRID_ROW_META = shorthandMeta("grid-row");

    private static final CssMetaData<GridItem, GridLine> COLUMN_START_META = lineMeta("grid-column-start");
    private static final CssMetaData<GridItem, GridLine> COLUMN_END_META = lineMeta("grid-column-end");
    private static final CssMetaData<GridItem, GridLine> ROW_START_META = lineMeta("grid-row-start");
    private static final CssMetaData<GridItem, GridLine> ROW_END_META = lineMeta("grid-row-end");

    private static final CssMetaData<GridItem, GridItemAlignment> JUSTIFY_SELF_META =
            new CssMetaData<>("justify-self", CssGrid.ITEM_ALIGNMENT_CONVERTER) {
                @Override public boolean isSettable(GridItem node) { return !node.justifySelf.isBound(); }
                @Override public StyleableProperty<GridItemAlignment> getStyleableProperty(GridItem node) { return node.justifySelf; }
            };

    private static final CssMetaData<GridItem, GridItemAlignment> ALIGN_SELF_META =
            new CssMetaData<>("align-self", CssGrid.ITEM_ALIGNMENT_CONVERTER) {
                @Override public boolean isSettable(GridItem node) { return !node.alignSelf.isBound(); }
                @Override public StyleableProperty<GridItemAlignment> getStyleableProperty(GridItem node) { return node.alignSelf; }
            };

    private static final CssMetaData<GridItem, Number> ORDER_META =
            new CssMetaData<>("order", StyleConverter.getSizeConverter(), 0) {
                @Override public boolean isSettable(GridItem node) { return !node.order.isBound(); }
                @Override public StyleableProperty<Number> getStyleableProperty(GridItem node) {
                    return (StyleableProperty<Number>) (StyleableProperty<?>) node.order;
                }
            };

    private static CssMetaData<GridItem, String> shorthandMeta(String property) {
        return new CssMetaData<>(property, new GridStyleSupport.Converter<>(GridStyleSupport::toText)) {
            @Override public boolean isSettable(GridItem node) { return true; }
            @Override public StyleableProperty<String> getStyleableProperty(GridItem node) { return node.shorthand(property); }
        };
    }

    private static CssMetaData<GridItem, GridLine> lineMeta(String property) {
        return new CssMetaData<>(property, new GridStyleSupport.Converter<>(GridStyleSupport::toLine), GridLine.AUTO) {
            @Override public boolean isSettable(GridItem node) { return !node.line(property).isBound(); }
            @Override public StyleableProperty<GridLine> getStyleableProperty(GridItem node) { return node.line(property); }
        };
    }

    private static final List<CssMetaData<? extends Styleable, ?>> CLASS_CSS_META_DATA;
    static {
        List<CssMetaData<? extends Styleable, ?>> list = new ArrayList<>(StackPane.getClassCssMetaData());
        // Shorthands first so that longhands declared alongside them win
        list.add(GRID_AREA_META);
        list.add(GRID_COLUMN_META);
        list.add(GRID_ROW_META);
        list.add(COLUMN_START_META);
        list.add(COLUMN_END_META);
        list.add(ROW_START_META);
        list.add(ROW_END_META);
        list.add(JUSTIFY_SELF_META);
        list.add(ALIGN_SELF_META);
        list.add(ORDER_META);
        CLASS_CSS_META_DATA = Collections.unmodifiableList(list);
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CLASS_CSS_META_DATA;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    // ── Placement properties ──────────────────────────────────────────

    private final StyleableObjectProperty<GridLine> columnStart = GridStyleSupport.CoercingProperty.create(
            this, "columnStart", COLUMN_START_META, GridLine.AUTO, GridStyleSupport::toLine,
            () -> CssGrid.setColumnStart(this, getColumnStart()));

    public final GridLine getColumnStart() { return orAuto(columnStart.get()); }
    public final void setColumnStart(GridLine value) { columnStart.set(value); }
    public final ObjectProperty<GridLine> columnStartProperty() { return columnStart; }

    private final StyleableObjectProperty<GridLine> columnEnd = GridStyleSupport.CoercingProperty.create(
            this, "columnEnd", COLUMN_END_META, GridLine.AUTO, GridStyleSupport::toLine,
            () -> CssGrid.setColumnEnd(this, getColumnEnd()));

    public final GridLine getColumnEnd() { return orAuto(columnEnd.get()); }
    public final void setColumnEnd(GridLine value) { columnEnd.set(value); }
    public final ObjectProperty<GridLine> columnEndProperty() { return columnEnd; }

    private final StyleableObjectProperty<GridLine> rowStart = GridStyleSupport.CoercingProperty.create(
            this, "rowStart", ROW_START_META, GridLine.AUTO, GridStyleSupport::toLine,
            () -> CssGrid.setRowStart(this, getRowStart()));

    public final GridLine getRowStart() { return orAuto(rowStart.get()); }
    public final void setRowStart(GridLine value) { rowStart.set(value); }
    public final ObjectProperty<GridLine> rowStartProperty() { return rowStart; }

    private final StyleableObjectProperty<GridLine> rowEnd = GridStyleSupport.CoercingProperty.create(
            this, "rowEnd", ROW_END_META, GridLine.AUTO, GridStyleSupport::toLine,
            () -> CssGrid.setRowEnd(this, getRowEnd()));

    public final GridLine getRowEnd() { return orAuto(rowEnd.get()); }
    public final void setRowEnd(GridLine value) { rowEnd.set(value); }
    public final ObjectProperty<GridLine> rowEndProperty() { return rowEnd; }

    private static GridLine orAuto(GridLine line) {
        return line != null ? line : GridLine.AUTO;
    }

    private StyleableObjectProperty<GridLine> line(String property) {
        switch (property) {
            case "grid-column-start": return columnStart;
            case "grid-column-end":   return columnEnd;
            case "grid-row-start":    return rowStart;
            default:                  return rowEnd;
        }
    }

    // ── Shorthands (CSS only) ─────────────────────────────────────────

    private final StyleableObjectProperty<String> gridArea = GridStyleSupport.CoercingProperty.createCssApplied(
            this, "gridArea", GRID_AREA_META, GridStyleSupport::toText, this::applyGridArea);

    private final StyleableObjectProperty<String> gridColumn = GridStyleSupport.CoercingProperty.createCssApplied(
            this, "gridColumn", GRID_COLUMN_META, GridStyleSupport::toText, this::applyGridColumn);

    private final StyleableObjectProperty<String> gridRow = GridStyleSupport.CoercingProperty.createCssApplied(
            this, "gridRow", GRID_ROW_META, GridStyleSupport::toText, this::applyGridRow);

    private StyleableObjectProperty<String> shorthand(String property) {
        switch (property) {
            case "grid-area":   return gridArea;
            case "grid-column": return gridColumn;
            default:            return gridRow;
        }
    }

    // Values each shorthand last propagated, so that a reset shorthand only reverts what it set itself
    private GridLine[] gridColumnApplied, gridRowApplied, gridAreaApplied;

    private void applyGridColumn() {
        gridColumnApplied = applyShorthand(gridColumn, 2, gridColumnApplied, columnStart, columnEnd);
    }

    private void applyGridRow() {
        gridRowApplied = applyShorthand(gridRow, 2, gridRowApplied, rowStart, rowEnd);
    }

    private void applyGridArea() {
        gridAreaApplied = applyShorthand(gridArea, 4, gridAreaApplied, rowStart, columnStart, rowEnd, columnEnd);
    }

    @SafeVarargs
    private static GridLine[] applyShorthand(StyleableObjectProperty<String> shorthand, int count, GridLine[] previous,
                                             StyleableObjectProperty<GridLine>... targets) {
        String text = shorthand.get();
        StyleOrigin origin = shorthand.getStyleOrigin();
        if (text == null) {
            if (previous != null) {
                for (int i = 0; i < count; i++) {
                    if (previous[i].equals(targets[i].get())) propagate(targets[i], GridLine.AUTO, null);
                }
            }
            return null;
        }
        GridLine[] lines = parseShorthand(text, count);
        for (int i = 0; i < count; i++) propagate(targets[i], lines[i], origin);
        return lines;
    }

    /**
     * Splits {@code a / b [/ c / d]} into the longhand lines. Omitted end lines repeat a named start line
     * and are auto otherwise, as in CSS.
     */
    static GridLine[] parseShorthand(String text, int count) {
        GridLine[] result = new GridLine[count];
        String[] parts = text == null || text.trim().isEmpty() ? new String[0] : text.split("/");
        if (parts.length > count) throw new IllegalArgumentException("Too many values in '" + text + "'");
        for (int i = 0; i < parts.length; i++) result[i] = GridLine.parse(parts[i]);
        for (int i = parts.length; i < count; i++) {
            GridLine opposite = i == 0 ? GridLine.AUTO : i == 1 ? result[0] : result[i - 2];
            result[i] = opposite.isNamed() ? opposite : GridLine.AUTO;
        }
        return result;
    }

    /** Same rule as the CSS engine: a value set from Java only yields to author and inline styles. */
    private static void propagate(StyleableObjectProperty<GridLine> target, GridLine value, StyleOrigin origin) {
        if (target.getStyleOrigin() == StyleOrigin.USER && origin == StyleOrigin.USER_AGENT) return;
        target.applyStyle(origin, value);
    }

    // ── Alignment and order ───────────────────────────────────────────

    private final StyleableObjectProperty<GridItemAlignment> justifySelf =
            new SimpleStyleableObjectProperty<>(JUSTIFY_SELF_META, this, "justifySelf") {
                @Override protected void invalidated() { CssGrid.setJustifySelf(GridItem.this, get()); }
            };

    /** Overrides the grid's justify-items for this item; null inherits. */
    public final GridItemAlignment getJustifySelf() { return justifySelf.get(); }
    public final void setJustifySelf(GridItemAlignment value) { justifySelf.set(value); }
    public final ObjectProperty<GridItemAlignment> justifySelfProperty() { return justifySelf; }

    private final StyleableObjectProperty<GridItemAlignment> alignSelf =
            new SimpleStyleableObjectProperty<>(ALIGN_SELF_META, this, "alignSelf") {
                @Override protected void invalidated() { CssGrid.setAlignSelf(GridItem.this, get()); }
            };

    /** Overrides the grid's align-items for this item; null inherits. */
    public final GridItemAlignment getAlignSelf() { return alignSelf.get(); }
    public final void setAlignSelf(GridItemAlignment value) { alignSelf.set(value); }
    public final ObjectProperty<GridItemAlignment> alignSelfProperty() { return alignSelf; }

    private final StyleableIntegerProperty order = new StyleableIntegerProperty(0) {
        @Override protected void invalidated() { CssGrid.setOrder(GridItem.this, get()); }
        @Override public Object getBean() { return GridItem.this; }
        @Override public String getName() { return "order"; }
        @Override public CssMetaData<GridItem, Number> getCssMetaData() { return ORDER_META; }
    };

    public final int getOrder() { return order.get(); }
    public final void setOrder(int value) { order.set(value); }
    public final IntegerProperty orderProperty() { return order; }

    // ── Convenience setters ───────────────────────────────────────────

    /** {@code grid-column: <start> / <end>} with 1-based lines. */
    public final void setColumn(int start, int end) {
        setColumnStart(GridLine.at(start));
        setColumnEnd(GridLine.at(end));
    }

    /** {@code grid-column: <start>}. */
    public final void setColumn(int start) {
        setColumnStart(GridLine.at(start));
        setColumnEnd(GridLine.AUTO);
    }

    /** {@code grid-column: span <n>}. */
    public final void setColumnSpan(int span) {
        setColumnStart(GridLine.span(span));
        setColumnEnd(GridLine.AUTO);
    }

    /** {@code grid-row: <start> / <end>} with 1-based lines. */
    public final void setRow(int start, int end) {
        setRowStart(GridLine.at(start));
        setRowEnd(GridLine.at(end));
    }

    /** {@code grid-row: <start>}. */
    public final void setRow(int start) {
        setRowStart(GridLine.at(start));
        setRowEnd(GridLine.AUTO);
    }

    /** {@code grid-row: span <n>}. */
    public final void setRowSpan(int span) {
        setRowStart(GridLine.span(span));
        setRowEnd(GridLine.AUTO);
    }

    /** {@code grid-area: <name>}. */
    public final void setArea(String areaName) {
        GridLine line = GridLine.named(areaName);
        setRowStart(line);
        setColumnStart(line);
        setRowEnd(line);
        setColumnEnd(line);
    }

    /** {@code grid-area: <rowStart> / <columnStart> / <rowEnd> / <columnEnd>}. */
    public final void setArea(int rowStart, int columnStart, int rowEnd, int columnEnd) {
        setRowStart(GridLine.at(rowStart));
        setColumnStart(GridLine.at(columnStart));
        setRowEnd(GridLine.at(rowEnd));
        setColumnEnd(GridLine.at(columnEnd));
    }

    /** Resets to auto placement on both axes. */
    public final void clearPlacement() {
        setColumnStart(GridLine.AUTO);
        setColumnEnd(GridLine.AUTO);
        setRowStart(GridLine.AUTO);
        setRowEnd(GridLine.AUTO);
    }

    // ── Constructors ──────────────────────────────────────────────────

    public GridItem() {
    }

    public GridItem(Node... children) {
        super(children);
    }
}
