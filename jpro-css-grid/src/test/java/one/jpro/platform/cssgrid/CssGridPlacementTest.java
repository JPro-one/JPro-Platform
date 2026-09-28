package one.jpro.platform.cssgrid;

import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CssGridPlacementTest extends CssGridTestBase {

    private static CssGrid threeColumns() {
        CssGrid grid = new CssGrid();
        grid.setTemplateColumns("100 100 100");
        grid.setAlignContent(GridContentAlignment.START);
        return grid;
    }

    @Test
    void autoPlacementInRowFlow() {
        CssGrid grid = threeColumns();
        Region[] boxes = new Region[5];
        for (int i = 0; i < boxes.length; i++) grid.getChildren().add(boxes[i] = createBox(50, 40));

        layoutAt(grid, 300, 300);

        assertBounds(boxes[0], 0, 0, 100, 40);
        assertBounds(boxes[1], 100, 0, 100, 40);
        assertBounds(boxes[2], 200, 0, 100, 40);
        assertBounds(boxes[3], 0, 40, 100, 40);
        assertBounds(boxes[4], 100, 40, 100, 40);
    }

    @Test
    void autoPlacementInColumnFlow() {
        CssGrid grid = new CssGrid();
        grid.setTemplateRows("50 50");
        grid.setAutoColumns("100");
        grid.setAutoFlow(GridAutoFlow.COLUMN);
        grid.setJustifyContent(GridContentAlignment.START);
        Region a = createBox(30, 30), b = createBox(30, 30), c = createBox(30, 30);
        grid.getChildren().addAll(a, b, c);

        layoutAt(grid, 400, 100);

        assertBounds(a, 0, 0, 100, 50);
        assertBounds(b, 0, 50, 100, 50);
        assertBounds(c, 100, 0, 100, 50);
    }

    @Test
    void explicitColumnAndRow() {
        CssGrid grid = threeColumns();
        grid.setTemplateRows("40 40");
        Region a = createBox(20, 20);
        Region b = createBox(20, 20);
        CssGrid.setColumn(b, 3);
        CssGrid.setRow(b, 2);
        grid.getChildren().addAll(a, b);

        layoutAt(grid, 300, 100);

        assertBounds(a, 0, 0, 100, 40);
        assertBounds(b, 200, 40, 100, 40);
    }

    @Test
    void lineRangeSpansTracks() {
        CssGrid grid = threeColumns();
        Region a = createBox(20, 20);
        CssGrid.setColumn(a, 1, 3);
        grid.getChildren().add(a);

        layoutAt(grid, 300, 100);

        assertBounds(a, 0, 0, 200, 20);
    }

    @Test
    void reversedLinesAreSwappedAndEqualLinesSpanOne() {
        CssGrid grid = threeColumns();
        Region a = createBox(20, 20);
        Region b = createBox(20, 20);
        CssGrid.setColumn(a, 3, 1);
        CssGrid.setColumn(b, 2, 2);
        CssGrid.setRow(b, 2);
        grid.getChildren().addAll(a, b);

        layoutAt(grid, 300, 100);

        assertBounds(a, 0, 0, 200, 20);
        assertBounds(b, 100, 20, 100, 20);
    }

    @Test
    void negativeLinesCountFromExplicitEnd() {
        CssGrid grid = threeColumns();
        Region a = createBox(20, 20);
        Region b = createBox(20, 20);
        CssGrid.setColumn(a, -2, -1);
        CssGrid.setColumn(b, -4, -2);
        grid.getChildren().addAll(a, b);

        layoutAt(grid, 300, 100);

        assertBounds(a, 200, 0, 100, 20);
        assertBounds(b, 0, 20, 200, 20);
    }

    @Test
    void lastLineAsStartCreatesImplicitTrackLikeCss() {
        CssGrid grid = threeColumns();
        grid.setAutoColumns("50");
        grid.setJustifyContent(GridContentAlignment.START);
        Region a = createBox(20, 20);
        CssGrid.setColumn(a, -1);
        grid.getChildren().add(a);

        layoutAt(grid, 400, 100);

        assertBounds(a, 300, 0, 50, 20);
    }

    @Test
    void spanCreatesImplicitColumns() {
        CssGrid grid = threeColumns();
        grid.setAutoColumns("50");
        grid.setJustifyContent(GridContentAlignment.START);
        Region a = createBox(20, 20);
        CssGrid.setColumn(a, 1, 5);
        Region b = createBox(20, 20);
        grid.getChildren().addAll(a, b);

        layoutAt(grid, 400, 100);

        assertBounds(a, 0, 0, 350, 20);
        assertBounds(b, 0, 20, 100, 20);
    }

    @Test
    void implicitTracksBeforeExplicitGrid() {
        CssGrid grid = threeColumns();
        grid.setAutoColumns("50");
        grid.setJustifyContent(GridContentAlignment.START);
        Region a = createBox(20, 20);
        CssGrid.setColumn(a, -5, -4);
        Region b = createBox(20, 20);
        grid.getChildren().addAll(a, b);

        layoutAt(grid, 400, 100);

        // Line -5 is one before the explicit grid: an implicit 50px column precedes the explicit ones
        assertBounds(a, 0, 0, 50, 20);
        assertBounds(b, 50, 0, 100, 20);
    }

    @Test
    void lineBeforeExplicitGridAsStart() {
        CssGrid grid = threeColumns();
        grid.setAutoColumns("50");
        grid.setJustifyContent(GridContentAlignment.START);
        Region a = createBox(20, 20);
        Region b = createBox(20, 20);
        CssGrid.setColumn(a, -5);
        CssGrid.setColumn(b, 1, -5);
        grid.getChildren().addAll(a, b);

        layoutAt(grid, 400, 100);

        // Line -5 is one line before the explicit grid: both items land in the implicit 50px column
        assertBounds(a, 0, 0, 50, 20);
        assertBounds(b, 0, 20, 50, 20);
    }

    @Test
    void columnDenseFillsHolesDownwards() {
        CssGrid grid = new CssGrid();
        grid.setTemplateRows("30 30 30");
        grid.setAutoColumns("100");
        grid.setAutoFlow(GridAutoFlow.COLUMN_DENSE);
        grid.setJustifyContent(GridContentAlignment.START);
        Region a = createBox(20, 20), b = createBox(20, 20), c = createBox(20, 20);
        CssGrid.setRowSpan(a, 2);
        CssGrid.setRowSpan(b, 2);
        grid.getChildren().addAll(a, b, c);

        layoutAt(grid, 400, 90);

        assertBounds(a, 0, 0, 100, 60);
        assertBounds(b, 100, 0, 100, 60);
        assertBounds(c, 0, 60, 100, 30);
    }

    @Test
    void autoSpanAutoPlaces() {
        CssGrid grid = threeColumns();
        Region a = createBox(20, 20);
        Region b = createBox(20, 20);
        CssGrid.setColumnSpan(b, 2);
        Region c = createBox(20, 20);
        grid.getChildren().addAll(a, b, c);

        layoutAt(grid, 300, 100);

        assertBounds(a, 0, 0, 100, 20);
        assertBounds(b, 100, 0, 200, 20);
        assertBounds(c, 0, 20, 100, 20);
    }

    @Test
    void spanLargerThanExplicitGridGrowsGrid() {
        CssGrid grid = threeColumns();
        grid.setAutoColumns("50");
        grid.setJustifyContent(GridContentAlignment.START);
        Region a = createBox(20, 20);
        CssGrid.setColumnSpan(a, 4);
        grid.getChildren().add(a);

        layoutAt(grid, 400, 100);

        assertBounds(a, 0, 0, 350, 20);
    }

    @Test
    void sparseFlowLeavesHolesAndDenseFillsThem() {
        for (boolean dense : new boolean[]{false, true}) {
            CssGrid grid = threeColumns();
            grid.setAutoFlow(dense ? GridAutoFlow.ROW_DENSE : GridAutoFlow.ROW);
            Region a = createBox(20, 20), b = createBox(20, 20), c = createBox(20, 20);
            CssGrid.setColumnSpan(a, 2);
            CssGrid.setColumnSpan(b, 2);
            grid.getChildren().addAll(a, b, c);

            layoutAt(grid, 300, 100);

            assertBounds(a, 0, 0, 200, 20);
            assertBounds(b, 0, 20, 200, 20);
            if (dense) {
                assertBounds(c, 200, 0, 100, 20);
            } else {
                assertBounds(c, 200, 20, 100, 20);
            }
        }
    }

    @Test
    void itemsLockedToARowArePlacedBeforeAutoItems() {
        CssGrid grid = threeColumns();
        Region a = createBox(20, 20), b = createBox(20, 20), c = createBox(20, 20), d = createBox(20, 20);
        CssGrid.setRow(c, 1);
        CssGrid.setRow(d, 1);
        grid.getChildren().addAll(a, b, c, d);

        layoutAt(grid, 300, 100);

        assertBounds(c, 0, 0, 100, 20);
        assertBounds(d, 100, 0, 100, 20);
        assertBounds(a, 200, 0, 100, 20);
        assertBounds(b, 0, 20, 100, 20);
    }

    @Test
    void definiteColumnBeforeCursorMovesToNextRow() {
        CssGrid grid = threeColumns();
        Region a = createBox(20, 20), b = createBox(20, 20), c = createBox(20, 20);
        CssGrid.setColumn(c, 1);
        grid.getChildren().addAll(a, b, c);

        layoutAt(grid, 300, 100);

        assertBounds(a, 0, 0, 100, 20);
        assertBounds(b, 100, 0, 100, 20);
        assertBounds(c, 0, 20, 100, 20);
    }

    @Test
    void orderChangesPlacementSequence() {
        CssGrid grid = threeColumns();
        Region a = createBox(20, 20), b = createBox(20, 20), c = createBox(20, 20);
        CssGrid.setOrder(a, 1);
        grid.getChildren().addAll(a, b, c);

        layoutAt(grid, 300, 100);

        assertBounds(b, 0, 0, 100, 20);
        assertBounds(c, 100, 0, 100, 20);
        assertBounds(a, 200, 0, 100, 20);
    }

    @Test
    void unmanagedChildrenAreIgnored() {
        CssGrid grid = threeColumns();
        Region a = createBox(20, 20), b = createBox(20, 20);
        a.setManaged(false);
        grid.getChildren().addAll(a, b);

        layoutAt(grid, 300, 100);

        assertBounds(b, 0, 0, 100, 20);
    }

    @Test
    void explicitlyPlacedItemsOccupyCellsBeforeAutoPlacement() {
        CssGrid grid = threeColumns();
        Region a = createBox(20, 20), b = createBox(20, 20);
        CssGrid.setArea(b, 1, 1, 2, 3);
        grid.getChildren().addAll(a, b);

        layoutAt(grid, 300, 100);

        assertBounds(b, 0, 0, 200, 20);
        assertBounds(a, 200, 0, 100, 20);
    }

    @Test
    void emptyGridDoesNotFail() {
        CssGrid grid = new CssGrid();
        assertDoesNotThrow(() -> layoutAt(grid, 100, 100));
        assertEquals(0, grid.prefWidth(-1), 0.01);
    }

    @Test
    void paddedGridWithoutSizeDoesNotFail() {
        CssGrid grid = new CssGrid(createBox(10, 10));
        grid.setTemplateColumns("50%");
        grid.setTemplateRows("50%");
        grid.setPadding(new javafx.geometry.Insets(8));
        assertDoesNotThrow(() -> layoutAt(grid, 0, 0));
    }
}
