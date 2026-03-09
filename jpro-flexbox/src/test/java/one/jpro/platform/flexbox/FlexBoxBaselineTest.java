package one.jpro.platform.flexbox;

import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for baseline alignment.
 */
class FlexBoxBaselineTest extends FlexBoxTestBase {

    /** A Region with a custom baseline offset for testing. */
    private static Region createBoxWithBaseline(double prefWidth, double prefHeight, double baseline) {
        Region r = new Region() {
            @Override
            public double getBaselineOffset() {
                return baseline;
            }
        };
        r.setPrefSize(prefWidth, prefHeight);
        r.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        r.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return r;
    }

    @Test
    void baselineAlignsSameOffset() {
        FlexBox box = new FlexBox();
        box.setAlignItems(FlexAlignItems.BASELINE);

        // Two items with same baseline at 20px from top
        Region r1 = createBoxWithBaseline(50, 30, 20);
        Region r2 = createBoxWithBaseline(50, 40, 20);
        box.getChildren().addAll(r1, r2);

        layoutAt(box, 200, 100);

        // Both baselines at 20px → same y position since baseline offset is equal
        assertEquals(r1.getLayoutY(), r2.getLayoutY(), 0.5);
    }

    @Test
    void baselineAlignsDifferentOffsets() {
        FlexBox box = new FlexBox();
        box.setAlignItems(FlexAlignItems.BASELINE);

        // r1: 30px tall, baseline at 20px from top
        // r2: 40px tall, baseline at 30px from top
        Region r1 = createBoxWithBaseline(50, 30, 20);
        Region r2 = createBoxWithBaseline(50, 40, 30);
        box.getChildren().addAll(r1, r2);

        layoutAt(box, 200, 100);

        // Line baseline = max(20, 30) = 30
        // r1 positioned at: 30 - 20 = 10
        // r2 positioned at: 30 - 30 = 0
        assertEquals(10, r1.getLayoutY(), 0.5);
        assertEquals(0, r2.getLayoutY(), 0.5);
    }

    @Test
    void baselineWithThreeItems() {
        FlexBox box = new FlexBox();
        box.setAlignItems(FlexAlignItems.BASELINE);

        Region r1 = createBoxWithBaseline(40, 20, 15);  // baseline at 15
        Region r2 = createBoxWithBaseline(40, 30, 10);  // baseline at 10
        Region r3 = createBoxWithBaseline(40, 25, 20);  // baseline at 20
        box.getChildren().addAll(r1, r2, r3);

        layoutAt(box, 200, 100);

        // Line baseline = max(15, 10, 20) = 20
        // r1: 20 - 15 = 5
        // r2: 20 - 10 = 10
        // r3: 20 - 20 = 0
        assertEquals(5, r1.getLayoutY(), 0.5);
        assertEquals(10, r2.getLayoutY(), 0.5);
        assertEquals(0, r3.getLayoutY(), 0.5);
    }

    @Test
    void baselineMixedWithOtherAlignments() {
        FlexBox box = new FlexBox();
        box.setAlignItems(FlexAlignItems.BASELINE);

        Region r1 = createBoxWithBaseline(50, 30, 20);
        Region r2 = createBoxWithBaseline(50, 40, 25);
        // r3 uses align-self: center instead of baseline
        Region r3 = createBox(50, 20);
        FlexBox.setAlignSelf(r3, FlexAlignItems.CENTER);
        box.getChildren().addAll(r1, r2, r3);

        layoutAt(box, 300, 100);

        // Line baseline = max(20, 25) = 25 (only baseline-aligned items)
        // r1: 25 - 20 = 5
        // r2: 25 - 25 = 0
        // r3: centered in 100px line → (100 - 20) / 2 = 40
        assertEquals(5, r1.getLayoutY(), 0.5);
        assertEquals(0, r2.getLayoutY(), 0.5);
        assertEquals(40, r3.getLayoutY(), 0.5);
    }
}
