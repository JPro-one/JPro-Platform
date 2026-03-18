package one.jpro.platform.flexbox;

import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import javafx.geometry.Orientation;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that FlexBox computes correct prefWidth/prefHeight,
 * especially when wrapping causes multiple rows or columns.
 */
class FlexBoxPrefSizeTest extends FlexBoxTestBase {

    // ── Case 1: prefHeight(width) should reflect wrapping into multiple rows ──

    @Test
    void prefHeight_singleRow_noWrap() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP);
        Region a = createBox(100, 40);
        Region b = createBox(100, 50);
        box.getChildren().addAll(a, b);

        // Width 300 fits both items (100+100=200 <= 300), so single row
        // prefHeight should be the tallest child = 50
        double ph = box.prefHeight(300);
        assertEquals(50, ph, 0.5, "Single row: prefHeight should be max child height");
    }

    @Test
    void prefHeight_twoRows_whenWrapping() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP);
        Region a = createBox(100, 40);
        Region b = createBox(100, 50);
        Region c = createBox(100, 30);
        box.getChildren().addAll(a, b, c);

        // Width 250: a+b = 200 fit on row 1 (max height 50), c on row 2 (height 30)
        // prefHeight should be 50 + 30 = 80
        double ph = box.prefHeight(250);
        assertEquals(80, ph, 0.5,
                "Two rows: prefHeight should be sum of row heights (50 + 30)");
    }

    @Test
    void prefHeight_threeRows_whenWrapping() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP);
        Region a = createBox(100, 40);
        Region b = createBox(100, 50);
        Region c = createBox(100, 30);
        Region d = createBox(100, 60);
        box.getChildren().addAll(a, b, c, d);

        // Width 150: each item gets its own row (100 <= 150, but 100+100=200 > 150)
        // Row 1: a (40), Row 2: b (50), Row 3: c (30), Row 4: d (60)
        // prefHeight = 40 + 50 + 30 + 60 = 180
        double ph = box.prefHeight(150);
        assertEquals(180, ph, 0.5,
                "Four rows: prefHeight should be sum of all row heights");
    }

    @Test
    void prefHeight_twoRows_withRowGap() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP);
        box.setRowGap(10);
        Region a = createBox(100, 40);
        Region b = createBox(100, 50);
        Region c = createBox(100, 30);
        box.getChildren().addAll(a, b, c);

        // Width 250: row 1 (a, b) height 50, row 2 (c) height 30
        // prefHeight = 50 + 10 (gap) + 30 = 90
        double ph = box.prefHeight(250);
        assertEquals(90, ph, 0.5,
                "Two rows with gap: prefHeight should include row gap");
    }

    @Test
    void prefHeight_wideEnough_allFitOneRow() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP);
        Region a = createBox(100, 40);
        Region b = createBox(100, 50);
        Region c = createBox(100, 30);
        box.getChildren().addAll(a, b, c);

        // Width 500: all fit on one row (300 <= 500)
        // prefHeight should be max height = 50
        double ph = box.prefHeight(500);
        assertEquals(50, ph, 0.5,
                "All fit one row: prefHeight should be max child height");
    }

    @Test
    void prefHeight_narrowWidth_forcesMoreWrapping() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP);
        Region a = createBox(150, 40);
        Region b = createBox(150, 50);
        box.getChildren().addAll(a, b);

        // Width 200: 150+150=300 > 200, so each on its own row
        // prefHeight = 40 + 50 = 90
        double phNarrow = box.prefHeight(200);
        assertEquals(90, phNarrow, 0.5,
                "Narrow: items should wrap to separate rows");

        // Width 400: 150+150=300 <= 400, single row
        // prefHeight = 50
        double phWide = box.prefHeight(400);
        assertEquals(50, phWide, 0.5,
                "Wide: items should fit on one row");

        // The narrow case must report more height than the wide case
        assertTrue(phNarrow > phWide,
                "prefHeight should be larger when width is narrower and causes wrapping");
    }

    @Test
    void prefHeight_withColumnGap_affectsWrapping() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP);
        box.setColumnGap(20);
        Region a = createBox(100, 40);
        Region b = createBox(100, 50);
        Region c = createBox(100, 30);
        box.getChildren().addAll(a, b, c);

        // Without gap: 100+100+100 = 300 would fit in 300
        // With column gap: 100+20+100+20+100 = 340 > 300
        // So at width 300: row 1 (a, b) = 100+20+100=220, row 2 (c)
        // prefHeight = 50 + 30 = 80
        double ph = box.prefHeight(300);
        assertEquals(80, ph, 0.5,
                "Column gap should be considered when computing wrap breaks");
    }

    // ── Case 1b: prefWidth(height) should reflect wrapping into multiple columns ──

    @Test
    void prefWidth_twoColumns_whenWrappingInColumnDirection() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.COLUMN);
        box.setWrap(FlexWrap.WRAP);
        Region a = createBox(40, 100);
        Region b = createBox(50, 100);
        Region c = createBox(30, 100);
        box.getChildren().addAll(a, b, c);

        // Height 250: a+b = 200 fit in column 1 (max width 50), c in column 2 (width 30)
        // prefWidth = 50 + 30 = 80
        double pw = box.prefWidth(250);
        assertEquals(80, pw, 0.5,
                "Two columns: prefWidth should be sum of column widths (50 + 30)");
    }

    @Test
    void prefWidth_narrowHeight_forcesMoreWrappingInColumns() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.COLUMN);
        box.setWrap(FlexWrap.WRAP);
        Region a = createBox(40, 150);
        Region b = createBox(50, 150);
        box.getChildren().addAll(a, b);

        // Height 200: 150+150=300 > 200, each in own column
        // prefWidth = 40 + 50 = 90
        double pwNarrow = box.prefWidth(200);
        assertEquals(90, pwNarrow, 0.5,
                "Narrow height: items should wrap to separate columns");

        // Height 400: 150+150=300 <= 400, single column
        // prefWidth = max(40, 50) = 50
        double pwWide = box.prefWidth(400);
        assertEquals(50, pwWide, 0.5,
                "Tall: items should fit in one column");

        assertTrue(pwNarrow > pwWide,
                "prefWidth should be larger when height is shorter and causes column wrapping");
    }

    @Test
    void prefWidth_twoColumns_withColumnGap() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.COLUMN);
        box.setWrap(FlexWrap.WRAP);
        box.setColumnGap(10);
        Region a = createBox(40, 100);
        Region b = createBox(50, 100);
        Region c = createBox(30, 100);
        box.getChildren().addAll(a, b, c);

        // Height 250: column 1 (a, b) max width 50, column 2 (c) width 30
        // prefWidth = 50 + 10 (gap) + 30 = 90
        double pw = box.prefWidth(250);
        assertEquals(90, pw, 0.5,
                "Two columns with gap: prefWidth should include column gap");
    }

    // ── Case 2: prefHeight/prefWidth for single or few elements ──

    @Test
    void prefHeight_singleElement() {
        FlexBox box = new FlexBox();
        Region a = createBox(80, 60);
        box.getChildren().add(a);

        // prefHeight should simply be the element's height
        double ph = box.prefHeight(-1);
        assertEquals(60, ph, 0.5,
                "Single element: prefHeight should be the element's prefHeight");
    }

    @Test
    void prefWidth_singleElement() {
        FlexBox box = new FlexBox();
        Region a = createBox(80, 60);
        box.getChildren().add(a);

        // prefWidth should simply be the element's width
        double pw = box.prefWidth(-1);
        assertEquals(80, pw, 0.5,
                "Single element: prefWidth should be the element's prefWidth");
    }

    @Test
    void prefHeight_singleElement_columnDirection() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.COLUMN);
        Region a = createBox(80, 60);
        box.getChildren().add(a);

        double ph = box.prefHeight(-1);
        assertEquals(60, ph, 0.5,
                "Single element column: prefHeight should be the element's prefHeight");
    }

    @Test
    void prefWidth_singleElement_columnDirection() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.COLUMN);
        Region a = createBox(80, 60);
        box.getChildren().add(a);

        double pw = box.prefWidth(-1);
        assertEquals(80, pw, 0.5,
                "Single element column: prefWidth should be the element's prefWidth");
    }

    @Test
    void prefHeight_doesNotExceedSingleRowHeight_forRowDirection() {
        FlexBox box = new FlexBox();
        // No wrap — row direction, two items that fit
        Region a = createBox(100, 40);
        Region b = createBox(100, 60);
        box.getChildren().addAll(a, b);

        // In row direction, prefHeight is max child height = 60, not sum
        double ph = box.prefHeight(-1);
        assertEquals(60, ph, 0.5,
                "Row direction: prefHeight should be max height, not sum");
    }

    @Test
    void prefWidth_doesNotExceedSingleColumnWidth_forColumnDirection() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.COLUMN);
        Region a = createBox(40, 100);
        Region b = createBox(60, 100);
        box.getChildren().addAll(a, b);

        // In column direction, prefWidth is max child width = 60, not sum
        double pw = box.prefWidth(-1);
        assertEquals(60, pw, 0.5,
                "Column direction: prefWidth should be max width, not sum");
    }

    // ── Content bias ──

    @Test
    void contentBias_rowWrap_isHorizontal() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP);
        assertEquals(Orientation.HORIZONTAL, box.getContentBias(),
                "Row + wrap: content bias should be HORIZONTAL (height depends on width)");
    }

    @Test
    void contentBias_columnWrap_isVertical() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.COLUMN);
        box.setWrap(FlexWrap.WRAP);
        assertEquals(Orientation.VERTICAL, box.getContentBias(),
                "Column + wrap: content bias should be VERTICAL (width depends on height)");
    }

    @Test
    void contentBias_noWrap_isNull() {
        FlexBox box = new FlexBox();
        assertNull(box.getContentBias(),
                "No wrap: content bias should be null");
    }

    // ── minHeight with wrapping ──

    @Test
    void minHeight_twoRows_whenWrapping() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP);
        Region a = createBox(100, 40);
        Region b = createBox(100, 50);
        Region c = createBox(100, 30);
        box.getChildren().addAll(a, b, c);

        // Width 250: row 1 (a, b) max height 50, row 2 (c) height 30
        double mh = box.minHeight(250);
        assertEquals(80, mh, 0.5,
                "Two rows: minHeight should be sum of row heights (50 + 30)");
    }

    @Test
    void minHeight_withRowGap_whenWrapping() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP);
        box.setRowGap(10);
        Region a = createBox(100, 40);
        Region b = createBox(100, 50);
        Region c = createBox(100, 30);
        box.getChildren().addAll(a, b, c);

        // Width 250: row 1 (a, b) height 50, gap 10, row 2 (c) height 30
        double mh = box.minHeight(250);
        assertEquals(90, mh, 0.5,
                "Two rows with gap: minHeight should include row gap");
    }

    // ── minWidth with wrapping in column direction ──

    @Test
    void minWidth_twoColumns_whenWrappingInColumnDirection() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.COLUMN);
        box.setWrap(FlexWrap.WRAP);
        Region a = createBox(40, 100);
        Region b = createBox(50, 100);
        Region c = createBox(30, 100);
        box.getChildren().addAll(a, b, c);

        // Height 250: column 1 (a, b) max width 50, column 2 (c) width 30
        double mw = box.minWidth(250);
        assertEquals(80, mw, 0.5,
                "Two columns: minWidth should be sum of column widths (50 + 30)");
    }

    @Test
    void minWidth_withColumnGap_whenWrappingInColumnDirection() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.COLUMN);
        box.setWrap(FlexWrap.WRAP);
        box.setColumnGap(10);
        Region a = createBox(40, 100);
        Region b = createBox(50, 100);
        Region c = createBox(30, 100);
        box.getChildren().addAll(a, b, c);

        // Height 250: column 1 (a, b) max width 50, gap 10, column 2 (c) width 30
        double mw = box.minWidth(250);
        assertEquals(90, mw, 0.5,
                "Two columns with gap: minWidth should include column gap");
    }
}
