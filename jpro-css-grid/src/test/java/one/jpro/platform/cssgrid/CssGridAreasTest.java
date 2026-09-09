package one.jpro.platform.cssgrid;

import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CssGridAreasTest extends CssGridTestBase {

    @Test
    void holyGrailLayout() {
        Region header = createBox(10, 10), sidebar = createBox(10, 10), main = createBox(10, 10), footer = createBox(10, 10);
        CssGrid.setArea(header, "header");
        CssGrid.setArea(sidebar, "sidebar");
        CssGrid.setArea(main, "main");
        CssGrid.setArea(footer, "footer");
        CssGrid grid = new CssGrid(header, sidebar, main, footer);
        grid.setTemplateAreas("header header", "sidebar main", "footer footer");
        grid.setTemplateColumns("100 1fr");
        grid.setTemplateRows("50 1fr 30");

        layoutAt(grid, 400, 300);

        assertBounds(header, 0, 0, 400, 50);
        assertBounds(sidebar, 0, 50, 100, 220);
        assertBounds(main, 100, 50, 300, 220);
        assertBounds(footer, 0, 270, 400, 30);
    }

    @Test
    void areasDefineExplicitGridWhenTemplatesAreMissing() {
        Region a = createBox(50, 20), b = createBox(70, 20), c = createBox(10, 10);
        CssGrid.setArea(a, "a");
        CssGrid.setArea(b, "b");
        CssGrid grid = new CssGrid(a, b, c);
        grid.setTemplateAreas("a b");
        grid.setJustifyContent(GridContentAlignment.START);
        grid.setAlignContent(GridContentAlignment.START);

        layoutAt(grid, 400, 300);

        assertBounds(a, 0, 0, 50, 20);
        assertBounds(b, 50, 0, 70, 20);
        // c is auto-placed into the implicit second row
        assertBounds(c, 0, 20, 50, 10);
    }

    @Test
    void templateAreasAcceptsCssForm() {
        CssGrid grid = new CssGrid();
        grid.setTemplateAreas("'a a' 'b c'");
        assertEquals(GridTemplateAreas.of("a a", "b c"), grid.getTemplateAreas());
    }

    @Test
    void namedLinesFromAreas() {
        Region a = createBox(10, 10);
        CssGrid.setColumnStart(a, GridLine.named("main-start"));
        CssGrid.setColumnEnd(a, GridLine.named("footer-end"));
        CssGrid.setRowStart(a, GridLine.named("main"));
        CssGrid.setRowEnd(a, GridLine.named("footer"));
        CssGrid grid = new CssGrid(a);
        grid.setTemplateAreas("header header header", "side main main", "side footer footer");
        grid.setTemplateColumns("100 100 100");
        grid.setTemplateRows("50 50 50");

        layoutAt(grid, 300, 150);

        assertBounds(a, 100, 50, 200, 100);
    }

    @Test
    void unknownAreaNameResolvesToFirstImplicitLine() {
        Region a = createBox(10, 10), b = createBox(10, 10);
        CssGrid.setArea(b, "missing");
        CssGrid grid = new CssGrid(a, b);
        grid.setTemplateColumns("100 100");
        grid.setTemplateRows("20");
        grid.setAutoColumns("50");
        grid.setAutoRows("30");
        grid.setJustifyContent(GridContentAlignment.START);
        grid.setAlignContent(GridContentAlignment.START);

        layoutAt(grid, 400, 100);

        assertBounds(a, 0, 0, 100, 20);
        // Like in CSS, an unknown name refers to the line after the explicit grid on both axes
        assertBounds(b, 200, 20, 50, 30);
    }

    @Test
    void areasWithPartialTemplateUseAutoTracksForRemainder() {
        Region a = createBox(10, 10), b = createBox(10, 10);
        CssGrid.setArea(a, "a");
        CssGrid.setArea(b, "b");
        CssGrid grid = new CssGrid(a, b);
        grid.setTemplateAreas("a b");
        grid.setTemplateColumns("100");
        grid.setAutoColumns("60");
        grid.setJustifyContent(GridContentAlignment.START);

        layoutAt(grid, 300, 100);

        assertEquals(100, width(a), 0.5);
        assertBounds(b, 100, 0, 60, 100);
    }
}
