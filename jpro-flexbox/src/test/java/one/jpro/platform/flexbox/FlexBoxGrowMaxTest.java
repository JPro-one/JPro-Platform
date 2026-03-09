package one.jpro.platform.flexbox;

import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that grow respects max-size constraints and redistributes excess.
 */
class FlexBoxGrowMaxTest extends FlexBoxTestBase {

    @Test
    void growRespectsMaxWidth() {
        FlexBox box = new FlexBox();
        Region r1 = createBox(50, 30);
        r1.setMaxWidth(80); // can only grow to 80
        FlexBox.setGrow(r1, 1);
        Region r2 = createBox(50, 30);
        FlexBox.setGrow(r2, 1);
        box.getChildren().addAll(r1, r2);

        // 300 wide, 2 items at 50px each, 200px free space, equal grow
        // r1 would get 100 extra → 150, but max is 80, so frozen at 80 (used 30 of free space)
        // remaining 170 redistributed to r2 → 50 + 170 = 220
        layoutAt(box, 300, 100);

        assertEquals(80, r1.getLayoutBounds().getWidth(), 0.5);
        assertEquals(220, r2.getLayoutBounds().getWidth(), 0.5);
    }

    @Test
    void growMaxFreezeRedistributes() {
        FlexBox box = new FlexBox();
        Region r1 = createBox(50, 30);
        r1.setMaxWidth(60);
        FlexBox.setGrow(r1, 1);
        Region r2 = createBox(50, 30);
        r2.setMaxWidth(70);
        FlexBox.setGrow(r2, 1);
        Region r3 = createBox(50, 30);
        FlexBox.setGrow(r3, 1);
        box.getChildren().addAll(r1, r2, r3);

        // 300 wide, 3 items at 50 each = 150 basis, 150 free
        // First pass: each gets +50 → 100. r1 clamped to 60 (frozen), r2 clamped to 70 (frozen)
        // r1 consumed 10, r2 consumed 20, remaining = 150 - 10 - 20 = 120 for r3
        // r3 = 50 + 120 = 170
        layoutAt(box, 300, 100);

        assertEquals(60, r1.getLayoutBounds().getWidth(), 0.5);
        assertEquals(70, r2.getLayoutBounds().getWidth(), 0.5);
        assertEquals(170, r3.getLayoutBounds().getWidth(), 0.5);
    }

    @Test
    void growMaxInColumnDirection() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.COLUMN);
        Region r1 = createBox(30, 50);
        r1.setMaxHeight(80);
        FlexBox.setGrow(r1, 1);
        Region r2 = createBox(30, 50);
        FlexBox.setGrow(r2, 1);
        box.getChildren().addAll(r1, r2);

        // 300 tall, 2 items at 50px each, 200px free, r1 frozen at 80 (used 30)
        // r2 gets 50 + 170 = 220
        layoutAt(box, 100, 300);

        assertEquals(80, r1.getLayoutBounds().getHeight(), 0.5);
        assertEquals(220, r2.getLayoutBounds().getHeight(), 0.5);
    }

    @Test
    void growWithNoMaxIsUnbounded() {
        FlexBox box = new FlexBox();
        Region r1 = createBox(50, 30);
        FlexBox.setGrow(r1, 1);
        Region r2 = createBox(50, 30);
        FlexBox.setGrow(r2, 1);
        box.getChildren().addAll(r1, r2);

        layoutAt(box, 300, 100);

        // No max constraints → each gets equal share: 150
        assertEquals(150, r1.getLayoutBounds().getWidth(), 0.5);
        assertEquals(150, r2.getLayoutBounds().getWidth(), 0.5);
    }
}
