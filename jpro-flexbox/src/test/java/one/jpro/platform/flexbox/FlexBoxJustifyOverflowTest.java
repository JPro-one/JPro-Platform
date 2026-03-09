package one.jpro.platform.flexbox;

import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that justify-content works correctly when items overflow the container.
 */
class FlexBoxJustifyOverflowTest extends FlexBoxTestBase {

    @Test
    void flexEndOverflowsAtStart() {
        FlexBox box = new FlexBox();
        box.setJustifyContent(FlexJustifyContent.FLEX_END);
        Region r1 = createBox(80, 30);
        Region r2 = createBox(80, 30);
        box.getChildren().addAll(r1, r2);

        // Container is 100 wide, items need 160 → overflow of -60
        // flex-end: start at -60, so items overflow to the left
        layoutAt(box, 100, 100);

        assertEquals(-60, r1.getLayoutX(), 0.5);
        assertEquals(20, r2.getLayoutX(), 0.5);
    }

    @Test
    void centerOverflowsEqually() {
        FlexBox box = new FlexBox();
        box.setJustifyContent(FlexJustifyContent.CENTER);
        Region r1 = createBox(80, 30);
        Region r2 = createBox(80, 30);
        box.getChildren().addAll(r1, r2);

        // Container is 100, items need 160, remaining = -60
        // center: start at -30
        layoutAt(box, 100, 100);

        assertEquals(-30, r1.getLayoutX(), 0.5);
        assertEquals(50, r2.getLayoutX(), 0.5);
    }

    @Test
    void spaceBetweenFallsBackToFlexStart() {
        FlexBox box = new FlexBox();
        box.setJustifyContent(FlexJustifyContent.SPACE_BETWEEN);
        Region r1 = createBox(80, 30);
        Region r2 = createBox(80, 30);
        box.getChildren().addAll(r1, r2);

        layoutAt(box, 100, 100);

        // space-between with overflow → falls back to flex-start (pos 0)
        assertEquals(0, r1.getLayoutX(), 0.5);
        assertEquals(80, r2.getLayoutX(), 0.5);
    }

    @Test
    void spaceAroundFallsBackToCenter() {
        FlexBox box = new FlexBox();
        box.setJustifyContent(FlexJustifyContent.SPACE_AROUND);
        Region r1 = createBox(80, 30);
        Region r2 = createBox(80, 30);
        box.getChildren().addAll(r1, r2);

        layoutAt(box, 100, 100);

        // space-around with overflow → falls back to center
        assertEquals(-30, r1.getLayoutX(), 0.5);
        assertEquals(50, r2.getLayoutX(), 0.5);
    }

    @Test
    void spaceEvenlyFallsBackToCenter() {
        FlexBox box = new FlexBox();
        box.setJustifyContent(FlexJustifyContent.SPACE_EVENLY);
        Region r1 = createBox(80, 30);
        Region r2 = createBox(80, 30);
        box.getChildren().addAll(r1, r2);

        layoutAt(box, 100, 100);

        // space-evenly with overflow → falls back to center
        assertEquals(-30, r1.getLayoutX(), 0.5);
        assertEquals(50, r2.getLayoutX(), 0.5);
    }

    @Test
    void flexStartOverflowStaysAtZero() {
        FlexBox box = new FlexBox();
        box.setJustifyContent(FlexJustifyContent.FLEX_START);
        Region r1 = createBox(80, 30);
        Region r2 = createBox(80, 30);
        box.getChildren().addAll(r1, r2);

        layoutAt(box, 100, 100);

        assertEquals(0, r1.getLayoutX(), 0.5);
        assertEquals(80, r2.getLayoutX(), 0.5);
    }
}
