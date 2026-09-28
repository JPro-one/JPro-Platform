package one.jpro.platform.cssgrid;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CssGridPrefSizeTest extends CssGridTestBase {

    @Test
    void prefWidthSumsFixedTracksGapsAndInsets() {
        CssGrid grid = new CssGrid();
        grid.setTemplateColumns("100 200");
        grid.setColumnGap(10);
        grid.setPadding(new Insets(0, 5, 0, 5));

        assertEquals(320, grid.prefWidth(-1), 0.5);
        assertEquals(320, grid.minWidth(-1), 0.5);
    }

    @Test
    void prefWidthOfAutoTracksIsPreferredContent() {
        Region a = createBox(20, 80, 10, 10), b = createBox(30, 120, 10, 10);
        CssGrid grid = new CssGrid(a, b);
        grid.setTemplateColumns("auto auto");

        assertEquals(200, grid.prefWidth(-1), 0.5);
        assertEquals(50, grid.minWidth(-1), 0.5);
    }

    @Test
    void prefWidthOfFlexibleTracksKeepsThemEqual() {
        Region a = createBox(20, 80, 10, 10), b = createBox(30, 120, 10, 10);
        CssGrid grid = new CssGrid(a, b);
        grid.setTemplateColumns("1fr 1fr");

        assertEquals(240, grid.prefWidth(-1), 0.5);
    }

    @Test
    void minWidthDoesNotExpandFlexibleTracks() {
        Region a = createBox(20, 80, 10, 10), b = createBox(30, 120, 10, 10);
        CssGrid grid = new CssGrid(a, b);
        grid.setTemplateColumns("1fr 1fr");

        assertEquals(50, grid.minWidth(-1), 0.5);
    }

    @Test
    void prefWidthOfWeightedFlexibleTracks() {
        Region a = createBox(100, 10), b = createBox(100, 10);
        CssGrid grid = new CssGrid(a, b);
        grid.setTemplateColumns("1fr 2fr");

        // fr = max(100 / 1, 100 / 2) = 100 → 100 + 200
        assertEquals(300, grid.prefWidth(-1), 0.5);
    }

    @Test
    void percentTracksAreAutoWhenWidthIsIndefinite() {
        Region a = createBox(80, 10);
        CssGrid grid = new CssGrid(a);
        grid.setTemplateColumns("50%");

        assertEquals(80, grid.prefWidth(-1), 0.5);
    }

    @Test
    void prefHeightDependsOnWidth() {
        Region a = createBox(60, 30), b = createBox(60, 30), c = createBox(60, 30);
        CssGrid grid = new CssGrid(a, b, c);
        grid.setTemplateColumns("repeat(auto-fill, 100)");
        grid.setRowGap(10);

        assertEquals(Orientation.HORIZONTAL, grid.getContentBias());
        assertEquals(30, grid.prefHeight(300), 0.5);
        assertEquals(70, grid.prefHeight(200), 0.5);
        assertEquals(110, grid.prefHeight(100), 0.5);
        assertEquals(110, grid.prefHeight(-1), 0.5);
    }

    @Test
    void prefHeightUsesItemHeightAtResolvedWidth() {
        Region a = new Region() {
            @Override public Orientation getContentBias() { return Orientation.HORIZONTAL; }
            @Override protected double computePrefHeight(double width) { return width > 0 ? 2000 / width : 100; }
            @Override protected double computeMinHeight(double width) { return computePrefHeight(width); }
        };
        a.setMinWidth(10);
        a.setPrefWidth(20);
        a.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        CssGrid grid = new CssGrid(a);
        grid.setTemplateColumns("1fr");

        assertEquals(10, grid.prefHeight(200), 0.5);
        assertEquals(20, grid.prefHeight(100), 0.5);
    }

    @Test
    void minHeightUsesMinimumContributions() {
        Region a = createBox(10, 10, 20, 80);
        CssGrid grid = new CssGrid(a);
        grid.setTemplateColumns("100");

        assertEquals(80, grid.prefHeight(100), 0.5);
        assertEquals(20, grid.minHeight(100), 0.5);
    }

    @Test
    void prefSizeIncludesExplicitRowsWithoutItems() {
        CssGrid grid = new CssGrid();
        grid.setTemplateRows("40 60");
        grid.setRowGap(5);

        assertEquals(105, grid.prefHeight(-1), 0.5);
    }

    @Test
    void prefSizeOfAreasGrid() {
        Region header = createBox(10, 40), side = createBox(100, 10), main = createBox(200, 10);
        CssGrid.setArea(header, "header");
        CssGrid.setArea(side, "side");
        CssGrid.setArea(main, "main");
        CssGrid grid = new CssGrid(header, side, main);
        grid.setTemplateAreas("header header", "side main");

        assertEquals(300, grid.prefWidth(-1), 0.5);
        assertEquals(50, grid.prefHeight(300), 0.5);
    }
}
