package one.jpro.platform.flexbox;

import javafx.geometry.Insets;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for child margin handling.
 */
class FlexBoxMarginTest extends FlexBoxTestBase {

    @Test
    void defaultMarginIsEmpty() {
        Region r = createBox(50, 30);
        assertEquals(Insets.EMPTY, FlexBox.getMargin(r));
    }

    @Test
    void marginOffsetsPositionInRow() {
        FlexBox box = new FlexBox();
        Region r1 = createBox(50, 30);
        FlexBox.setMargin(r1, new Insets(10, 5, 10, 15)); // top, right, bottom, left
        box.getChildren().add(r1);

        layoutAt(box, 300, 100);

        // In row: left margin shifts x, top margin shifts y
        assertEquals(15, r1.getLayoutX(), 0.5);  // margin-left
        assertEquals(10, r1.getLayoutY(), 0.5);   // margin-top (cross axis)
        assertEquals(50, r1.getLayoutBounds().getWidth(), 0.5);
        // Cross size should be: available (100) - top (10) - bottom (10) = 80 (stretch)
        assertEquals(80, r1.getLayoutBounds().getHeight(), 0.5);
    }

    @Test
    void marginReducesFreeSpaceForGrow() {
        FlexBox box = new FlexBox();
        Region r1 = createBox(50, 30);
        FlexBox.setGrow(r1, 1);
        FlexBox.setMargin(r1, new Insets(0, 10, 0, 10)); // 20px horizontal margin
        box.getChildren().add(r1);

        layoutAt(box, 300, 100);

        // 300 wide container, 50 basis, 20 margin = 230 free space → grows to 280
        assertEquals(280, r1.getLayoutBounds().getWidth(), 0.5);
        assertEquals(10, r1.getLayoutX(), 0.5); // offset by left margin
    }

    @Test
    void marginBetweenItems() {
        FlexBox box = new FlexBox();
        Region r1 = createBox(50, 30);
        FlexBox.setMargin(r1, new Insets(0, 20, 0, 0)); // right margin 20
        Region r2 = createBox(50, 30);
        FlexBox.setMargin(r2, new Insets(0, 0, 0, 10)); // left margin 10
        box.getChildren().addAll(r1, r2);

        layoutAt(box, 300, 100);

        assertEquals(0, r1.getLayoutX(), 0.5);
        // r2 starts at: r1.width(50) + r1.marginRight(20) + r2.marginLeft(10) = 80
        assertEquals(80, r2.getLayoutX(), 0.5);
    }

    @Test
    void marginAffectsWrapBreaking() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP);
        box.setAlignContent(FlexAlignContent.FLEX_START);

        // 2 items of 40px with 15px margin each side = 70px each outer
        // Container is 120px → first item fits (70), second doesn't (70+70=140 > 120) → wraps
        Region r1 = createBox(40, 30);
        FlexBox.setMargin(r1, new Insets(0, 15, 0, 15));
        Region r2 = createBox(40, 30);
        FlexBox.setMargin(r2, new Insets(0, 15, 0, 15));
        box.getChildren().addAll(r1, r2);

        layoutAt(box, 120, 200);

        // Items should be on different lines
        assertEquals(0, r1.getLayoutY(), 0.5);
        assertTrue(r2.getLayoutY() > 0, "r2 should wrap to next line");
    }

    @Test
    void marginInColumnDirection() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.COLUMN);
        Region r1 = createBox(30, 50);
        FlexBox.setMargin(r1, new Insets(10, 5, 15, 5)); // top=10, right=5, bottom=15, left=5
        box.getChildren().add(r1);

        layoutAt(box, 100, 300);

        // In column: top margin shifts y (main), left margin shifts x (cross)
        assertEquals(5, r1.getLayoutX(), 0.5);   // margin-left (cross)
        assertEquals(10, r1.getLayoutY(), 0.5);   // margin-top (main)
    }

    @Test
    void marginAffectsPrefWidth() {
        FlexBox box = new FlexBox();
        Region r1 = createBox(50, 30);
        FlexBox.setMargin(r1, new Insets(0, 10, 0, 10));
        Region r2 = createBox(50, 30);
        box.getChildren().addAll(r1, r2);

        // prefWidth should include margins: 50+20 + 50 = 120
        assertEquals(120, box.prefWidth(-1), 0.5);
    }

    @Test
    void marginAffectsPrefHeight() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.COLUMN);
        Region r1 = createBox(30, 50);
        FlexBox.setMargin(r1, new Insets(10, 0, 10, 0));
        Region r2 = createBox(30, 50);
        box.getChildren().addAll(r1, r2);

        // prefHeight in column: 50+20 + 50 = 120
        assertEquals(120, box.prefHeight(-1), 0.5);
    }

    @Test
    void clearMargin() {
        Region r = createBox(50, 30);
        FlexBox.setMargin(r, new Insets(10));
        assertEquals(new Insets(10), FlexBox.getMargin(r));

        FlexBox.setMargin(r, null);
        assertEquals(Insets.EMPTY, FlexBox.getMargin(r));
    }
}
