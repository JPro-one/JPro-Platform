package one.jpro.platform.flexbox;

import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for separate row-gap and column-gap properties.
 */
class FlexBoxRowColumnGapTest extends FlexBoxTestBase {

    @Test
    void setGapSetsBoth() {
        FlexBox box = new FlexBox();
        box.setGap(10);
        assertEquals(10, box.getRowGap(), 0.01);
        assertEquals(10, box.getColumnGap(), 0.01);
    }

    @Test
    void separateRowAndColumnGapInRowDirection() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.ROW);
        box.setWrap(FlexWrap.WRAP);
        box.setColumnGap(20);  // between items on main axis
        box.setRowGap(10);     // between lines on cross axis
        box.setAlignContent(FlexAlignContent.FLEX_START);

        // 3 items of 60px in a 140px container → 2 fit per line (60+20+60=140), 1 wraps
        Region r1 = createBox(60, 30);
        Region r2 = createBox(60, 30);
        Region r3 = createBox(60, 30);
        box.getChildren().addAll(r1, r2, r3);

        layoutAt(box, 140, 200);

        // Line 1: r1 at x=0, r2 at x=80 (60+20 column-gap)
        assertEquals(0, r1.getLayoutX(), 0.5);
        assertEquals(80, r2.getLayoutX(), 0.5);
        // Line 2: r3 at y=40 (30 height + 10 row-gap)
        assertEquals(40, r3.getLayoutY(), 0.5);
    }

    @Test
    void columnGapUsedOnMainAxisInRowLayout() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.ROW);
        box.setColumnGap(15);
        box.setRowGap(100); // should not affect main axis spacing

        Region r1 = createBox(50, 30);
        Region r2 = createBox(50, 30);
        box.getChildren().addAll(r1, r2);

        layoutAt(box, 300, 100);

        assertEquals(0, r1.getLayoutX(), 0.5);
        assertEquals(65, r2.getLayoutX(), 0.5); // 50 + 15 column-gap
    }

    @Test
    void rowGapUsedOnMainAxisInColumnLayout() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.COLUMN);
        box.setRowGap(15);
        box.setColumnGap(100); // should not affect main axis spacing

        Region r1 = createBox(30, 50);
        Region r2 = createBox(30, 50);
        box.getChildren().addAll(r1, r2);

        layoutAt(box, 100, 300);

        assertEquals(0, r1.getLayoutY(), 0.5);
        assertEquals(65, r2.getLayoutY(), 0.5); // 50 + 15 row-gap
    }

    @Test
    void columnGapUsedOnCrossAxisInColumnLayout() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.COLUMN);
        box.setWrap(FlexWrap.WRAP);
        box.setRowGap(5);      // main axis gap
        box.setColumnGap(20);  // cross axis gap between wrapped columns
        box.setAlignContent(FlexAlignContent.FLEX_START);

        // 3 items of 50px tall in 110px container → 2 fit (50+5+50=105), 1 wraps
        Region r1 = createBox(30, 50);
        Region r2 = createBox(30, 50);
        Region r3 = createBox(30, 50);
        box.getChildren().addAll(r1, r2, r3);

        layoutAt(box, 200, 110);

        // Column 1: r1 at y=0, r2 at y=55 (50+5 row-gap)
        assertEquals(0, r1.getLayoutY(), 0.5);
        assertEquals(55, r2.getLayoutY(), 0.5);
        // Column 2: r3 at x=50 (30 width + 20 column-gap)
        assertEquals(50, r3.getLayoutX(), 0.5);
    }

    @Test
    void defaultGapIsZero() {
        FlexBox box = new FlexBox();
        assertEquals(0, box.getRowGap(), 0.01);
        assertEquals(0, box.getColumnGap(), 0.01);
    }
}
