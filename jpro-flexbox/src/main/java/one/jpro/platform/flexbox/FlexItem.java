package one.jpro.platform.flexbox;

import javafx.css.*;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A StackPane wrapper that exposes FlexBox child properties as CSS-styleable properties.
 * <p>
 * Usage in Java:
 * <pre>
 * FlexItem item = new FlexItem(myButton);
 * item.setFlexGrow(1);
 * flexBox.getChildren().add(item);
 * </pre>
 * <p>
 * Usage in CSS:
 * <pre>
 * .my-item {
 *     flex-grow: 1;
 *     flex-shrink: 0;
 *     flex-basis: 200;
 *     order: 2;
 *     align-self: center;
 * }
 * </pre>
 */
public class FlexItem extends StackPane {

    // ── CSS metadata ──────────────────────────────────────────────────

    private static final CssMetaData<FlexItem, Number> FLEX_GROW_META =
            new CssMetaData<>("flex-grow", StyleConverter.getSizeConverter(), 0) {
                @Override public boolean isSettable(FlexItem node) { return !node.flexGrow.isBound(); }
                @Override public StyleableProperty<Number> getStyleableProperty(FlexItem node) {
                    return (StyleableProperty<Number>) (StyleableProperty<?>) node.flexGrow;
                }
            };

    private static final CssMetaData<FlexItem, Number> FLEX_SHRINK_META =
            new CssMetaData<>("flex-shrink", StyleConverter.getSizeConverter(), 1) {
                @Override public boolean isSettable(FlexItem node) { return !node.flexShrink.isBound(); }
                @Override public StyleableProperty<Number> getStyleableProperty(FlexItem node) {
                    return (StyleableProperty<Number>) (StyleableProperty<?>) node.flexShrink;
                }
            };

    private static final CssMetaData<FlexItem, Number> FLEX_BASIS_META =
            new CssMetaData<>("flex-basis", StyleConverter.getSizeConverter(), -1) {
                @Override public boolean isSettable(FlexItem node) { return !node.flexBasis.isBound(); }
                @Override public StyleableProperty<Number> getStyleableProperty(FlexItem node) {
                    return (StyleableProperty<Number>) (StyleableProperty<?>) node.flexBasis;
                }
            };

    private static final CssMetaData<FlexItem, Number> ORDER_META =
            new CssMetaData<>("order", StyleConverter.getSizeConverter(), 0) {
                @Override public boolean isSettable(FlexItem node) { return !node.order.isBound(); }
                @Override public StyleableProperty<Number> getStyleableProperty(FlexItem node) {
                    return (StyleableProperty<Number>) (StyleableProperty<?>) node.order;
                }
            };

    private static final CssMetaData<FlexItem, FlexAlignItems> ALIGN_SELF_META =
            new CssMetaData<>("align-self", new FlexEnumConverter<>(FlexAlignItems.class)) {
                @Override public boolean isSettable(FlexItem node) { return !node.alignSelf.isBound(); }
                @Override public StyleableProperty<FlexAlignItems> getStyleableProperty(FlexItem node) { return node.alignSelf; }
            };

    private static final List<CssMetaData<? extends Styleable, ?>> CLASS_CSS_META_DATA;
    static {
        List<CssMetaData<? extends Styleable, ?>> list = new ArrayList<>(StackPane.getClassCssMetaData());
        list.add(FLEX_GROW_META);
        list.add(FLEX_SHRINK_META);
        list.add(FLEX_BASIS_META);
        list.add(ORDER_META);
        list.add(ALIGN_SELF_META);
        CLASS_CSS_META_DATA = Collections.unmodifiableList(list);
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CLASS_CSS_META_DATA;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    // ── Properties ────────────────────────────────────────────────────

    private final StyleableDoubleProperty flexGrow = new StyleableDoubleProperty(0) {
        @Override protected void invalidated() { FlexBox.setGrow(FlexItem.this, get()); }
        @Override public Object getBean() { return FlexItem.this; }
        @Override public String getName() { return "flexGrow"; }
        @Override public CssMetaData<FlexItem, Number> getCssMetaData() { return FLEX_GROW_META; }
    };

    public final double getFlexGrow() { return flexGrow.get(); }
    public final void setFlexGrow(double value) { flexGrow.set(value); }
    public final StyleableDoubleProperty flexGrowProperty() { return flexGrow; }

    private final StyleableDoubleProperty flexShrink = new StyleableDoubleProperty(1) {
        @Override protected void invalidated() { FlexBox.setShrink(FlexItem.this, get()); }
        @Override public Object getBean() { return FlexItem.this; }
        @Override public String getName() { return "flexShrink"; }
        @Override public CssMetaData<FlexItem, Number> getCssMetaData() { return FLEX_SHRINK_META; }
    };

    public final double getFlexShrink() { return flexShrink.get(); }
    public final void setFlexShrink(double value) { flexShrink.set(value); }
    public final StyleableDoubleProperty flexShrinkProperty() { return flexShrink; }

    private final StyleableDoubleProperty flexBasis = new StyleableDoubleProperty(-1) {
        @Override protected void invalidated() { FlexBox.setBasis(FlexItem.this, get()); }
        @Override public Object getBean() { return FlexItem.this; }
        @Override public String getName() { return "flexBasis"; }
        @Override public CssMetaData<FlexItem, Number> getCssMetaData() { return FLEX_BASIS_META; }
    };

    public final double getFlexBasis() { return flexBasis.get(); }
    public final void setFlexBasis(double value) { flexBasis.set(value); }
    public final StyleableDoubleProperty flexBasisProperty() { return flexBasis; }

    private final StyleableDoubleProperty order = new StyleableDoubleProperty(0) {
        @Override protected void invalidated() { FlexBox.setOrder(FlexItem.this, (int) get()); }
        @Override public Object getBean() { return FlexItem.this; }
        @Override public String getName() { return "order"; }
        @Override public CssMetaData<FlexItem, Number> getCssMetaData() { return ORDER_META; }
    };

    public final int getFlexOrder() { return (int) order.get(); }
    public final void setFlexOrder(int value) { order.set(value); }
    public final StyleableDoubleProperty orderProperty() { return order; }

    private final StyleableObjectProperty<FlexAlignItems> alignSelf =
            new SimpleStyleableObjectProperty<>(ALIGN_SELF_META, this, "alignSelf") {
                @Override protected void invalidated() { FlexBox.setAlignSelf(FlexItem.this, get()); }
            };

    public final FlexAlignItems getAlignSelf() { return alignSelf.get(); }
    public final void setAlignSelf(FlexAlignItems value) { alignSelf.set(value); }
    public final StyleableObjectProperty<FlexAlignItems> alignSelfProperty() { return alignSelf; }

    // ── Constructors ──────────────────────────────────────────────────

    public FlexItem() {
    }

    public FlexItem(Node... children) {
        super(children);
    }
}
