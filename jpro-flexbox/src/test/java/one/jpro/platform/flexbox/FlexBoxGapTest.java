package one.jpro.platform.flexbox;

import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlexBoxGapTest extends FlexBoxTestBase {

    @Test
    void defaultGapIsZero() {
        FlexBox box = new FlexBox();
        assertEquals(0, box.getGap(), 0.01);
    }

    @Test
    void gapAddsSpaceBetweenChildren() {
        FlexBox box = new FlexBox();
        box.setGap(10);
        Region a = createBox(50, 30);
        Region b = createBox(50, 30);
        Region c = createBox(50, 30);
        box.getChildren().addAll(a, b, c);

        layoutAt(box, 400, 200);

        assertEquals(0, a.getLayoutX(), 0.5);
        assertEquals(60, b.getLayoutX(), 0.5);
        assertEquals(120, c.getLayoutX(), 0.5);
    }

    @Test
    void gapInColumnDirection() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.COLUMN);
        box.setGap(15);
        Region a = createBox(50, 40);
        Region b = createBox(50, 40);
        box.getChildren().addAll(a, b);

        layoutAt(box, 200, 400);

        assertEquals(0, a.getLayoutY(), 0.5);
        assertEquals(55, b.getLayoutY(), 0.5);
    }

    @Test
    void gapWithGrow() {
        FlexBox box = new FlexBox();
        box.setGap(20);
        Region a = createBox(0, 30);
        Region b = createBox(0, 30);
        FlexBox.setGrow(a, 1);
        FlexBox.setGrow(b, 1);
        FlexBox.setBasis(a, 0);
        FlexBox.setBasis(b, 0);
        box.getChildren().addAll(a, b);

        layoutAt(box, 400, 200);

        // 400 - 20 gap = 380 / 2 = 190 each
        assertEquals(190, a.getLayoutBounds().getWidth(), 0.5);
        assertEquals(190, b.getLayoutBounds().getWidth(), 0.5);
        assertEquals(210, b.getLayoutX(), 0.5); // 190 + 20
    }

    @Test
    void singleChildNoGap() {
        FlexBox box = new FlexBox();
        box.setGap(10);
        Region a = createBox(50, 30);
        box.getChildren().add(a);

        layoutAt(box, 400, 200);

        assertEquals(0, a.getLayoutX(), 0.5);
        assertEquals(50, a.getLayoutBounds().getWidth(), 0.5);
    }
}
