package one.jpro.platform.cssgrid;

import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CssGridTrackSizingTest extends CssGridTestBase {

    private static CssGrid gridWithColumns(String columns, Region... children) {
        CssGrid grid = new CssGrid(children);
        grid.setTemplateColumns(columns);
        grid.setAlignContent(GridContentAlignment.START);
        return grid;
    }

    @Test
    void fixedTracks() {
        Region a = createBox(10, 10), b = createBox(10, 10);
        CssGrid grid = gridWithColumns("100 200", a, b);

        layoutAt(grid, 500, 100);

        assertBounds(a, 0, 0, 100, 10);
        assertBounds(b, 100, 0, 200, 10);
    }

    @Test
    void flexibleTracksShareFreeSpace() {
        Region a = createBox(10, 10), b = createBox(10, 10);
        CssGrid grid = gridWithColumns("1fr 2fr", a, b);

        layoutAt(grid, 300, 100);

        assertBounds(a, 0, 0, 100, 10);
        assertBounds(b, 100, 0, 200, 10);
    }

    @Test
    void flexibleTrackRespectsItemMinimum() {
        Region a = createBox(10, 10), b = createBox(250, 10);
        CssGrid grid = gridWithColumns("1fr 1fr", a, b);

        layoutAt(grid, 300, 100);

        // 1fr is minmax(auto, 1fr): b cannot shrink below 250, so a gets the rest
        assertBounds(a, 0, 0, 50, 10);
        assertBounds(b, 50, 0, 250, 10);
    }

    @Test
    void minmaxZeroFrAllowsShrinkingBelowMinimum() {
        Region a = createBox(10, 10), b = createBox(0, 250, 10, 10);
        CssGrid grid = gridWithColumns("minmax(0, 1fr) minmax(0, 1fr)", a, b);

        layoutAt(grid, 300, 100);

        assertEquals(150, width(a), 0.5);
        assertEquals(150, width(b), 0.5);
        assertEquals(150, b.getLayoutX(), 0.5);
    }

    @Test
    void flexFactorSumBelowOneUsesFractionOfFreeSpace() {
        Region a = createBox(10, 10);
        CssGrid grid = gridWithColumns("0.5fr", a);

        layoutAt(grid, 400, 100);

        assertEquals(200, width(a), 0.5);
    }

    @Test
    void autoTracksStretchToFillWhenNoFlexibleTracks() {
        Region a = createBox(50, 10), b = createBox(100, 10);
        CssGrid grid = gridWithColumns("auto auto", a, b);

        layoutAt(grid, 350, 100);

        // 350 - 150 = 200 free, stretched equally
        assertBounds(a, 0, 0, 150, 10);
        assertBounds(b, 150, 0, 200, 10);
    }

    @Test
    void autoTracksDoNotStretchWithJustifyContentStart() {
        Region a = createBox(50, 10), b = createBox(100, 10);
        CssGrid grid = gridWithColumns("auto auto", a, b);
        grid.setJustifyContent(GridContentAlignment.START);

        layoutAt(grid, 350, 100);

        assertBounds(a, 0, 0, 50, 10);
        assertBounds(b, 50, 0, 100, 10);
    }

    @Test
    void autoTrackTakesPreferredSizeNextToFlexibleTrack() {
        Region a = createBox(80, 10), b = createBox(10, 10);
        CssGrid grid = gridWithColumns("auto 1fr", a, b);

        layoutAt(grid, 300, 100);

        assertBounds(a, 0, 0, 80, 10);
        assertBounds(b, 80, 0, 220, 10);
    }

    @Test
    void autoTracksShrinkTowardsMinimumWhenSpaceIsTight() {
        Region a = createBox(20, 200, 10, 10), b = createBox(20, 200, 10, 10);
        CssGrid grid = gridWithColumns("auto auto", a, b);

        layoutAt(grid, 100, 100);

        assertEquals(50, width(a), 0.5);
        assertEquals(50, width(b), 0.5);
    }

    @Test
    void autoTracksNeverShrinkBelowMinimum() {
        Region a = createBox(60, 200, 10, 10), b = createBox(60, 200, 10, 10);
        CssGrid grid = gridWithColumns("auto auto", a, b);

        layoutAt(grid, 100, 100);

        assertEquals(60, width(a), 0.5);
        assertEquals(60, width(b), 0.5);
        assertEquals(60, b.getLayoutX(), 0.5);
    }

    @Test
    void minmaxClampsTrack() {
        Region a = createBox(10, 10), b = createBox(10, 10);
        CssGrid grid = gridWithColumns("minmax(100, 150) 1fr", a, b);

        layoutAt(grid, 400, 100);
        assertBounds(a, 0, 0, 150, 10);
        assertBounds(b, 150, 0, 250, 10);

        // Tracks are maximized before flexible tracks are expanded: the minmax track gets the free space first
        layoutAt(grid, 120, 100);
        assertEquals(110, width(a), 0.5);
        assertEquals(10, width(b), 0.5);
    }

    @Test
    void percentTracksResolveAgainstContentWidth() {
        Region a = createBox(10, 10), b = createBox(10, 10);
        CssGrid grid = gridWithColumns("25% 1fr", a, b);
        grid.setPadding(new javafx.geometry.Insets(0, 50, 0, 50));

        layoutAt(grid, 500, 100);

        assertBounds(a, 50, 0, 100, 10);
        assertBounds(b, 150, 0, 300, 10);
    }

    @Test
    void minContentAndMaxContentTracks() {
        Region a = createBox(30, 120, 10, 10), b = createBox(30, 120, 10, 10);
        CssGrid grid = gridWithColumns("min-content max-content", a, b);
        grid.setJustifyContent(GridContentAlignment.START);

        layoutAt(grid, 500, 100);

        assertBounds(a, 0, 0, 30, 10);
        assertBounds(b, 30, 0, 120, 10);
    }

    @Test
    void spanningItemDistributesSpaceToAutoTracks() {
        Region a = createBox(10, 10), b = createBox(10, 10), wide = createBox(300, 10);
        CssGrid.setColumn(wide, 1, 3);
        CssGrid grid = gridWithColumns("auto auto", a, b, wide);
        grid.setJustifyContent(GridContentAlignment.START);

        layoutAt(grid, 500, 100);

        assertEquals(150, width(a), 0.5);
        assertEquals(150, width(b), 0.5);
        assertEquals(300, width(wide), 0.5);
    }

    @Test
    void overlappingSpanningItemsUsePlannedIncreases() {
        Region a = createBox(200, 10), b = createBox(200, 10);
        CssGrid.setColumn(a, 1, 3);
        CssGrid.setColumn(b, 2, 4);
        CssGrid grid = gridWithColumns("auto auto auto", a, b);
        grid.setJustifyContent(GridContentAlignment.START);

        layoutAt(grid, 1000, 100);

        // Each item's increase is planned per track and the maximum applied once: 100 / 100 / 100
        assertBounds(a, 0, 0, 200, 10);
        assertBounds(b, 100, 10, 200, 10);
        assertEquals(300, grid.prefWidth(-1), 0.5);
    }

    @Test
    void spanningItemDoesNotGrowFixedTracks() {
        Region wide = createBox(300, 10);
        CssGrid.setColumn(wide, 1, 3);
        CssGrid grid = gridWithColumns("100 100", wide);

        layoutAt(grid, 500, 100);

        // The area is only 200 wide; the item keeps its minimum and overflows
        assertEquals(300, width(wide), 0.5);
    }

    @Test
    void spanningItemAcrossFlexibleTracksRaisesTheirMinimum() {
        Region wide = createBox(300, 10);
        CssGrid.setColumn(wide, 1, 3);
        CssGrid grid = gridWithColumns("1fr 1fr", wide);

        layoutAt(grid, 100, 100);

        assertEquals(300, width(wide), 0.5);
    }

    @Test
    void adjacentTracksShareSnappedEdges() {
        Region a = createBox(10, 10), b = createBox(10, 10), c = createBox(10, 10);
        CssGrid grid = gridWithColumns("1fr 1fr 1fr", a, b, c);

        layoutAt(grid, 301, 100);

        assertEquals(a.getLayoutX() + width(a), b.getLayoutX(), 0.01);
        assertEquals(b.getLayoutX() + width(b), c.getLayoutX(), 0.01);
        assertEquals(301, c.getLayoutX() + width(c), 0.01);
    }

    @Test
    void repeatExpandsTracks() {
        Region a = createBox(10, 10), b = createBox(10, 10), c = createBox(10, 10), d = createBox(10, 10);
        CssGrid grid = gridWithColumns("repeat(2, 50 100)", a, b, c, d);

        layoutAt(grid, 300, 100);

        assertEquals(50, width(a), 0.5);
        assertEquals(100, width(b), 0.5);
        assertEquals(50, width(c), 0.5);
        assertEquals(100, width(d), 0.5);
        assertEquals(200, d.getLayoutX(), 0.5);
    }

    @Test
    void autoFillRepeatsAsOftenAsFits() {
        Region[] boxes = new Region[7];
        for (int i = 0; i < boxes.length; i++) boxes[i] = createBox(10, 10);
        CssGrid grid = gridWithColumns("repeat(auto-fill, 200)", boxes);
        grid.setColumnGap(10);

        layoutAt(grid, 650, 100);

        // floor((650 + 10) / 210) = 3 columns
        assertBounds(boxes[2], 420, 0, 200, 10);
        assertBounds(boxes[3], 0, 10, 200, 10);
        assertBounds(boxes[6], 0, 20, 200, 10);
    }

    @Test
    void autoFillCountsFixedTracksOutsideTheRepeat() {
        Region[] boxes = new Region[4];
        for (int i = 0; i < boxes.length; i++) boxes[i] = createBox(10, 10);
        CssGrid grid = gridWithColumns("100 repeat(auto-fill, 200)", boxes);

        layoutAt(grid, 500, 100);

        // 100 + 2 * 200 = 500 fits exactly, a third repetition would not
        assertBounds(boxes[1], 100, 0, 200, 10);
        assertBounds(boxes[2], 300, 0, 200, 10);
        assertBounds(boxes[3], 0, 10, 100, 10);
    }

    @Test
    void autoFillAlwaysProducesAtLeastOneTrack() {
        Region a = createBox(10, 10);
        CssGrid grid = gridWithColumns("repeat(auto-fill, 200)", a);

        layoutAt(grid, 50, 100);

        assertEquals(200, width(a), 0.5);
    }

    @Test
    void autoFillLeavesEmptyTracksInPlace() {
        Region a = createBox(10, 10), b = createBox(10, 10);
        CssGrid grid = gridWithColumns("repeat(auto-fill, minmax(200, 1fr))", a, b);

        layoutAt(grid, 650, 100);

        // 3 tracks, the empty third one still takes its share (track edges are snapped to whole pixels)
        assertEquals(650 / 3.0, width(a), 1);
        assertEquals(650 / 3.0, width(b), 1);
    }

    @Test
    void autoFitCollapsesEmptyTracks() {
        Region a = createBox(10, 10), b = createBox(10, 10);
        CssGrid grid = gridWithColumns("repeat(auto-fit, minmax(200, 1fr))", a, b);
        grid.setColumnGap(10);

        layoutAt(grid, 650, 100);

        // 3 repetitions fit, the empty third one collapses together with its gap: (650 - 10) / 2
        assertBounds(a, 0, 0, 320, 10);
        assertBounds(b, 330, 0, 320, 10);
    }

    @Test
    void implicitTracksUseAutoColumnsCyclically() {
        Region a = createBox(10, 10), b = createBox(10, 10), c = createBox(10, 10), d = createBox(10, 10);
        CssGrid grid = gridWithColumns("50", a, b, c, d);
        grid.setAutoColumns("100 20");
        grid.setAutoFlow(GridAutoFlow.COLUMN);
        grid.setTemplateRows("30");
        grid.setJustifyContent(GridContentAlignment.START);

        layoutAt(grid, 500, 100);

        assertEquals(50, width(a), 0.5);
        assertEquals(100, width(b), 0.5);
        assertEquals(20, width(c), 0.5);
        assertEquals(100, width(d), 0.5);
    }

    @Test
    void autoFillRowsUseTheDefiniteHeight() {
        Region[] boxes = new Region[5];
        for (int i = 0; i < boxes.length; i++) boxes[i] = createBox(10, 10);
        CssGrid grid = gridWithColumns("100", boxes);
        grid.setTemplateRows("repeat(auto-fill, 40)");
        grid.setAutoFlow(GridAutoFlow.COLUMN);
        grid.setAutoColumns("100");
        grid.setJustifyContent(GridContentAlignment.START);

        layoutAt(grid, 500, 130);

        // 3 explicit rows fit into 130: the 4th item starts the second column
        assertBounds(boxes[2], 0, 80, 100, 40);
        assertBounds(boxes[3], 100, 0, 100, 40);
    }

    @Test
    void implicitRowsUseAutoRows() {
        Region a = createBox(10, 10), b = createBox(10, 10);
        CssGrid grid = gridWithColumns("100", a, b);
        grid.setTemplateRows("30");
        grid.setAutoRows("minmax(60, auto)");

        layoutAt(grid, 100, 300);

        assertBounds(a, 0, 0, 100, 30);
        assertBounds(b, 0, 30, 100, 60);
    }

    @Test
    void rowsAreSizedFromItemHeightsAtResolvedWidth() {
        Region tallBiased = new Region() {
            @Override public javafx.geometry.Orientation getContentBias() { return javafx.geometry.Orientation.HORIZONTAL; }
            @Override protected double computePrefHeight(double width) { return width > 0 ? 1000 / width : 10; }
            @Override protected double computeMinHeight(double width) { return computePrefHeight(width); }
        };
        tallBiased.setMinWidth(10);
        tallBiased.setPrefWidth(10);
        tallBiased.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        CssGrid grid = gridWithColumns("200", tallBiased);

        layoutAt(grid, 200, 300);

        // Stretched to 200 wide, so the row is 1000 / 200 = 5 high
        assertEquals(5, height(tallBiased), 0.5);
    }
}
