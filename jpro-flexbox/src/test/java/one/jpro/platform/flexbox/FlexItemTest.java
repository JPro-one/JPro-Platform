package one.jpro.platform.flexbox;

import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FlexItem CSS-styleable wrapper.
 */
class FlexItemTest extends FlexBoxTestBase {

    @Test
    void cssMetaDataContainsFlexProperties() {
        List<String> names = FlexItem.getClassCssMetaData().stream()
                .map(CssMetaData::getProperty)
                .collect(Collectors.toList());

        assertTrue(names.contains("flex-grow"));
        assertTrue(names.contains("flex-shrink"));
        assertTrue(names.contains("flex-basis"));
        assertTrue(names.contains("order"));
        assertTrue(names.contains("align-self"));
    }

    @Test
    void defaultValues() {
        FlexItem item = new FlexItem();
        assertEquals(0, item.getFlexGrow(), 0.01);
        assertEquals(1, item.getFlexShrink(), 0.01);
        assertEquals(-1, item.getFlexBasis(), 0.01);
        assertEquals(0, item.getFlexOrder());
        assertNull(item.getAlignSelf());
    }

    @Test
    void setFlexGrowForwardsToConstraint() {
        FlexItem item = new FlexItem();
        item.setFlexGrow(2);
        assertEquals(2, FlexBox.getGrow(item), 0.01);
    }

    @Test
    void setFlexShrinkForwardsToConstraint() {
        FlexItem item = new FlexItem();
        item.setFlexShrink(0);
        assertEquals(0, FlexBox.getShrink(item), 0.01);
    }

    @Test
    void setFlexBasisForwardsToConstraint() {
        FlexItem item = new FlexItem();
        item.setFlexBasis(200);
        assertEquals(200, FlexBox.getBasis(item), 0.01);
    }

    @Test
    void setOrderForwardsToConstraint() {
        FlexItem item = new FlexItem();
        item.setFlexOrder(3);
        assertEquals(3, FlexBox.getOrder(item));
    }

    @Test
    void setAlignSelfForwardsToConstraint() {
        FlexItem item = new FlexItem();
        item.setAlignSelf(FlexAlignItems.CENTER);
        assertEquals(FlexAlignItems.CENTER, FlexBox.getAlignSelf(item));
    }

    @Test
    void worksInsideFlexBox() {
        FlexBox box = new FlexBox();

        Region content = new Region();
        content.setPrefSize(50, 30);

        FlexItem item1 = new FlexItem(content);
        item1.setPrefSize(50, 30);
        item1.setMinSize(20, 20);
        item1.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        item1.setFlexGrow(1);

        FlexItem item2 = new FlexItem();
        item2.setPrefSize(50, 30);
        item2.setMinSize(20, 20);
        item2.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        item2.setFlexGrow(1);

        box.getChildren().addAll(item1, item2);
        layoutAt(box, 200, 100);

        // Each should get half: 100px
        assertEquals(100, item1.getLayoutBounds().getWidth(), 0.5);
        assertEquals(100, item2.getLayoutBounds().getWidth(), 0.5);
    }

    @Test
    void constructorAcceptsChildren() {
        Region child1 = new Region();
        Region child2 = new Region();
        FlexItem item = new FlexItem(child1, child2);
        assertEquals(2, item.getChildren().size());
    }

    @Test
    void includesParentCssProperties() {
        List<String> names = FlexItem.getClassCssMetaData().stream()
                .map(CssMetaData::getProperty)
                .collect(Collectors.toList());

        // Should also have StackPane properties
        assertTrue(names.contains("-fx-alignment"));
    }
}
