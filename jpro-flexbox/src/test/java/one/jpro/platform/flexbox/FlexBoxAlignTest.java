package one.jpro.platform.flexbox;

import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlexBoxAlignTest extends FlexBoxTestBase {

    @Test
    void stretchIsDefault() {
        FlexBox box = new FlexBox();
        assertEquals(FlexAlignItems.STRETCH, box.getAlignItems());
    }

    @Test
    void stretchExpandsCrossAxis() {
        FlexBox box = new FlexBox();
        box.setAlignItems(FlexAlignItems.STRETCH);
        Region a = createBox(50, 30);
        box.getChildren().add(a);

        layoutAt(box, 400, 200);

        assertEquals(200, a.getLayoutBounds().getHeight(), 0.5);
    }

    @Test
    void flexStartAlignsToCrossStart() {
        FlexBox box = new FlexBox();
        box.setAlignItems(FlexAlignItems.FLEX_START);
        Region a = createBox(50, 30);
        box.getChildren().add(a);

        layoutAt(box, 400, 200);

        assertEquals(0, a.getLayoutY(), 0.5);
        assertEquals(30, a.getLayoutBounds().getHeight(), 0.5);
    }

    @Test
    void flexEndAlignsToCrossEnd() {
        FlexBox box = new FlexBox();
        box.setAlignItems(FlexAlignItems.FLEX_END);
        Region a = createBox(50, 30);
        box.getChildren().add(a);

        layoutAt(box, 400, 200);

        assertEquals(170, a.getLayoutY(), 0.5);
    }

    @Test
    void centerCentersOnCrossAxis() {
        FlexBox box = new FlexBox();
        box.setAlignItems(FlexAlignItems.CENTER);
        Region a = createBox(50, 30);
        box.getChildren().add(a);

        layoutAt(box, 400, 200);

        assertEquals(85, a.getLayoutY(), 0.5);
    }

    @Test
    void stretchRespectsMaxHeight() {
        FlexBox box = new FlexBox();
        box.setAlignItems(FlexAlignItems.STRETCH);
        Region a = createBox(50, 30);
        a.setMaxHeight(100);
        box.getChildren().add(a);

        layoutAt(box, 400, 200);

        assertEquals(100, a.getLayoutBounds().getHeight(), 0.5);
    }

    @Test
    void alignInColumnDirection() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.COLUMN);
        box.setAlignItems(FlexAlignItems.CENTER);
        Region a = createBox(50, 30);
        box.getChildren().add(a);

        layoutAt(box, 200, 400);

        // Cross axis is now horizontal: (200 - 50) / 2 = 75
        assertEquals(75, a.getLayoutX(), 0.5);
    }
}
