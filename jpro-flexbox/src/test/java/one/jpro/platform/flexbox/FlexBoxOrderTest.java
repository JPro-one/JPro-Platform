package one.jpro.platform.flexbox;

import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlexBoxOrderTest extends FlexBoxTestBase {

    @Test
    void defaultOrderIsZero() {
        Region r = createBox(50, 30);
        assertEquals(0, FlexBox.getOrder(r));
    }

    @Test
    void orderAffectsVisualPosition() {
        FlexBox box = new FlexBox();
        Region a = createBox(50, 30);
        Region b = createBox(50, 30);
        Region c = createBox(50, 30);
        FlexBox.setOrder(a, 3);
        FlexBox.setOrder(b, 1);
        FlexBox.setOrder(c, 2);
        box.getChildren().addAll(a, b, c);

        layoutAt(box, 400, 200);

        // Visual order: b(1), c(2), a(3)
        assertTrue(b.getLayoutX() < c.getLayoutX(), "b (order=1) should be before c (order=2)");
        assertTrue(c.getLayoutX() < a.getLayoutX(), "c (order=2) should be before a (order=3)");
    }

    @Test
    void equalOrderPreservesInsertionOrder() {
        FlexBox box = new FlexBox();
        Region a = createBox(50, 30);
        Region b = createBox(60, 30);
        Region c = createBox(70, 30);
        // all default order = 0
        box.getChildren().addAll(a, b, c);

        layoutAt(box, 400, 200);

        assertEquals(0, a.getLayoutX(), 0.5);
        assertEquals(50, b.getLayoutX(), 0.5);
        assertEquals(110, c.getLayoutX(), 0.5);
    }

    @Test
    void negativeOrderComesFirst() {
        FlexBox box = new FlexBox();
        Region a = createBox(50, 30);
        Region b = createBox(50, 30);
        FlexBox.setOrder(b, -1);
        box.getChildren().addAll(a, b);

        layoutAt(box, 400, 200);

        // b has order -1 → visually first
        assertTrue(b.getLayoutX() < a.getLayoutX(),
                "b (order=-1) should appear before a (order=0)");
    }

    @Test
    void orderWithReverse() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.ROW_REVERSE);
        Region a = createBox(50, 30);
        Region b = createBox(50, 30);
        Region c = createBox(50, 30);
        FlexBox.setOrder(a, 1);
        FlexBox.setOrder(b, 2);
        FlexBox.setOrder(c, 3);
        box.getChildren().addAll(a, b, c);

        layoutAt(box, 400, 200);

        // Sorted by order: a, b, c. Then reversed: c is leftmost, a is rightmost
        assertTrue(c.getLayoutX() < b.getLayoutX());
        assertTrue(b.getLayoutX() < a.getLayoutX());
    }
}
