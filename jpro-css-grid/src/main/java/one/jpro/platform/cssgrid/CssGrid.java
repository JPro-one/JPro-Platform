package one.jpro.platform.cssgrid;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A JavaFX layout pane that implements CSS Grid layout.
 * <p>
 * Container properties are styleable via CSS. Track lists and areas are written as quoted strings because
 * JavaFX's CSS parser does not understand {@code fr}, {@code minmax()} or {@code repeat()}:
 * <pre>
 * .my-grid {
 *     grid-template-columns: "200 1fr 2fr";
 *     grid-template-rows: "auto";
 *     grid-template-areas: "'header header header' 'sidebar main main'";
 *     grid-auto-rows: "minmax(100, auto)";
 *     grid-auto-flow: row;
 *     justify-items: stretch;
 *     align-items: stretch;
 *     justify-content: start;
 *     align-content: start;
 *     row-gap: 8;
 *     column-gap: 8;
 * }
 * </pre>
 * Per-child placement is set via static methods such as {@link #setColumn(Node, int, int)}, {@link #setRow(Node, int, int)},
 * {@link #setArea(Node, String)}, {@link #setJustifySelf}, {@link #setAlignSelf} and {@link #setOrder}, or via CSS on a
 * {@link GridItem} child.
 */
public class CssGrid extends Pane {

    private static final String COLUMN_START_CONSTRAINT = "cssgrid-column-start";
    private static final String COLUMN_END_CONSTRAINT = "cssgrid-column-end";
    private static final String ROW_START_CONSTRAINT = "cssgrid-row-start";
    private static final String ROW_END_CONSTRAINT = "cssgrid-row-end";
    private static final String JUSTIFY_SELF_CONSTRAINT = "cssgrid-justify-self";
    private static final String ALIGN_SELF_CONSTRAINT = "cssgrid-align-self";
    private static final String ORDER_CONSTRAINT = "cssgrid-order";
    private static final String MARGIN_CONSTRAINT = "cssgrid-margin";

    static final GridEnumConverter<GridItemAlignment> ITEM_ALIGNMENT_CONVERTER =
            new GridEnumConverter<>(GridItemAlignment.class)
                    .alias("normal", GridItemAlignment.STRETCH)
                    .alias("flex-start", GridItemAlignment.START)
                    .alias("flex-end", GridItemAlignment.END)
                    .alias("self-start", GridItemAlignment.START)
                    .alias("self-end", GridItemAlignment.END)
                    .alias("left", GridItemAlignment.START)
                    .alias("right", GridItemAlignment.END);

    static final GridEnumConverter<GridContentAlignment> CONTENT_ALIGNMENT_CONVERTER =
            new GridEnumConverter<>(GridContentAlignment.class)
                    .alias("normal", GridContentAlignment.STRETCH)
                    .alias("flex-start", GridContentAlignment.START)
                    .alias("flex-end", GridContentAlignment.END)
                    .alias("left", GridContentAlignment.START)
                    .alias("right", GridContentAlignment.END);

    static final GridEnumConverter<GridAutoFlow> AUTO_FLOW_CONVERTER =
            new GridEnumConverter<>(GridAutoFlow.class)
                    .alias("dense", GridAutoFlow.ROW_DENSE);

    // ── CSS metadata ──────────────────────────────────────────────────

    private static final CssMetaData<CssGrid, GridTrackList> TEMPLATE_COLUMNS_META =
            new CssMetaData<>("grid-template-columns", new GridStyleSupport.Converter<>(GridStyleSupport::toTrackList), GridTrackList.NONE) {
                @Override public boolean isSettable(CssGrid node) { return !node.templateColumns.isBound(); }
                @Override public StyleableProperty<GridTrackList> getStyleableProperty(CssGrid node) { return node.templateColumns; }
            };

    private static final CssMetaData<CssGrid, GridTrackList> TEMPLATE_ROWS_META =
            new CssMetaData<>("grid-template-rows", new GridStyleSupport.Converter<>(GridStyleSupport::toTrackList), GridTrackList.NONE) {
                @Override public boolean isSettable(CssGrid node) { return !node.templateRows.isBound(); }
                @Override public StyleableProperty<GridTrackList> getStyleableProperty(CssGrid node) { return node.templateRows; }
            };

    private static final CssMetaData<CssGrid, GridTemplateAreas> TEMPLATE_AREAS_META =
            new CssMetaData<>("grid-template-areas", new GridStyleSupport.Converter<>(GridStyleSupport::toAreas), GridTemplateAreas.NONE) {
                @Override public boolean isSettable(CssGrid node) { return !node.templateAreas.isBound(); }
                @Override public StyleableProperty<GridTemplateAreas> getStyleableProperty(CssGrid node) { return node.templateAreas; }
            };

    private static final CssMetaData<CssGrid, GridTrackList> AUTO_COLUMNS_META =
            new CssMetaData<>("grid-auto-columns", new GridStyleSupport.Converter<>(GridStyleSupport::toTrackList), GridTrackList.AUTO) {
                @Override public boolean isSettable(CssGrid node) { return !node.autoColumns.isBound(); }
                @Override public StyleableProperty<GridTrackList> getStyleableProperty(CssGrid node) { return node.autoColumns; }
            };

    private static final CssMetaData<CssGrid, GridTrackList> AUTO_ROWS_META =
            new CssMetaData<>("grid-auto-rows", new GridStyleSupport.Converter<>(GridStyleSupport::toTrackList), GridTrackList.AUTO) {
                @Override public boolean isSettable(CssGrid node) { return !node.autoRows.isBound(); }
                @Override public StyleableProperty<GridTrackList> getStyleableProperty(CssGrid node) { return node.autoRows; }
            };

    private static final CssMetaData<CssGrid, GridAutoFlow> AUTO_FLOW_META =
            new CssMetaData<>("grid-auto-flow", AUTO_FLOW_CONVERTER, GridAutoFlow.ROW) {
                @Override public boolean isSettable(CssGrid node) { return !node.autoFlow.isBound(); }
                @Override public StyleableProperty<GridAutoFlow> getStyleableProperty(CssGrid node) { return node.autoFlow; }
            };

    private static final CssMetaData<CssGrid, GridItemAlignment> JUSTIFY_ITEMS_META =
            new CssMetaData<>("justify-items", ITEM_ALIGNMENT_CONVERTER, GridItemAlignment.STRETCH) {
                @Override public boolean isSettable(CssGrid node) { return !node.justifyItems.isBound(); }
                @Override public StyleableProperty<GridItemAlignment> getStyleableProperty(CssGrid node) { return node.justifyItems; }
            };

    private static final CssMetaData<CssGrid, GridItemAlignment> ALIGN_ITEMS_META =
            new CssMetaData<>("align-items", ITEM_ALIGNMENT_CONVERTER, GridItemAlignment.STRETCH) {
                @Override public boolean isSettable(CssGrid node) { return !node.alignItems.isBound(); }
                @Override public StyleableProperty<GridItemAlignment> getStyleableProperty(CssGrid node) { return node.alignItems; }
            };

    private static final CssMetaData<CssGrid, GridContentAlignment> JUSTIFY_CONTENT_META =
            new CssMetaData<>("justify-content", CONTENT_ALIGNMENT_CONVERTER, GridContentAlignment.STRETCH) {
                @Override public boolean isSettable(CssGrid node) { return !node.justifyContent.isBound(); }
                @Override public StyleableProperty<GridContentAlignment> getStyleableProperty(CssGrid node) { return node.justifyContent; }
            };

    private static final CssMetaData<CssGrid, GridContentAlignment> ALIGN_CONTENT_META =
            new CssMetaData<>("align-content", CONTENT_ALIGNMENT_CONVERTER, GridContentAlignment.STRETCH) {
                @Override public boolean isSettable(CssGrid node) { return !node.alignContent.isBound(); }
                @Override public StyleableProperty<GridContentAlignment> getStyleableProperty(CssGrid node) { return node.alignContent; }
            };

    private static final CssMetaData<CssGrid, Number> ROW_GAP_META =
            new CssMetaData<>("row-gap", StyleConverter.getSizeConverter(), 0) {
                @Override public boolean isSettable(CssGrid node) { return !node.rowGap.isBound(); }
                @Override public StyleableProperty<Number> getStyleableProperty(CssGrid node) { return (StyleableProperty<Number>) (StyleableProperty<?>) node.rowGap; }
            };

    private static final CssMetaData<CssGrid, Number> COLUMN_GAP_META =
            new CssMetaData<>("column-gap", StyleConverter.getSizeConverter(), 0) {
                @Override public boolean isSettable(CssGrid node) { return !node.columnGap.isBound(); }
                @Override public StyleableProperty<Number> getStyleableProperty(CssGrid node) { return (StyleableProperty<Number>) (StyleableProperty<?>) node.columnGap; }
            };

    private static final List<CssMetaData<? extends Styleable, ?>> CLASS_CSS_META_DATA;
    static {
        List<CssMetaData<? extends Styleable, ?>> list = new ArrayList<>(Pane.getClassCssMetaData());
        list.add(TEMPLATE_COLUMNS_META);
        list.add(TEMPLATE_ROWS_META);
        list.add(TEMPLATE_AREAS_META);
        list.add(AUTO_COLUMNS_META);
        list.add(AUTO_ROWS_META);
        list.add(AUTO_FLOW_META);
        list.add(JUSTIFY_ITEMS_META);
        list.add(ALIGN_ITEMS_META);
        list.add(JUSTIFY_CONTENT_META);
        list.add(ALIGN_CONTENT_META);
        list.add(ROW_GAP_META);
        list.add(COLUMN_GAP_META);
        CLASS_CSS_META_DATA = Collections.unmodifiableList(list);
    }

    /** The CSS metadata of this class: the Pane properties plus the grid container properties. */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CLASS_CSS_META_DATA;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    // ── Container properties (styleable) ──────────────────────────────

    private final StyleableObjectProperty<GridTrackList> templateColumns = GridStyleSupport.CoercingProperty.create(
            this, "templateColumns", TEMPLATE_COLUMNS_META, GridTrackList.NONE, GridStyleSupport::toTrackList, this::requestLayout);

    public final GridTrackList getTemplateColumns() { return nonNull(templateColumns.get(), GridTrackList.NONE); }
    public final void setTemplateColumns(GridTrackList value) { templateColumns.set(value); }
    /** Sets the column tracks from CSS syntax, e.g. {@code "200 1fr repeat(2, 100)"}. */
    public final void setTemplateColumns(String css) { setTemplateColumns(GridTrackList.parse(css)); }
    public final void setTemplateColumns(GridTrack... tracks) { setTemplateColumns(GridTrackList.of(tracks)); }
    public final ObjectProperty<GridTrackList> templateColumnsProperty() { return templateColumns; }

    private final StyleableObjectProperty<GridTrackList> templateRows = GridStyleSupport.CoercingProperty.create(
            this, "templateRows", TEMPLATE_ROWS_META, GridTrackList.NONE, GridStyleSupport::toTrackList, this::requestLayout);

    public final GridTrackList getTemplateRows() { return nonNull(templateRows.get(), GridTrackList.NONE); }
    public final void setTemplateRows(GridTrackList value) { templateRows.set(value); }
    /** Sets the row tracks from CSS syntax, e.g. {@code "auto 1fr auto"}. */
    public final void setTemplateRows(String css) { setTemplateRows(GridTrackList.parse(css)); }
    public final void setTemplateRows(GridTrack... tracks) { setTemplateRows(GridTrackList.of(tracks)); }
    public final ObjectProperty<GridTrackList> templateRowsProperty() { return templateRows; }

    private final StyleableObjectProperty<GridTemplateAreas> templateAreas = GridStyleSupport.CoercingProperty.create(
            this, "templateAreas", TEMPLATE_AREAS_META, GridTemplateAreas.NONE, GridStyleSupport::toAreas, this::requestLayout);

    public final GridTemplateAreas getTemplateAreas() { return nonNull(templateAreas.get(), GridTemplateAreas.NONE); }
    public final void setTemplateAreas(GridTemplateAreas value) { templateAreas.set(value); }
    /**
     * Sets the named areas, one string per row, e.g. {@code setTemplateAreas("header header", "sidebar main")}.
     * A single argument in CSS form ({@code "'header header' 'sidebar main'"}) is accepted as well.
     */
    public final void setTemplateAreas(String... rows) {
        boolean cssForm = rows.length == 1 && (rows[0].indexOf('\'') >= 0 || rows[0].indexOf('"') >= 0);
        setTemplateAreas(cssForm ? GridTemplateAreas.parse(rows[0]) : GridTemplateAreas.of(rows));
    }
    public final ObjectProperty<GridTemplateAreas> templateAreasProperty() { return templateAreas; }

    private final StyleableObjectProperty<GridTrackList> autoColumns = GridStyleSupport.CoercingProperty.create(
            this, "autoColumns", AUTO_COLUMNS_META, GridTrackList.AUTO, GridStyleSupport::toTrackList, this::requestLayout);

    public final GridTrackList getAutoColumns() { return nonNull(autoColumns.get(), GridTrackList.AUTO); }
    public final void setAutoColumns(GridTrackList value) { autoColumns.set(value); }
    public final void setAutoColumns(String css) { setAutoColumns(GridTrackList.parse(css)); }
    public final void setAutoColumns(GridTrack... tracks) { setAutoColumns(GridTrackList.of(tracks)); }
    public final ObjectProperty<GridTrackList> autoColumnsProperty() { return autoColumns; }

    private final StyleableObjectProperty<GridTrackList> autoRows = GridStyleSupport.CoercingProperty.create(
            this, "autoRows", AUTO_ROWS_META, GridTrackList.AUTO, GridStyleSupport::toTrackList, this::requestLayout);

    public final GridTrackList getAutoRows() { return nonNull(autoRows.get(), GridTrackList.AUTO); }
    public final void setAutoRows(GridTrackList value) { autoRows.set(value); }
    public final void setAutoRows(String css) { setAutoRows(GridTrackList.parse(css)); }
    public final void setAutoRows(GridTrack... tracks) { setAutoRows(GridTrackList.of(tracks)); }
    public final ObjectProperty<GridTrackList> autoRowsProperty() { return autoRows; }

    private final StyleableObjectProperty<GridAutoFlow> autoFlow =
            new SimpleStyleableObjectProperty<>(AUTO_FLOW_META, this, "autoFlow", GridAutoFlow.ROW) {
                @Override protected void invalidated() { requestLayout(); }
            };

    public final GridAutoFlow getAutoFlow() { return nonNull(autoFlow.get(), GridAutoFlow.ROW); }
    public final void setAutoFlow(GridAutoFlow value) { autoFlow.set(value); }
    public final ObjectProperty<GridAutoFlow> autoFlowProperty() { return autoFlow; }

    private final StyleableObjectProperty<GridItemAlignment> justifyItems =
            new SimpleStyleableObjectProperty<>(JUSTIFY_ITEMS_META, this, "justifyItems", GridItemAlignment.STRETCH) {
                @Override protected void invalidated() { requestLayout(); }
            };

    public final GridItemAlignment getJustifyItems() { return nonNull(justifyItems.get(), GridItemAlignment.STRETCH); }
    public final void setJustifyItems(GridItemAlignment value) { justifyItems.set(value); }
    public final ObjectProperty<GridItemAlignment> justifyItemsProperty() { return justifyItems; }

    private final StyleableObjectProperty<GridItemAlignment> alignItems =
            new SimpleStyleableObjectProperty<>(ALIGN_ITEMS_META, this, "alignItems", GridItemAlignment.STRETCH) {
                @Override protected void invalidated() { requestLayout(); }
            };

    public final GridItemAlignment getAlignItems() { return nonNull(alignItems.get(), GridItemAlignment.STRETCH); }
    public final void setAlignItems(GridItemAlignment value) { alignItems.set(value); }
    public final ObjectProperty<GridItemAlignment> alignItemsProperty() { return alignItems; }

    private final StyleableObjectProperty<GridContentAlignment> justifyContent =
            new SimpleStyleableObjectProperty<>(JUSTIFY_CONTENT_META, this, "justifyContent", GridContentAlignment.STRETCH) {
                @Override protected void invalidated() { requestLayout(); }
            };

    public final GridContentAlignment getJustifyContent() { return nonNull(justifyContent.get(), GridContentAlignment.STRETCH); }
    public final void setJustifyContent(GridContentAlignment value) { justifyContent.set(value); }
    public final ObjectProperty<GridContentAlignment> justifyContentProperty() { return justifyContent; }

    private final StyleableObjectProperty<GridContentAlignment> alignContent =
            new SimpleStyleableObjectProperty<>(ALIGN_CONTENT_META, this, "alignContent", GridContentAlignment.STRETCH) {
                @Override protected void invalidated() { requestLayout(); }
            };

    public final GridContentAlignment getAlignContent() { return nonNull(alignContent.get(), GridContentAlignment.STRETCH); }
    public final void setAlignContent(GridContentAlignment value) { alignContent.set(value); }
    public final ObjectProperty<GridContentAlignment> alignContentProperty() { return alignContent; }

    private final StyleableDoubleProperty rowGap = new StyleableDoubleProperty(0) {
        @Override protected void invalidated() { requestLayout(); }
        @Override public Object getBean() { return CssGrid.this; }
        @Override public String getName() { return "rowGap"; }
        @Override public CssMetaData<CssGrid, Number> getCssMetaData() { return ROW_GAP_META; }
    };

    public final double getRowGap() { return rowGap.get(); }
    public final void setRowGap(double value) { rowGap.set(value); }
    public final DoubleProperty rowGapProperty() { return rowGap; }

    private final StyleableDoubleProperty columnGap = new StyleableDoubleProperty(0) {
        @Override protected void invalidated() { requestLayout(); }
        @Override public Object getBean() { return CssGrid.this; }
        @Override public String getName() { return "columnGap"; }
        @Override public CssMetaData<CssGrid, Number> getCssMetaData() { return COLUMN_GAP_META; }
    };

    public final double getColumnGap() { return columnGap.get(); }
    public final void setColumnGap(double value) { columnGap.set(value); }
    public final DoubleProperty columnGapProperty() { return columnGap; }

    /** Sets both row-gap and column-gap. */
    public final void setGap(double value) {
        setRowGap(value);
        setColumnGap(value);
    }

    /** Returns the row-gap (the value last set with {@link #setGap} when both gaps are equal). */
    public final double getGap() { return getRowGap(); }

    private static <T> T nonNull(T value, T fallback) {
        return value != null ? value : fallback;
    }

    // ── Constructors ──────────────────────────────────────────────────

    public CssGrid() {
    }

    public CssGrid(Node... children) {
        super(children);
    }

    // ── Static child constraint methods ───────────────────────────────

    /** {@code grid-column-start}: the child's start column line; null resets to auto. */
    public static void setColumnStart(Node child, GridLine line) { setConstraint(child, COLUMN_START_CONSTRAINT, line); }
    /** The child's start column line, {@link GridLine#AUTO} if unset. */
    public static GridLine getColumnStart(Node child) { return getLine(child, COLUMN_START_CONSTRAINT); }

    /** {@code grid-column-end}: the child's end column line; null resets to auto. */
    public static void setColumnEnd(Node child, GridLine line) { setConstraint(child, COLUMN_END_CONSTRAINT, line); }
    /** The child's end column line, {@link GridLine#AUTO} if unset. */
    public static GridLine getColumnEnd(Node child) { return getLine(child, COLUMN_END_CONSTRAINT); }

    /** {@code grid-row-start}: the child's start row line; null resets to auto. */
    public static void setRowStart(Node child, GridLine line) { setConstraint(child, ROW_START_CONSTRAINT, line); }
    /** The child's start row line, {@link GridLine#AUTO} if unset. */
    public static GridLine getRowStart(Node child) { return getLine(child, ROW_START_CONSTRAINT); }

    /** {@code grid-row-end}: the child's end row line; null resets to auto. */
    public static void setRowEnd(Node child, GridLine line) { setConstraint(child, ROW_END_CONSTRAINT, line); }
    /** The child's end row line, {@link GridLine#AUTO} if unset. */
    public static GridLine getRowEnd(Node child) { return getLine(child, ROW_END_CONSTRAINT); }

    /** {@code grid-column: <start>}: places the child at the given 1-based column line, spanning one track. */
    public static void setColumn(Node child, int start) {
        setColumnStart(child, GridLine.at(start));
        setColumnEnd(child, GridLine.AUTO);
    }

    /** {@code grid-column: <start> / <end>}: places the child between the given 1-based column lines. */
    public static void setColumn(Node child, int start, int end) {
        setColumnStart(child, GridLine.at(start));
        setColumnEnd(child, GridLine.at(end));
    }

    /** {@code grid-column: span <n>}: auto-places the child spanning {@code span} columns. */
    public static void setColumnSpan(Node child, int span) {
        setColumnStart(child, GridLine.span(span));
        setColumnEnd(child, GridLine.AUTO);
    }

    /** {@code grid-row: <start>}: places the child at the given 1-based row line, spanning one track. */
    public static void setRow(Node child, int start) {
        setRowStart(child, GridLine.at(start));
        setRowEnd(child, GridLine.AUTO);
    }

    /** {@code grid-row: <start> / <end>}: places the child between the given 1-based row lines. */
    public static void setRow(Node child, int start, int end) {
        setRowStart(child, GridLine.at(start));
        setRowEnd(child, GridLine.at(end));
    }

    /** {@code grid-row: span <n>}: auto-places the child spanning {@code span} rows. */
    public static void setRowSpan(Node child, int span) {
        setRowStart(child, GridLine.span(span));
        setRowEnd(child, GridLine.AUTO);
    }

    /** {@code grid-area: <name>}: places the child into the named area of {@link #getTemplateAreas()}. */
    public static void setArea(Node child, String areaName) {
        GridLine line = GridLine.named(areaName);
        setRowStart(child, line);
        setColumnStart(child, line);
        setRowEnd(child, line);
        setColumnEnd(child, line);
    }

    /** {@code grid-area: <rowStart> / <columnStart> / <rowEnd> / <columnEnd>}. */
    public static void setArea(Node child, int rowStart, int columnStart, int rowEnd, int columnEnd) {
        setRowStart(child, GridLine.at(rowStart));
        setColumnStart(child, GridLine.at(columnStart));
        setRowEnd(child, GridLine.at(rowEnd));
        setColumnEnd(child, GridLine.at(columnEnd));
    }

    /** Resets the child to auto placement on both axes. */
    public static void clearPlacement(Node child) {
        child.getProperties().remove(COLUMN_START_CONSTRAINT);
        child.getProperties().remove(COLUMN_END_CONSTRAINT);
        child.getProperties().remove(ROW_START_CONSTRAINT);
        child.getProperties().remove(ROW_END_CONSTRAINT);
        requestParentLayout(child);
    }

    public static void setJustifySelf(Node child, GridItemAlignment value) { setConstraint(child, JUSTIFY_SELF_CONSTRAINT, value); }

    /** The child's justify-self, or null to inherit {@link #getJustifyItems()}. */
    public static GridItemAlignment getJustifySelf(Node child) {
        Object val = child.getProperties().get(JUSTIFY_SELF_CONSTRAINT);
        return val instanceof GridItemAlignment ? (GridItemAlignment) val : null;
    }

    public static void setAlignSelf(Node child, GridItemAlignment value) { setConstraint(child, ALIGN_SELF_CONSTRAINT, value); }

    /** The child's align-self, or null to inherit {@link #getAlignItems()}. */
    public static GridItemAlignment getAlignSelf(Node child) {
        Object val = child.getProperties().get(ALIGN_SELF_CONSTRAINT);
        return val instanceof GridItemAlignment ? (GridItemAlignment) val : null;
    }

    /** {@code order}: children are placed in ascending order (stable), default 0. */
    public static void setOrder(Node child, int value) { setConstraint(child, ORDER_CONSTRAINT, value); }

    /** The child's placement order, 0 if unset. */
    public static int getOrder(Node child) {
        Object val = child.getProperties().get(ORDER_CONSTRAINT);
        return val instanceof Number ? ((Number) val).intValue() : 0;
    }

    /** Margin between the child and its grid area; null removes it. */
    public static void setMargin(Node child, Insets value) { setConstraint(child, MARGIN_CONSTRAINT, value); }

    /** The child's margin, {@link Insets#EMPTY} if unset. */
    public static Insets getMargin(Node child) {
        Object val = child.getProperties().get(MARGIN_CONSTRAINT);
        return val instanceof Insets ? (Insets) val : Insets.EMPTY;
    }

    private static GridLine getLine(Node child, String key) {
        Object val = child.getProperties().get(key);
        return val instanceof GridLine ? (GridLine) val : GridLine.AUTO;
    }

    private static void setConstraint(Node child, String key, Object value) {
        if (value == null) {
            child.getProperties().remove(key);
        } else {
            child.getProperties().put(key, value);
        }
        requestParentLayout(child);
    }

    private static void requestParentLayout(Node child) {
        if (child.getParent() instanceof CssGrid) {
            child.getParent().requestLayout();
        }
    }

    // ── Layout ────────────────────────────────────────────────────────

    /** Result of placing the items and sizing both axes. */
    private static final class GridState {
        GridPlacement placement;
        List<GridTrackSizing.Track> explicitRows;
        List<GridTrackSizing.Track> columns;
        List<GridTrackSizing.Track> rows;
        double[] itemWidths;
    }

    @Override
    protected void layoutChildren() {
        double insetTop = snappedTopInset();
        double insetLeft = snappedLeftInset();
        double contentWidth = Math.max(0, getWidth() - insetLeft - snappedRightInset());
        double contentHeight = Math.max(0, getHeight() - insetTop - snappedBottomInset());

        GridState state = sizeColumns(contentWidth, contentHeight, false);
        sizeRows(state, contentHeight, false);

        double[] colPos = GridTrackSizing.positions(state.columns, contentWidth, getColumnGap(), getJustifyContent());
        double[] rowPos = GridTrackSizing.positions(state.rows, contentHeight, getRowGap(), getAlignContent());

        // Track edges are snapped once so that adjacent items share pixel-exact boundaries
        double[] colStart = new double[state.columns.size()], colEnd = new double[state.columns.size()];
        for (int i = 0; i < state.columns.size(); i++) {
            colStart[i] = snapPositionX(insetLeft + colPos[i]);
            colEnd[i] = snapPositionX(insetLeft + colPos[i] + state.columns.get(i).base);
        }
        double[] rowStart = new double[state.rows.size()], rowEnd = new double[state.rows.size()];
        for (int i = 0; i < state.rows.size(); i++) {
            rowStart[i] = snapPositionY(insetTop + rowPos[i]);
            rowEnd[i] = snapPositionY(insetTop + rowPos[i] + state.rows.get(i).base);
        }

        for (GridPlacement.Item item : state.placement.items) {
            Node node = item.node;
            Insets m = item.margin;

            double areaX = colStart[item.columnStart];
            double areaW = colEnd[item.columnEnd() - 1] - areaX;
            double areaY = rowStart[item.rowStart];
            double areaH = rowEnd[item.rowEnd() - 1] - areaY;

            double availW = areaW - m.getLeft() - m.getRight();
            double availH = areaH - m.getTop() - m.getBottom();
            double w = resolveItemWidth(node, justifySelfOf(node), availW);
            double h = resolveItemHeight(node, alignSelfOf(node), availH, w);

            double x = areaX + m.getLeft() + alignmentOffset(justifySelfOf(node), availW, w);
            double y = areaY + m.getTop() + alignmentOffset(alignSelfOf(node), availH, h);
            node.resizeRelocate(snapPositionX(x), snapPositionY(y), snapSizeX(w), snapSizeY(h));
        }
    }

    private static double trackEnd(List<GridTrackSizing.Track> tracks, double[] positions, int index) {
        return positions[index] + tracks.get(index).base;
    }

    private GridItemAlignment justifySelfOf(Node node) {
        GridItemAlignment self = getJustifySelf(node);
        return self != null ? self : getJustifyItems();
    }

    private GridItemAlignment alignSelfOf(Node node) {
        GridItemAlignment self = getAlignSelf(node);
        return self != null ? self : getAlignItems();
    }

    private static double alignmentOffset(GridItemAlignment alignment, double available, double size) {
        switch (alignment) {
            case END:    return available - size;
            case CENTER: return (available - size) / 2;
            default:     return 0;
        }
    }

    private static double resolveItemWidth(Node node, GridItemAlignment alignment, double available) {
        double min = node.minWidth(-1);
        double max = node.maxWidth(-1);
        double target = alignment == GridItemAlignment.STRETCH ? available : Math.min(node.prefWidth(-1), available);
        return boundedSize(min, target, max);
    }

    private static double resolveItemHeight(Node node, GridItemAlignment alignment, double available, double width) {
        double alt = node.getContentBias() == Orientation.HORIZONTAL ? width : -1;
        double min = node.minHeight(alt);
        double max = node.maxHeight(alt);
        double target = alignment == GridItemAlignment.STRETCH ? available : Math.min(node.prefHeight(alt), available);
        return boundedSize(min, target, max);
    }

    private static double boundedSize(double min, double value, double max) {
        return Math.min(Math.max(value, min), Math.max(min, max));
    }

    private List<Node> sortedManagedChildren() {
        List<Node> nodes = new ArrayList<>(getManagedChildren());
        nodes.sort(Comparator.comparingInt(CssGrid::getOrder));
        return nodes;
    }

    /**
     * Places the items and sizes the columns.
     *
     * @param contentWidth  available content width, or NaN if indefinite
     * @param contentHeight available content height, or NaN if indefinite (only used to expand auto-fill rows)
     * @param minMode       size intrinsic tracks by minimum contributions only (for computeMinWidth)
     */
    private GridState sizeColumns(double contentWidth, double contentHeight, boolean minMode) {
        double colGap = getColumnGap();
        GridTemplateAreas areas = getTemplateAreas();

        List<GridTrackSizing.Track> explicitCols = GridTrackSizing.expand(getTemplateColumns(), contentWidth, colGap);
        for (int i = 0; explicitCols.size() < areas.getColumnCount(); i++) {
            explicitCols.add(GridTrackSizing.implicitTrack(getAutoColumns(), i, contentWidth));
        }
        List<GridTrackSizing.Track> explicitRows = GridTrackSizing.expand(getTemplateRows(), contentHeight, getRowGap());
        for (int i = 0; explicitRows.size() < areas.getRowCount(); i++) {
            explicitRows.add(GridTrackSizing.implicitTrack(getAutoRows(), i, contentHeight));
        }

        GridState state = new GridState();
        state.explicitRows = explicitRows;
        state.placement = GridPlacement.place(sortedManagedChildren(), explicitCols.size(), explicitRows.size(), getAutoFlow(), areas);
        state.columns = buildTracks(state.placement.columnCount, state.placement.explicitColumnOffset, explicitCols,
                getAutoColumns(), contentWidth);
        collapseEmptyAutoFitTracks(state.columns, state.placement, true);

        List<GridTrackSizing.Contributor> contributors = new ArrayList<>();
        for (GridPlacement.Item item : state.placement.items) {
            double horizontalMargin = item.margin.getLeft() + item.margin.getRight();
            double min = item.node.minWidth(-1) + horizontalMargin;
            double pref = minMode ? min : item.node.prefWidth(-1) + horizontalMargin;
            contributors.add(new GridTrackSizing.Contributor(item.columnStart, item.columnSpan, min, pref));
        }
        GridTrackSizing.size(state.columns, contributors, contentWidth, colGap, getJustifyContent() == GridContentAlignment.STRETCH, minMode);

        double[] colPos = GridTrackSizing.positions(state.columns, Double.isNaN(contentWidth) ? 0 : contentWidth, colGap, GridContentAlignment.START);
        state.itemWidths = new double[state.placement.items.size()];
        for (int i = 0; i < state.itemWidths.length; i++) {
            GridPlacement.Item item = state.placement.items.get(i);
            double areaW = trackEnd(state.columns, colPos, item.columnEnd() - 1) - colPos[item.columnStart];
            double availW = areaW - item.margin.getLeft() - item.margin.getRight();
            state.itemWidths[i] = resolveItemWidth(item.node, justifySelfOf(item.node), availW);
        }
        return state;
    }

    /**
     * Sizes the rows, using the item widths resolved by {@link #sizeColumns}.
     *
     * @param contentHeight available content height, or NaN if indefinite
     */
    private void sizeRows(GridState state, double contentHeight, boolean minMode) {
        double rowGap = getRowGap();
        state.rows = buildTracks(state.placement.rowCount, state.placement.explicitRowOffset, state.explicitRows, getAutoRows(), contentHeight);
        collapseEmptyAutoFitTracks(state.rows, state.placement, false);

        List<GridTrackSizing.Contributor> contributors = new ArrayList<>();
        List<GridPlacement.Item> items = state.placement.items;
        for (int i = 0; i < items.size(); i++) {
            GridPlacement.Item item = items.get(i);
            double alt = item.node.getContentBias() == Orientation.HORIZONTAL ? state.itemWidths[i] : -1;
            double verticalMargin = item.margin.getTop() + item.margin.getBottom();
            double min = item.node.minHeight(alt) + verticalMargin;
            double pref = minMode ? min : item.node.prefHeight(alt) + verticalMargin;
            contributors.add(new GridTrackSizing.Contributor(item.rowStart, item.rowSpan, min, pref));
        }
        GridTrackSizing.size(state.rows, contributors, contentHeight, rowGap, getAlignContent() == GridContentAlignment.STRETCH, minMode);
    }

    private static List<GridTrackSizing.Track> buildTracks(int count, int explicitOffset, List<GridTrackSizing.Track> explicit,
                                                          GridTrackList autoTracks, double available) {
        List<GridTrackSizing.Track> tracks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int explicitIndex = i - explicitOffset;
            if (explicitIndex >= 0 && explicitIndex < explicit.size()) {
                tracks.add(explicit.get(explicitIndex));
            } else {
                int implicitIndex = explicitIndex < 0 ? explicitIndex : explicitIndex - explicit.size();
                tracks.add(GridTrackSizing.implicitTrack(autoTracks, implicitIndex, available));
            }
        }
        return tracks;
    }

    private static void collapseEmptyAutoFitTracks(List<GridTrackSizing.Track> tracks, GridPlacement placement, boolean columns) {
        boolean[] used = new boolean[tracks.size()];
        for (GridPlacement.Item item : placement.items) {
            int start = columns ? item.columnStart : item.rowStart;
            int end = columns ? item.columnEnd() : item.rowEnd();
            for (int i = start; i < end; i++) used[i] = true;
        }
        for (int i = 0; i < tracks.size(); i++) {
            GridTrackSizing.Track t = tracks.get(i);
            t.collapsed = t.autoFit && !used[i];
        }
    }

    // ── Size computation ──────────────────────────────────────────────

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override
    protected double computeMinWidth(double height) {
        GridState state = sizeColumns(Double.NaN, Double.NaN, true);
        return snappedLeftInset() + trackTotal(state.columns, getColumnGap()) + snappedRightInset();
    }

    @Override
    protected double computePrefWidth(double height) {
        GridState state = sizeColumns(Double.NaN, Double.NaN, false);
        return snappedLeftInset() + trackTotal(state.columns, getColumnGap()) + snappedRightInset();
    }

    @Override
    protected double computeMinHeight(double width) {
        GridState state = sizeColumns(contentWidthFor(width), Double.NaN, false);
        sizeRows(state, Double.NaN, true);
        return snappedTopInset() + trackTotal(state.rows, getRowGap()) + snappedBottomInset();
    }

    @Override
    protected double computePrefHeight(double width) {
        GridState state = sizeColumns(contentWidthFor(width), Double.NaN, false);
        sizeRows(state, Double.NaN, false);
        return snappedTopInset() + trackTotal(state.rows, getRowGap()) + snappedBottomInset();
    }

    private double contentWidthFor(double width) {
        return width < 0 ? Double.NaN : Math.max(0, width - snappedLeftInset() - snappedRightInset());
    }

    private static double trackTotal(List<GridTrackSizing.Track> tracks, double gap) {
        return GridTrackSizing.baseSum(tracks) + GridTrackSizing.gapsTotal(tracks, gap);
    }
}
