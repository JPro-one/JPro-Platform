package one.jpro.platform.flexbox;

import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlexBoxWrapTest extends FlexBoxTestBase {

    @Test
    void defaultIsNowrap() {
        FlexBox box = new FlexBox();
        assertEquals(FlexWrap.NOWRAP, box.getWrap());
    }

    @Test
    void nowrapKeepsAllOnOneLine() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.NOWRAP);
        Region a = createBox(200, 30);
        Region b = createBox(200, 30);
        Region c = createBox(200, 30);
        box.getChildren().addAll(a, b, c);

        layoutAt(box, 400, 200);

        // All on the same Y, even though they overflow
        assertEquals(a.getLayoutY(), b.getLayoutY(), 0.5);
        assertEquals(b.getLayoutY(), c.getLayoutY(), 0.5);
    }

    @Test
    void wrapBreaksIntoMultipleLines() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP);
        Region a = createBox(150, 40);
        Region b = createBox(150, 40);
        Region c = createBox(150, 40);
        box.getChildren().addAll(a, b, c);

        layoutAt(box, 350, 200);

        // a and b fit on line 1 (150+150=300 <= 350), c on line 2
        assertEquals(a.getLayoutY(), b.getLayoutY(), 0.5);
        assertTrue(c.getLayoutY() > a.getLayoutY(),
                "Third child should wrap to next line");
    }

    @Test
    void wrapReverseReversesLineOrder() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP_REVERSE);
        box.setAlignContent(FlexAlignContent.FLEX_START);
        Region a = createBox(150, 40);
        Region b = createBox(150, 40);
        Region c = createBox(150, 40);
        box.getChildren().addAll(a, b, c);

        layoutAt(box, 350, 200);

        // With WRAP_REVERSE, the first line should be below the second line
        assertTrue(a.getLayoutY() > c.getLayoutY(),
                "With WRAP_REVERSE, first line should be below second line");
    }

    @Test
    void wrapColumnDirection() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.COLUMN);
        box.setWrap(FlexWrap.WRAP);
        box.setAlignContent(FlexAlignContent.FLEX_START);
        Region a = createBox(50, 100);
        Region b = createBox(50, 100);
        Region c = createBox(50, 100);
        box.getChildren().addAll(a, b, c);

        layoutAt(box, 400, 250);

        // a and b fit on column 1 (100+100=200 <= 250), c on column 2
        assertEquals(a.getLayoutX(), b.getLayoutX(), 0.5);
        assertTrue(c.getLayoutX() > a.getLayoutX(),
                "Third child should wrap to next column");
    }

    @Test
    void wrapWithGap() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP);
        box.setGap(10);
        Region a = createBox(150, 40);
        Region b = createBox(150, 40);
        Region c = createBox(150, 40);
        box.getChildren().addAll(a, b, c);

        layoutAt(box, 350, 200);

        // a+b with gap = 150+10+150 = 310 <= 350, fits on line 1
        // c on line 2
        assertEquals(0, a.getLayoutX(), 0.5);
        assertEquals(160, b.getLayoutX(), 0.5);
        assertEquals(0, c.getLayoutX(), 0.5);
        assertTrue(c.getLayoutY() > a.getLayoutY());
    }

    @Test
    void wrapThreeLines() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP);
        box.setAlignContent(FlexAlignContent.FLEX_START);
        Region a = createBox(100, 30);
        Region b = createBox(100, 30);
        Region c = createBox(100, 30);
        Region d = createBox(100, 30);
        Region e = createBox(100, 30);
        box.getChildren().addAll(a, b, c, d, e);

        layoutAt(box, 250, 300);

        // Line 1: a, b (200 <= 250)
        // Line 2: c, d (200 <= 250)
        // Line 3: e
        assertEquals(a.getLayoutY(), b.getLayoutY(), 0.5);
        assertEquals(c.getLayoutY(), d.getLayoutY(), 0.5);
        assertTrue(c.getLayoutY() > a.getLayoutY());
        assertTrue(e.getLayoutY() > c.getLayoutY());
    }
}
