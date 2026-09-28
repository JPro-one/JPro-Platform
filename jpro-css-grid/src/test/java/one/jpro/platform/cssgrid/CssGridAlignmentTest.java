package one.jpro.platform.cssgrid;

import javafx.geometry.Insets;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CssGridAlignmentTest extends CssGridTestBase {

    private static CssGrid twoFixedColumns(Region... children) {
        CssGrid grid = new CssGrid(children);
        grid.setTemplateColumns("100 100");
        grid.setTemplateRows("50");
        return grid;
    }

    @Test
    void justifyContentDistributesTracks() {
        Region a = createBox(10, 10), b = createBox(10, 10);
        CssGrid grid = twoFixedColumns(a, b);

        grid.setJustifyContent(GridContentAlignment.START);
        layoutAt(grid, 400, 50);
        assertEquals(0, a.getLayoutX(), 0.5);
        assertEquals(100, b.getLayoutX(), 0.5);

        grid.setJustifyContent(GridContentAlignment.END);
        layoutAt(grid, 400, 50);
        assertEquals(200, a.getLayoutX(), 0.5);
        assertEquals(300, b.getLayoutX(), 0.5);

        grid.setJustifyContent(GridContentAlignment.CENTER);
        layoutAt(grid, 400, 50);
        assertEquals(100, a.getLayoutX(), 0.5);
        assertEquals(200, b.getLayoutX(), 0.5);

        grid.setJustifyContent(GridContentAlignment.SPACE_BETWEEN);
        layoutAt(grid, 400, 50);
        assertEquals(0, a.getLayoutX(), 0.5);
        assertEquals(300, b.getLayoutX(), 0.5);

        grid.setJustifyContent(GridContentAlignment.SPACE_AROUND);
        layoutAt(grid, 400, 50);
        assertEquals(50, a.getLayoutX(), 0.5);
        assertEquals(250, b.getLayoutX(), 0.5);

        grid.setJustifyContent(GridContentAlignment.SPACE_EVENLY);
        layoutAt(grid, 400, 50);
        assertEquals(200 / 3.0, a.getLayoutX(), 0.5);
        assertEquals(200 / 3.0 * 2 + 100, b.getLayoutX(), 0.5);

        // stretch without auto tracks behaves like start
        grid.setJustifyContent(GridContentAlignment.STRETCH);
        layoutAt(grid, 400, 50);
        assertEquals(0, a.getLayoutX(), 0.5);
        assertEquals(100, b.getLayoutX(), 0.5);
    }

    @Test
    void justifyContentWithGap() {
        Region a = createBox(10, 10), b = createBox(10, 10);
        CssGrid grid = twoFixedColumns(a, b);
        grid.setColumnGap(20);
        grid.setJustifyContent(GridContentAlignment.CENTER);

        layoutAt(grid, 400, 50);

        assertEquals(90, a.getLayoutX(), 0.5);
        assertEquals(210, b.getLayoutX(), 0.5);
    }

    @Test
    void alignContentDistributesRows() {
        Region a = createBox(10, 10), b = createBox(10, 10);
        CssGrid grid = new CssGrid(a, b);
        grid.setTemplateColumns("100");
        grid.setTemplateRows("50 50");

        grid.setAlignContent(GridContentAlignment.END);
        layoutAt(grid, 100, 300);
        assertEquals(200, a.getLayoutY(), 0.5);
        assertEquals(250, b.getLayoutY(), 0.5);

        grid.setAlignContent(GridContentAlignment.SPACE_BETWEEN);
        layoutAt(grid, 100, 300);
        assertEquals(0, a.getLayoutY(), 0.5);
        assertEquals(250, b.getLayoutY(), 0.5);
    }

    @Test
    void alignContentStretchGrowsAutoRows() {
        Region a = createBox(10, 10), b = createBox(10, 10);
        CssGrid grid = new CssGrid(a, b);
        grid.setTemplateColumns("100");

        layoutAt(grid, 100, 300);

        assertBounds(a, 0, 0, 100, 150);
        assertBounds(b, 0, 150, 100, 150);
    }

    @Test
    void overflowingContentFallsBackLikeCss() {
        Region a = createBox(10, 10), b = createBox(10, 10);
        CssGrid grid = twoFixedColumns(a, b);

        grid.setJustifyContent(GridContentAlignment.SPACE_BETWEEN);
        layoutAt(grid, 150, 50);
        assertEquals(0, a.getLayoutX(), 0.5);

        grid.setJustifyContent(GridContentAlignment.SPACE_AROUND);
        layoutAt(grid, 150, 50);
        assertEquals(-25, a.getLayoutX(), 0.5);

        grid.setJustifyContent(GridContentAlignment.END);
        layoutAt(grid, 150, 50);
        assertEquals(-50, a.getLayoutX(), 0.5);
    }

    @Test
    void justifyItemsAlignsWithinArea() {
        Region a = createBox(40, 20);
        CssGrid grid = twoFixedColumns(a);

        grid.setJustifyItems(GridItemAlignment.START);
        layoutAt(grid, 200, 50);
        assertBounds(a, 0, 0, 40, 50);

        grid.setJustifyItems(GridItemAlignment.CENTER);
        layoutAt(grid, 200, 50);
        assertBounds(a, 30, 0, 40, 50);

        grid.setJustifyItems(GridItemAlignment.END);
        layoutAt(grid, 200, 50);
        assertBounds(a, 60, 0, 40, 50);

        grid.setJustifyItems(GridItemAlignment.STRETCH);
        layoutAt(grid, 200, 50);
        assertBounds(a, 0, 0, 100, 50);
    }

    @Test
    void alignItemsAlignsWithinArea() {
        Region a = createBox(40, 20);
        CssGrid grid = twoFixedColumns(a);

        grid.setAlignItems(GridItemAlignment.CENTER);
        layoutAt(grid, 200, 50);
        assertBounds(a, 0, 15, 100, 20);

        grid.setAlignItems(GridItemAlignment.END);
        layoutAt(grid, 200, 50);
        assertBounds(a, 0, 30, 100, 20);
    }

    @Test
    void selfAlignmentOverridesContainer() {
        Region a = createBox(40, 20), b = createBox(40, 20);
        CssGrid grid = twoFixedColumns(a, b);
        grid.setJustifyItems(GridItemAlignment.START);
        grid.setAlignItems(GridItemAlignment.START);
        CssGrid.setJustifySelf(b, GridItemAlignment.END);
        CssGrid.setAlignSelf(b, GridItemAlignment.CENTER);

        layoutAt(grid, 200, 50);

        assertBounds(a, 0, 0, 40, 20);
        assertBounds(b, 160, 15, 40, 20);
    }

    @Test
    void stretchRespectsMaxSize() {
        Region a = createBox(40, 20);
        a.setMaxSize(60, 30);
        CssGrid grid = twoFixedColumns(a);

        layoutAt(grid, 200, 50);

        assertBounds(a, 0, 0, 60, 30);
    }

    @Test
    void nonStretchedItemShrinksToAreaButNotBelowMinimum() {
        Region a = createBox(30, 200, 10, 10);
        CssGrid grid = new CssGrid(a);
        grid.setTemplateColumns("100");
        grid.setJustifyItems(GridItemAlignment.START);

        layoutAt(grid, 100, 50);
        assertEquals(100, width(a), 0.5);

        grid.setTemplateColumns("20");
        layoutAt(grid, 100, 50);
        assertEquals(30, width(a), 0.5);
    }

    @Test
    void marginsReduceTheAvailableArea() {
        Region a = createBox(10, 10);
        CssGrid.setMargin(a, new Insets(5, 10, 15, 20));
        CssGrid grid = twoFixedColumns(a);

        layoutAt(grid, 200, 50);

        assertBounds(a, 20, 5, 70, 30);
    }

    @Test
    void marginsContributeToTrackSizes() {
        Region a = createBox(50, 10);
        CssGrid.setMargin(a, new Insets(0, 10, 0, 10));
        CssGrid grid = new CssGrid(a);
        grid.setTemplateColumns("auto 1fr");
        grid.setAlignContent(GridContentAlignment.START);

        layoutAt(grid, 300, 50);

        assertBounds(a, 10, 0, 50, 10);
        assertEquals(70, grid.prefWidth(-1), 0.5);
    }

    @Test
    void paddingOffsetsTheGrid() {
        Region a = createBox(10, 10);
        CssGrid grid = twoFixedColumns(a);
        grid.setPadding(new Insets(7, 0, 0, 9));

        layoutAt(grid, 300, 100);

        assertBounds(a, 9, 7, 100, 50);
    }
}
