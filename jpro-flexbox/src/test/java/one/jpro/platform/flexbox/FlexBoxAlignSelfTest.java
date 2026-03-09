package one.jpro.platform.flexbox;

import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlexBoxAlignSelfTest extends FlexBoxTestBase {

    @Test
    void defaultAlignSelfIsNull() {
        Region r = createBox(50, 30);
        assertNull(FlexBox.getAlignSelf(r));
    }

    @Test
    void alignSelfOverridesAlignItems() {
        FlexBox box = new FlexBox();
        box.setAlignItems(FlexAlignItems.FLEX_START);
        Region a = createBox(50, 30);
        Region b = createBox(50, 30);
        FlexBox.setAlignSelf(b, FlexAlignItems.FLEX_END);
        box.getChildren().addAll(a, b);

        layoutAt(box, 400, 200);

        // a: flex-start → Y=0
        assertEquals(0, a.getLayoutY(), 0.5);
        // b: flex-end → Y=200-30=170
        assertEquals(170, b.getLayoutY(), 0.5);
    }

    @Test
    void alignSelfCenter() {
        FlexBox box = new FlexBox();
        box.setAlignItems(FlexAlignItems.FLEX_START);
        Region a = createBox(50, 30);
        FlexBox.setAlignSelf(a, FlexAlignItems.CENTER);
        box.getChildren().add(a);

        layoutAt(box, 400, 200);

        assertEquals(85, a.getLayoutY(), 0.5);
    }

    @Test
    void alignSelfStretch() {
        FlexBox box = new FlexBox();
        box.setAlignItems(FlexAlignItems.FLEX_START);
        Region a = createBox(50, 30);
        Region b = createBox(50, 30);
        FlexBox.setAlignSelf(b, FlexAlignItems.STRETCH);
        box.getChildren().addAll(a, b);

        layoutAt(box, 400, 200);

        // a stays at pref height
        assertEquals(30, a.getLayoutBounds().getHeight(), 0.5);
        // b stretches to full cross size
        assertEquals(200, b.getLayoutBounds().getHeight(), 0.5);
    }

    @Test
    void alignSelfInWrappedLine() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP);
        box.setAlignItems(FlexAlignItems.FLEX_START);
        box.setAlignContent(FlexAlignContent.FLEX_START);
        Region a = createBox(150, 40);
        Region b = createBox(150, 60); // taller, sets line height
        Region c = createBox(150, 40);
        FlexBox.setAlignSelf(a, FlexAlignItems.FLEX_END);
        box.getChildren().addAll(a, b, c);

        layoutAt(box, 350, 300);

        // Line 1: a and b, line height = 60
        // a has align-self: flex-end → Y = 60 - 40 = 20
        assertEquals(20, a.getLayoutY(), 0.5);
        // b at flex-start → Y = 0
        assertEquals(0, b.getLayoutY(), 0.5);
    }
}
