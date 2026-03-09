package one.jpro.platform.flexbox;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.css.*;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A JavaFX layout pane that implements CSS FlexBox layout.
 * <p>
 * Container properties are styleable via CSS:
 * <pre>
 * .my-flexbox {
 *     flex-direction: row;
 *     flex-wrap: wrap;
 *     justify-content: center;
 *     align-items: stretch;
 *     align-content: flex-start;
 *     gap: 12;
 * }
 * </pre>
 * <p>
 * Per-child constraints are set via static methods:
 * {@link #setGrow}, {@link #setShrink}, {@link #setBasis}, {@link #setAlignSelf}, {@link #setOrder}.
 */
public class FlexBox extends Pane {

    private static final String GROW_CONSTRAINT = "flexbox-grow";
    private static final String SHRINK_CONSTRAINT = "flexbox-shrink";
    private static final String BASIS_CONSTRAINT = "flexbox-basis";
    private static final String ALIGN_SELF_CONSTRAINT = "flexbox-align-self";
    private static final String ORDER_CONSTRAINT = "flexbox-order";

    // ── CSS metadata ──────────────────────────────────────────────────

    private static final CssMetaData<FlexBox, FlexDirection> DIRECTION_META =
            new CssMetaData<>("flex-direction", new FlexEnumConverter<>(FlexDirection.class), FlexDirection.ROW) {
                @Override public boolean isSettable(FlexBox node) { return !node.direction.isBound(); }
                @Override public StyleableProperty<FlexDirection> getStyleableProperty(FlexBox node) { return node.direction; }
            };

    private static final CssMetaData<FlexBox, FlexWrap> WRAP_META =
            new CssMetaData<>("flex-wrap", new FlexEnumConverter<>(FlexWrap.class), FlexWrap.NOWRAP) {
                @Override public boolean isSettable(FlexBox node) { return !node.wrap.isBound(); }
                @Override public StyleableProperty<FlexWrap> getStyleableProperty(FlexBox node) { return node.wrap; }
            };

    private static final CssMetaData<FlexBox, FlexJustifyContent> JUSTIFY_CONTENT_META =
            new CssMetaData<>("justify-content", new FlexEnumConverter<>(FlexJustifyContent.class), FlexJustifyContent.FLEX_START) {
                @Override public boolean isSettable(FlexBox node) { return !node.justifyContent.isBound(); }
                @Override public StyleableProperty<FlexJustifyContent> getStyleableProperty(FlexBox node) { return node.justifyContent; }
            };

    private static final CssMetaData<FlexBox, FlexAlignItems> ALIGN_ITEMS_META =
            new CssMetaData<>("align-items", new FlexEnumConverter<>(FlexAlignItems.class), FlexAlignItems.STRETCH) {
                @Override public boolean isSettable(FlexBox node) { return !node.alignItems.isBound(); }
                @Override public StyleableProperty<FlexAlignItems> getStyleableProperty(FlexBox node) { return node.alignItems; }
            };

    private static final CssMetaData<FlexBox, FlexAlignContent> ALIGN_CONTENT_META =
            new CssMetaData<>("align-content", new FlexEnumConverter<>(FlexAlignContent.class), FlexAlignContent.STRETCH) {
                @Override public boolean isSettable(FlexBox node) { return !node.alignContent.isBound(); }
                @Override public StyleableProperty<FlexAlignContent> getStyleableProperty(FlexBox node) { return node.alignContent; }
            };

    private static final CssMetaData<FlexBox, Number> GAP_META =
            new CssMetaData<>("gap", StyleConverter.getSizeConverter(), 0) {
                @Override public boolean isSettable(FlexBox node) { return !node.gap.isBound(); }
                @Override public StyleableProperty<Number> getStyleableProperty(FlexBox node) { return (StyleableProperty<Number>) (StyleableProperty<?>) node.gap; }
            };

    private static final List<CssMetaData<? extends Styleable, ?>> CLASS_CSS_META_DATA;
    static {
        List<CssMetaData<? extends Styleable, ?>> list = new ArrayList<>(Pane.getClassCssMetaData());
        list.add(DIRECTION_META);
        list.add(WRAP_META);
        list.add(JUSTIFY_CONTENT_META);
        list.add(ALIGN_ITEMS_META);
        list.add(ALIGN_CONTENT_META);
        list.add(GAP_META);
        CLASS_CSS_META_DATA = Collections.unmodifiableList(list);
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CLASS_CSS_META_DATA;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    // ── Container properties (styleable) ──────────────────────────────

    private final StyleableObjectProperty<FlexDirection> direction =
            new SimpleStyleableObjectProperty<>(DIRECTION_META, this, "direction", FlexDirection.ROW) {
                @Override protected void invalidated() { requestLayout(); }
            };

    public final FlexDirection getDirection() { return direction.get(); }
    public final void setDirection(FlexDirection value) { direction.set(value); }
    public final ObjectProperty<FlexDirection> directionProperty() { return direction; }

    private final StyleableObjectProperty<FlexWrap> wrap =
            new SimpleStyleableObjectProperty<>(WRAP_META, this, "wrap", FlexWrap.NOWRAP) {
                @Override protected void invalidated() { requestLayout(); }
            };

    public final FlexWrap getWrap() { return wrap.get(); }
    public final void setWrap(FlexWrap value) { wrap.set(value); }
    public final ObjectProperty<FlexWrap> wrapProperty() { return wrap; }

    private final StyleableObjectProperty<FlexJustifyContent> justifyContent =
            new SimpleStyleableObjectProperty<>(JUSTIFY_CONTENT_META, this, "justifyContent", FlexJustifyContent.FLEX_START) {
                @Override protected void invalidated() { requestLayout(); }
            };

    public final FlexJustifyContent getJustifyContent() { return justifyContent.get(); }
    public final void setJustifyContent(FlexJustifyContent value) { justifyContent.set(value); }
    public final ObjectProperty<FlexJustifyContent> justifyContentProperty() { return justifyContent; }

    private final StyleableObjectProperty<FlexAlignItems> alignItems =
            new SimpleStyleableObjectProperty<>(ALIGN_ITEMS_META, this, "alignItems", FlexAlignItems.STRETCH) {
                @Override protected void invalidated() { requestLayout(); }
            };

    public final FlexAlignItems getAlignItems() { return alignItems.get(); }
    public final void setAlignItems(FlexAlignItems value) { alignItems.set(value); }
    public final ObjectProperty<FlexAlignItems> alignItemsProperty() { return alignItems; }

    private final StyleableObjectProperty<FlexAlignContent> alignContent =
            new SimpleStyleableObjectProperty<>(ALIGN_CONTENT_META, this, "alignContent", FlexAlignContent.STRETCH) {
                @Override protected void invalidated() { requestLayout(); }
            };

    public final FlexAlignContent getAlignContent() { return alignContent.get(); }
    public final void setAlignContent(FlexAlignContent value) { alignContent.set(value); }
    public final ObjectProperty<FlexAlignContent> alignContentProperty() { return alignContent; }

    private final StyleableDoubleProperty gap =
            new StyleableDoubleProperty(0) {
                @Override protected void invalidated() { requestLayout(); }
                @Override public Object getBean() { return FlexBox.this; }
                @Override public String getName() { return "gap"; }
                @Override public CssMetaData<FlexBox, Number> getCssMetaData() {
                    return GAP_META;
                }
            };

    public final double getGap() { return gap.get(); }
    public final void setGap(double value) { gap.set(value); }
    public final DoubleProperty gapProperty() { return gap; }

    // ── Static child constraint methods ───────────────────────────────

    public static void setGrow(Node child, double value) {
        child.getProperties().put(GROW_CONSTRAINT, value);
        requestParentLayout(child);
    }

    public static double getGrow(Node child) {
        Object val = child.getProperties().get(GROW_CONSTRAINT);
        return val instanceof Number ? ((Number) val).doubleValue() : 0;
    }

    public static void setShrink(Node child, double value) {
        child.getProperties().put(SHRINK_CONSTRAINT, value);
        requestParentLayout(child);
    }

    public static double getShrink(Node child) {
        Object val = child.getProperties().get(SHRINK_CONSTRAINT);
        return val instanceof Number ? ((Number) val).doubleValue() : 1;
    }

    public static void setBasis(Node child, double value) {
        child.getProperties().put(BASIS_CONSTRAINT, value);
        requestParentLayout(child);
    }

    public static double getBasis(Node child) {
        Object val = child.getProperties().get(BASIS_CONSTRAINT);
        return val instanceof Number ? ((Number) val).doubleValue() : -1;
    }

    public static void setAlignSelf(Node child, FlexAlignItems value) {
        child.getProperties().put(ALIGN_SELF_CONSTRAINT, value);
        requestParentLayout(child);
    }

    public static FlexAlignItems getAlignSelf(Node child) {
        Object val = child.getProperties().get(ALIGN_SELF_CONSTRAINT);
        return val instanceof FlexAlignItems ? (FlexAlignItems) val : null;
    }

    public static void setOrder(Node child, int value) {
        child.getProperties().put(ORDER_CONSTRAINT, value);
        requestParentLayout(child);
    }

    public static int getOrder(Node child) {
        Object val = child.getProperties().get(ORDER_CONSTRAINT);
        return val instanceof Number ? ((Number) val).intValue() : 0;
    }

    private static void requestParentLayout(Node child) {
        if (child.getParent() instanceof FlexBox) {
            child.getParent().requestLayout();
        }
    }

    // ── Internal data structures ──────────────────────────────────────

    private static class FlexItem {
        final Node node;
        final int originalIndex;
        double basis;
        double mainSize;
        double crossSize;
        double mainPos;
        double crossPos;

        FlexItem(Node node, int originalIndex) {
            this.node = node;
            this.originalIndex = originalIndex;
        }
    }

    private static class FlexLine {
        final List<FlexItem> items = new ArrayList<>();
        double totalBasis;
        double crossSize;
        double crossPos;
        double crossSizeActual;
    }

    // ── Layout ────────────────────────────────────────────────────────

    @Override
    protected void layoutChildren() {
        List<Node> managed = getManagedChildren();
        if (managed.isEmpty()) return;

        double insetTop = snappedTopInset();
        double insetRight = snappedRightInset();
        double insetBottom = snappedBottomInset();
        double insetLeft = snappedLeftInset();

        double contentWidth = getWidth() - insetLeft - insetRight;
        double contentHeight = getHeight() - insetTop - insetBottom;

        FlexDirection dir = getDirection();
        boolean isRow = dir.isRow();
        boolean isReverse = dir.isReverse();
        FlexWrap wrapMode = getWrap();
        boolean doWrap = wrapMode != FlexWrap.NOWRAP;
        double mainSize = isRow ? contentWidth : contentHeight;
        double crossSize = isRow ? contentHeight : contentWidth;
        double gapVal = getGap();

        // 1. Build sorted item list (stable sort by order)
        List<FlexItem> items = new ArrayList<>(managed.size());
        for (int i = 0; i < managed.size(); i++) {
            items.add(new FlexItem(managed.get(i), i));
        }
        items.sort(Comparator.comparingInt(a -> getOrder(a.node)));

        // 2. Compute basis for each item
        for (FlexItem item : items) {
            double basis = getBasis(item.node);
            if (basis < 0) {
                basis = isRow ? item.node.prefWidth(-1) : item.node.prefHeight(-1);
            }
            item.basis = basis;
        }

        // 3. Break into lines
        List<FlexLine> lines = buildLines(items, mainSize, gapVal, doWrap);

        // 4. Resolve main-axis sizes per line (grow/shrink)
        for (FlexLine line : lines) {
            resolveMainSizes(line, mainSize, gapVal, isRow);
        }

        // 5. Compute cross sizes per line
        for (FlexLine line : lines) {
            double maxCross = 0;
            for (FlexItem item : line.items) {
                double pref = isRow ? item.node.prefHeight(-1) : item.node.prefWidth(-1);
                maxCross = Math.max(maxCross, pref);
            }
            line.crossSize = maxCross;
        }

        // 6. Distribute lines along cross axis (align-content)
        distributeCrossLines(lines, crossSize, gapVal, wrapMode);

        // 7. Position items on main axis (justify-content) and cross axis (align-items/align-self)
        FlexAlignItems defaultAlign = getAlignItems();

        for (FlexLine line : lines) {
            positionMainAxis(line, mainSize, gapVal, isReverse);
            positionCrossAxis(line, defaultAlign, isRow);
        }

        // 8. Place children
        for (FlexLine line : lines) {
            for (FlexItem item : line.items) {
                double x, y, w, h;
                if (isRow) {
                    x = insetLeft + item.mainPos;
                    y = insetTop + item.crossPos + line.crossPos;
                    w = item.mainSize;
                    h = item.crossSize;
                } else {
                    x = insetLeft + item.crossPos + line.crossPos;
                    y = insetTop + item.mainPos;
                    w = item.crossSize;
                    h = item.mainSize;
                }
                item.node.resizeRelocate(snapPositionX(x), snapPositionY(y),
                        snapSizeX(w), snapSizeY(h));
            }
        }
    }

    private List<FlexLine> buildLines(List<FlexItem> items, double mainSize, double gapVal, boolean doWrap) {
        List<FlexLine> lines = new ArrayList<>();
        FlexLine currentLine = new FlexLine();
        lines.add(currentLine);

        double usedMain = 0;
        for (int i = 0; i < items.size(); i++) {
            FlexItem item = items.get(i);
            double neededGap = currentLine.items.isEmpty() ? 0 : gapVal;

            if (doWrap && !currentLine.items.isEmpty() && usedMain + neededGap + item.basis > mainSize) {
                currentLine = new FlexLine();
                lines.add(currentLine);
                usedMain = 0;
                neededGap = 0;
            }

            currentLine.items.add(item);
            currentLine.totalBasis += item.basis;
            usedMain += neededGap + item.basis;
        }

        return lines;
    }

    private void resolveMainSizes(FlexLine line, double mainSize, double gapVal, boolean isRow) {
        List<FlexItem> items = line.items;
        double totalGap = items.size() > 1 ? gapVal * (items.size() - 1) : 0;
        double freeSpace = mainSize - line.totalBasis - totalGap;

        if (freeSpace >= 0) {
            double totalGrow = 0;
            for (FlexItem item : items) totalGrow += getGrow(item.node);

            for (FlexItem item : items) {
                double grow = getGrow(item.node);
                double extra = (totalGrow > 0) ? freeSpace * (grow / totalGrow) : 0;
                item.mainSize = Math.max(0, item.basis + extra);
            }
        } else {
            resolveShrink(items, freeSpace, isRow);
        }
    }

    private void resolveShrink(List<FlexItem> items, double deficit, boolean isRow) {
        boolean[] frozen = new boolean[items.size()];
        double[] sizes = new double[items.size()];

        for (int i = 0; i < items.size(); i++) {
            sizes[i] = items.get(i).basis;
        }

        double remaining = deficit;

        for (int iteration = 0; iteration < items.size(); iteration++) {
            double totalScaledShrink = 0;
            for (int i = 0; i < items.size(); i++) {
                if (!frozen[i]) {
                    totalScaledShrink += getShrink(items.get(i).node) * items.get(i).basis;
                }
            }

            if (totalScaledShrink == 0) break;

            boolean anyFrozen = false;
            for (int i = 0; i < items.size(); i++) {
                if (frozen[i]) continue;

                FlexItem item = items.get(i);
                double scaledShrink = getShrink(item.node) * item.basis;
                double reduction = remaining * (scaledShrink / totalScaledShrink);
                double tentative = item.basis + reduction;

                double minMain = isRow ? item.node.minWidth(-1) : item.node.minHeight(-1);
                if (tentative < minMain) {
                    sizes[i] = minMain;
                    remaining -= (minMain - item.basis);
                    frozen[i] = true;
                    anyFrozen = true;
                } else {
                    sizes[i] = tentative;
                }
            }

            if (!anyFrozen) break;
        }

        for (int i = 0; i < items.size(); i++) {
            items.get(i).mainSize = Math.max(0, sizes[i]);
        }
    }

    private void distributeCrossLines(List<FlexLine> lines, double crossSize, double gapVal, FlexWrap wrapMode) {
        boolean reverseLines = wrapMode == FlexWrap.WRAP_REVERSE;
        double totalLinesCross = 0;
        for (FlexLine line : lines) totalLinesCross += line.crossSize;
        double totalCrossGap = lines.size() > 1 ? gapVal * (lines.size() - 1) : 0;
        double freeCross = crossSize - totalLinesCross - totalCrossGap;

        FlexAlignContent ac = getAlignContent();

        if (lines.size() == 1) {
            FlexLine line = lines.get(0);
            line.crossPos = 0;
            line.crossSizeActual = crossSize;
            return;
        }

        double stretchExtra = 0;
        if (ac == FlexAlignContent.STRETCH && freeCross > 0) {
            stretchExtra = freeCross / lines.size();
            freeCross = 0;
        }

        double startPos = 0;
        double lineGap = gapVal;

        if (freeCross > 0) {
            switch (ac) {
                case FLEX_END:
                    startPos = freeCross;
                    break;
                case CENTER:
                    startPos = freeCross / 2;
                    break;
                case SPACE_BETWEEN:
                    lineGap = gapVal + (lines.size() > 1 ? freeCross / (lines.size() - 1) : 0);
                    break;
                case SPACE_AROUND:
                    startPos = freeCross / (lines.size() * 2);
                    lineGap = gapVal + freeCross / lines.size();
                    break;
                case SPACE_EVENLY:
                    startPos = freeCross / (lines.size() + 1);
                    lineGap = gapVal + freeCross / (lines.size() + 1);
                    break;
                default:
                    break;
            }
        }

        List<FlexLine> ordered = reverseLines ? new ArrayList<>(lines) : lines;
        if (reverseLines) {
            Collections.reverse(ordered);
        }

        double pos = startPos;
        for (int i = 0; i < ordered.size(); i++) {
            FlexLine line = ordered.get(i);
            line.crossPos = pos;
            line.crossSizeActual = line.crossSize + stretchExtra;
            pos += line.crossSizeActual + (i < ordered.size() - 1 ? lineGap : 0);
        }
    }

    private void positionMainAxis(FlexLine line, double mainSize, double gapVal, boolean isReverse) {
        List<FlexItem> items = line.items;
        int count = items.size();
        double totalGap = count > 1 ? gapVal * (count - 1) : 0;

        double usedMain = 0;
        for (FlexItem item : items) usedMain += item.mainSize;
        double remainingSpace = mainSize - usedMain - totalGap;

        double mainStart = computeMainStart(remainingSpace, count);
        double mainGap = computeMainGap(remainingSpace, count, gapVal);

        double pos = mainStart;
        for (int i = 0; i < count; i++) {
            int idx = isReverse ? count - 1 - i : i;
            items.get(idx).mainPos = pos;
            pos += items.get(idx).mainSize + mainGap;
        }
    }

    private void positionCrossAxis(FlexLine line, FlexAlignItems defaultAlign, boolean isRow) {
        double lineCross = line.crossSizeActual;

        for (FlexItem item : line.items) {
            FlexAlignItems align = getAlignSelf(item.node);
            if (align == null) align = defaultAlign;

            item.crossSize = computeChildCrossSize(item.node, align, lineCross, isRow);
            item.crossPos = computeCrossPosition(item.node, align, lineCross, item.crossSize, isRow);
        }
    }

    private double computeMainStart(double remainingSpace, int count) {
        if (remainingSpace <= 0) return 0;
        switch (getJustifyContent()) {
            case FLEX_END:      return remainingSpace;
            case CENTER:        return remainingSpace / 2;
            case SPACE_BETWEEN: return 0;
            case SPACE_AROUND:  return count > 0 ? remainingSpace / (count * 2) : 0;
            case SPACE_EVENLY:  return count > 0 ? remainingSpace / (count + 1) : 0;
            default:            return 0;
        }
    }

    private double computeMainGap(double remainingSpace, int count, double gapVal) {
        if (count <= 1) return 0;
        if (remainingSpace <= 0) return gapVal;
        switch (getJustifyContent()) {
            case SPACE_BETWEEN: return gapVal + remainingSpace / (count - 1);
            case SPACE_AROUND:  return gapVal + remainingSpace / count;
            case SPACE_EVENLY:  return gapVal + remainingSpace / (count + 1);
            default:            return gapVal;
        }
    }

    private double computeChildCrossSize(Node child, FlexAlignItems align, double crossSize, boolean isRow) {
        if (align == FlexAlignItems.STRETCH) {
            double max = isRow ? child.maxHeight(-1) : child.maxWidth(-1);
            return Math.min(crossSize, max);
        }
        return isRow ? child.prefHeight(-1) : child.prefWidth(-1);
    }

    private double computeCrossPosition(Node child, FlexAlignItems align, double crossSize, double childCrossSize, boolean isRow) {
        switch (align) {
            case FLEX_END:  return crossSize - childCrossSize;
            case CENTER:    return (crossSize - childCrossSize) / 2;
            case BASELINE:
                if (isRow) {
                    return child.getBaselineOffset();
                }
                return 0;
            default:        return 0;
        }
    }

    // ── Size computation ──────────────────────────────────────────────

    @Override
    protected double computeMinWidth(double height) {
        List<Node> children = getManagedChildren();
        boolean isRow = getDirection().isRow();

        if (isRow) {
            if (getWrap() != FlexWrap.NOWRAP) {
                double max = 0;
                for (Node child : children) {
                    max = Math.max(max, child.minWidth(-1));
                }
                return snappedLeftInset() + max + snappedRightInset();
            } else {
                double total = 0;
                double gapVal = getGap();
                for (Node child : children) {
                    total += child.minWidth(-1);
                }
                total += children.size() > 1 ? gapVal * (children.size() - 1) : 0;
                return snappedLeftInset() + total + snappedRightInset();
            }
        } else {
            double max = 0;
            for (Node child : children) {
                max = Math.max(max, child.minWidth(-1));
            }
            return snappedLeftInset() + max + snappedRightInset();
        }
    }

    @Override
    protected double computeMinHeight(double width) {
        List<Node> children = getManagedChildren();
        boolean isRow = getDirection().isRow();

        if (!isRow) {
            if (getWrap() != FlexWrap.NOWRAP) {
                double max = 0;
                for (Node child : children) {
                    max = Math.max(max, child.minHeight(-1));
                }
                return snappedTopInset() + max + snappedBottomInset();
            } else {
                double total = 0;
                double gapVal = getGap();
                for (Node child : children) {
                    total += child.minHeight(-1);
                }
                total += children.size() > 1 ? gapVal * (children.size() - 1) : 0;
                return snappedTopInset() + total + snappedBottomInset();
            }
        } else {
            double max = 0;
            for (Node child : children) {
                max = Math.max(max, child.minHeight(-1));
            }
            return snappedTopInset() + max + snappedBottomInset();
        }
    }

    @Override
    protected double computePrefWidth(double height) {
        List<Node> children = getManagedChildren();
        boolean isRow = getDirection().isRow();
        double gapVal = getGap();

        if (isRow) {
            double total = 0;
            for (Node child : children) {
                double basis = getBasis(child);
                total += basis >= 0 ? basis : child.prefWidth(-1);
            }
            total += children.size() > 1 ? gapVal * (children.size() - 1) : 0;
            return snappedLeftInset() + total + snappedRightInset();
        } else {
            double max = 0;
            for (Node child : children) {
                max = Math.max(max, child.prefWidth(-1));
            }
            return snappedLeftInset() + max + snappedRightInset();
        }
    }

    @Override
    protected double computePrefHeight(double width) {
        List<Node> children = getManagedChildren();
        boolean isRow = getDirection().isRow();
        double gapVal = getGap();

        if (!isRow) {
            double total = 0;
            for (Node child : children) {
                double basis = getBasis(child);
                total += basis >= 0 ? basis : child.prefHeight(-1);
            }
            total += children.size() > 1 ? gapVal * (children.size() - 1) : 0;
            return snappedTopInset() + total + snappedBottomInset();
        } else {
            double max = 0;
            for (Node child : children) {
                max = Math.max(max, child.prefHeight(-1));
            }
            return snappedTopInset() + max + snappedBottomInset();
        }
    }
}
