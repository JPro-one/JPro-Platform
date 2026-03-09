package one.jpro.platform.flexbox;

import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlexBoxDirectionTest extends FlexBoxTestBase {

    @Test
    void rowLayoutsLeftToRight() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.ROW);
        Region a = createBox(50, 30);
        Region b = createBox(60, 30);
        box.getChildren().addAll(a, b);

        layoutAt(box, 400, 200);

        assertEquals(0, a.getLayoutX(), 0.5);
        assertEquals(50, b.getLayoutX(), 0.5);
    }

    @Test
    void rowReverseLayoutsRightToLeft() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.ROW_REVERSE);
        Region a = createBox(50, 30);
        Region b = createBox(60, 30);
        box.getChildren().addAll(a, b);

        layoutAt(box, 400, 200);

        // In reverse, 'b' is placed first (from the left), then 'a'
        assertTrue(a.getLayoutX() > b.getLayoutX(),
                "In ROW_REVERSE, first child should be to the right of second child");
    }

    @Test
    void columnLayoutsTopToBottom() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.COLUMN);
        Region a = createBox(50, 30);
        Region b = createBox(50, 40);
        box.getChildren().addAll(a, b);

        layoutAt(box, 200, 400);

        assertEquals(0, a.getLayoutY(), 0.5);
        assertEquals(30, b.getLayoutY(), 0.5);
    }

    @Test
    void columnReverseLayoutsBottomToTop() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.COLUMN_REVERSE);
        Region a = createBox(50, 30);
        Region b = createBox(50, 40);
        box.getChildren().addAll(a, b);

        layoutAt(box, 200, 400);

        assertTrue(a.getLayoutY() > b.getLayoutY(),
                "In COLUMN_REVERSE, first child should be below second child");
    }

    @Test
    void defaultDirectionIsRow() {
        FlexBox box = new FlexBox();
        assertEquals(FlexDirection.ROW, box.getDirection());
    }
}
