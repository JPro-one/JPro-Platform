package one.jpro.platform.flexbox;

import javafx.geometry.Orientation;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Children whose height depends on their width (wrapped text, cards with text) must be measured at the width they get,
 * otherwise a wrapping FlexBox reports less height than its laid-out lines need.
 */
class FlexBoxWrapHeightTest extends FlexBoxTestBase {

    /** 100 wide by default; its area is fixed, so it gets taller when narrower (like wrapped text). */
    private static Region widthDependent() {
        return widthDependent(100);
    }

    private static Region widthDependent(double prefWidth) {
        Region r = new Region() {
            @Override public Orientation getContentBias() { return Orientation.HORIZONTAL; }
            @Override protected double computePrefWidth(double height) { return prefWidth; }
            @Override protected double computePrefHeight(double width) { return width < 0 ? 20 : 4000 / Math.max(width, 1); }
        };
        r.setMinWidth(10);
        r.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return r;
    }

    /** Laid out at its reported height, every child ends inside the box. */
    private static void assertLinesFit(FlexBox box, double width) {
        double h = box.prefHeight(width);
        assertTrue(Double.isFinite(h), "finite height: " + h);
        layoutAt(box, width, h);
        for (var n : box.getChildren()) {
            Region r = (Region) n;
            assertTrue(r.getLayoutY() + r.getHeight() <= h + 0.5, "child ends at " + (r.getLayoutY() + r.getHeight()) + " but the box reported " + h);
        }
    }

    @Test
    void prefHeightOfWrappedLinesUsesTheChildrensWidths() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP);
        box.getChildren().addAll(widthDependent(), widthDependent());

        // 150 wide: one child per line, each 100 wide and therefore 40 high
        assertEquals(80, box.prefHeight(150), 0.5);
        assertEquals(80, box.minHeight(150), 0.5);

        layoutAt(box, 150, box.prefHeight(150));
        Region second = (Region) box.getChildren().get(1);
        assertEquals(40, second.getHeight(), 0.5);
        assertTrue(second.getLayoutY() + second.getHeight() <= box.prefHeight(150) + 0.5, "laid out at its reported height, the lines fit");
    }

    @Test
    void growingChildrenWithZeroBasisAreMeasuredAtTheirGrownWidth() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP);
        Region a = widthDependent(), b = widthDependent();
        for (Region r : new Region[]{a, b}) { FlexBox.setBasis(r, 0); FlexBox.setGrow(r, 1); }
        box.getChildren().addAll(a, b);
        // both on one 200-wide line, grown to 100 each: 40 high
        assertEquals(40, box.prefHeight(200), 0.5);
        assertLinesFit(box, 200);
    }

    @Test
    void childrenShrunkToTheLineAreMeasuredAtTheShrunkWidth() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP);
        box.getChildren().add(widthDependent(300));
        // alone on a 150-wide line, shrunk to 150: 4000 / 150
        assertEquals(4000.0 / 150, box.prefHeight(150), 0.5);
        assertLinesFit(box, 150);
    }

    @Test
    void linesFollowTheOrderProperty() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP);
        Region a = createBox(100, 10), big = createBox(150, 50), b = createBox(100, 10);
        FlexBox.setOrder(big, 2);
        box.getChildren().addAll(a, big, b);
        // by order: [a, b] then [big] = 10 + 50; in child order it would be three lines [a] [big] [b] = 70
        assertEquals(60, box.prefHeight(210), 0.5);
        assertLinesFit(box, 210);
    }

    @Test
    void aChangedChildIsMeasuredAgain() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP);
        Region a = createBox(100, 40);
        box.getChildren().add(a);
        assertEquals(40, box.prefHeight(150), 0.5);
        a.setPrefHeight(70);
        assertEquals(70, box.prefHeight(150), 0.5);
        box.getChildren().add(createBox(100, 30));
        assertEquals(100, box.prefHeight(150), 0.5, "the new child wraps onto a second line");
        box.setRowGap(5);
        assertEquals(105, box.prefHeight(150), 0.5);
    }

    @Test
    void columnDirectionMeasuresWidthAtTheResolvedHeight() {
        Region r = new Region() {
            @Override public Orientation getContentBias() { return Orientation.VERTICAL; }
            @Override protected double computePrefHeight(double width) { return 100; }
            @Override protected double computePrefWidth(double height) { return height < 0 ? 20 : 4000 / height; }
        };
        r.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        Region r2 = new Region() {
            @Override public Orientation getContentBias() { return Orientation.VERTICAL; }
            @Override protected double computePrefHeight(double width) { return 100; }
            @Override protected double computePrefWidth(double height) { return height < 0 ? 20 : 4000 / height; }
        };
        r2.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.COLUMN);
        box.setWrap(FlexWrap.WRAP);
        box.getChildren().addAll(r, r2);
        // 150 high: one child per column, each 100 high and therefore 40 wide
        assertEquals(80, box.prefWidth(150), 0.5);
        assertEquals(80, box.minWidth(150), 0.5);
    }

    @Test
    void gapsAndMarginsCountLikeInTheLayout() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP);
        box.setColumnGap(10);
        box.setRowGap(7);
        Region a = widthDependent(), b = widthDependent();
        FlexBox.setMargin(a, new javafx.geometry.Insets(3, 5, 3, 5));
        FlexBox.setMargin(b, new javafx.geometry.Insets(3, 5, 3, 5));
        box.getChildren().addAll(a, b);
        // 210 wide: 110 + 10 + 110 = 230 does not fit, so two lines of 40 + 6 margin, 7 apart
        assertEquals(46 + 7 + 46, box.prefHeight(210), 0.5);
        assertLinesFit(box, 210);
    }
}
