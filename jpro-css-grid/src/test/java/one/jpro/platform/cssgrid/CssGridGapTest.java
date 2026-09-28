package one.jpro.platform.cssgrid;

import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CssGridGapTest extends CssGridTestBase {

    @Test
    void gapsSeparateTracks() {
        Region a = createBox(10, 10), b = createBox(10, 10), c = createBox(10, 10), d = createBox(10, 10);
        CssGrid grid = new CssGrid(a, b, c, d);
        grid.setTemplateColumns("100 100");
        grid.setTemplateRows("50 50");
        grid.setColumnGap(10);
        grid.setRowGap(20);

        layoutAt(grid, 210, 120);

        assertBounds(a, 0, 0, 100, 50);
        assertBounds(b, 110, 0, 100, 50);
        assertBounds(c, 0, 70, 100, 50);
        assertBounds(d, 110, 70, 100, 50);
    }

    @Test
    void setGapSetsBoth() {
        CssGrid grid = new CssGrid();
        grid.setGap(12);
        assertEquals(12, grid.getRowGap(), 0.01);
        assertEquals(12, grid.getColumnGap(), 0.01);
    }

    @Test
    void gapsAreExcludedFromFlexibleSpace() {
        Region a = createBox(10, 10), b = createBox(10, 10);
        CssGrid grid = new CssGrid(a, b);
        grid.setTemplateColumns("1fr 1fr");
        grid.setColumnGap(20);

        layoutAt(grid, 220, 50);

        assertEquals(100, width(a), 0.5);
        assertBounds(b, 120, 0, 100, 50);
    }

    @Test
    void spanningItemAreaIncludesInnerGaps() {
        Region a = createBox(10, 10);
        CssGrid.setColumnSpan(a, 2);
        CssGrid grid = new CssGrid(a);
        grid.setTemplateColumns("100 100");
        grid.setColumnGap(10);

        layoutAt(grid, 300, 50);

        assertEquals(210, width(a), 0.5);
        assertEquals(210, grid.prefWidth(-1), 0.5);
    }
}
