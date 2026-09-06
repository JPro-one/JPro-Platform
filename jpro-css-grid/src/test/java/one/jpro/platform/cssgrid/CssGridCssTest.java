package one.jpro.platform.cssgrid;

import javafx.css.CssMetaData;
import javafx.css.StyleOrigin;
import javafx.css.StyleableProperty;
import javafx.css.Styleable;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Applies real CSS (inline styles and stylesheets) through the JavaFX CSS engine.
 */
class CssGridCssTest extends CssGridTestBase {

    @Test
    void cssMetaDataContainsGridProperties() {
        List<String> names = CssGrid.getClassCssMetaData().stream()
                .map(CssMetaData::getProperty)
                .collect(Collectors.toList());

        assertTrue(names.containsAll(List.of(
                "grid-template-columns", "grid-template-rows", "grid-template-areas",
                "grid-auto-columns", "grid-auto-rows", "grid-auto-flow",
                "justify-items", "align-items", "justify-content", "align-content",
                "row-gap", "column-gap", "-fx-padding")));
        assertEquals(CssGrid.getClassCssMetaData(), new CssGrid().getCssMetaData());
    }

    @Test
    void quotedTrackListsAndAreas() {
        CssGrid grid = new CssGrid();
        new Scene(grid);
        grid.setStyle("grid-template-columns: \"200 1fr repeat(2, minmax(50, auto))\";"
                + " grid-template-rows: \"auto 1fr\";"
                + " grid-template-areas: \"'a a' 'b c'\";"
                + " grid-auto-columns: \"minmax(10, 1fr)\";"
                + " grid-auto-rows: \"repeat(auto-fill, 20)\";");
        grid.applyCss();

        assertEquals(GridTrackList.parse("200 1fr repeat(2, minmax(50, auto))"), grid.getTemplateColumns());
        assertEquals(GridTrackList.parse("auto 1fr"), grid.getTemplateRows());
        assertEquals(GridTemplateAreas.of("a a", "b c"), grid.getTemplateAreas());
        assertEquals(GridTrackList.parse("minmax(10, 1fr)"), grid.getAutoColumns());
        assertEquals(GridTrackList.parse("repeat(auto-fill, 20)"), grid.getAutoRows());
    }

    @Test
    void unquotedNumbersAndKeywords() {
        CssGrid grid = new CssGrid();
        new Scene(grid);
        grid.setStyle("grid-template-columns: 100 200 300; grid-template-rows: 40; grid-auto-columns: auto;"
                + " grid-auto-rows: min-content; grid-template-areas: none;");
        grid.applyCss();

        assertEquals(GridTrackList.parse("100 200 300"), grid.getTemplateColumns());
        assertEquals(GridTrackList.parse("40"), grid.getTemplateRows());
        assertEquals(GridTrackList.AUTO, grid.getAutoColumns());
        assertEquals(GridTrackList.parse("min-content"), grid.getAutoRows());
        assertEquals(GridTemplateAreas.NONE, grid.getTemplateAreas());
    }

    @Test
    void enumProperties() {
        CssGrid grid = new CssGrid();
        new Scene(grid);
        grid.setStyle("grid-auto-flow: \"column dense\"; justify-items: center; align-items: end;"
                + " justify-content: space-between; align-content: flex-end; row-gap: 5; column-gap: 7;");
        grid.applyCss();

        assertEquals(GridAutoFlow.COLUMN_DENSE, grid.getAutoFlow());
        assertEquals(GridItemAlignment.CENTER, grid.getJustifyItems());
        assertEquals(GridItemAlignment.END, grid.getAlignItems());
        assertEquals(GridContentAlignment.SPACE_BETWEEN, grid.getJustifyContent());
        assertEquals(GridContentAlignment.END, grid.getAlignContent());
        assertEquals(5, grid.getRowGap(), 0.01);
        assertEquals(7, grid.getColumnGap(), 0.01);

        grid.setStyle("grid-auto-flow: dense; justify-items: normal; justify-content: normal;");
        grid.applyCss();
        assertEquals(GridAutoFlow.ROW_DENSE, grid.getAutoFlow());
        assertEquals(GridItemAlignment.STRETCH, grid.getJustifyItems());
        assertEquals(GridContentAlignment.STRETCH, grid.getJustifyContent());
    }

    @Test
    void styledGridLaysOutChildren() {
        Region a = createBox(10, 10), b = createBox(10, 10);
        CssGrid grid = new CssGrid(a, b);
        new Scene(grid);
        grid.setStyle("grid-template-columns: \"1fr 3fr\"; column-gap: 20; align-content: start;");
        grid.applyCss();

        layoutAt(grid, 420, 100);

        assertBounds(a, 0, 0, 100, 10);
        assertBounds(b, 120, 0, 300, 10);
    }

    @Test
    void invalidValueIsIgnored() {
        CssGrid grid = new CssGrid();
        grid.setTemplateColumns("100");
        new Scene(grid);
        grid.setStyle("grid-template-columns: \"1em\";");

        assertDoesNotThrow(grid::applyCss);
        assertEquals(GridTrackList.parse("100"), grid.getTemplateColumns());
    }

    @Test
    void gridItemLonghandsAndShorthands() {
        GridItem item = new GridItem();
        new Scene(item);
        item.setStyle("grid-column-start: 2; grid-column-end: \"span 2\"; grid-row-start: main; grid-row-end: -1;"
                + " justify-self: center; align-self: end; order: 3;");
        item.applyCss();

        assertEquals(GridLine.at(2), item.getColumnStart());
        assertEquals(GridLine.span(2), item.getColumnEnd());
        assertEquals(GridLine.named("main"), item.getRowStart());
        assertEquals(GridLine.at(-1), item.getRowEnd());
        assertEquals(GridItemAlignment.CENTER, item.getJustifySelf());
        assertEquals(GridItemAlignment.END, item.getAlignSelf());
        assertEquals(3, item.getOrder());
        assertEquals(GridLine.at(2), CssGrid.getColumnStart(item));
        assertEquals(GridItemAlignment.CENTER, CssGrid.getJustifySelf(item));
        assertEquals(3, CssGrid.getOrder(item));

        item.setStyle("grid-column: \"1 / 3\"; grid-row: \"span 2\";");
        item.applyCss();
        assertEquals(GridLine.at(1), item.getColumnStart());
        assertEquals(GridLine.at(3), item.getColumnEnd());
        assertEquals(GridLine.span(2), item.getRowStart());
        assertEquals(GridLine.AUTO, item.getRowEnd());

        item.setStyle("grid-area: header;");
        item.applyCss();
        assertEquals(GridLine.named("header"), item.getRowStart());
        assertEquals(GridLine.named("header"), item.getColumnStart());
        assertEquals(GridLine.named("header"), item.getRowEnd());
        assertEquals(GridLine.named("header"), item.getColumnEnd());

        item.setStyle("grid-area: \"2 / 1 / 4 / 3\"; grid-column: 3;");
        item.applyCss();
        assertEquals(GridLine.at(2), item.getRowStart());
        assertEquals(GridLine.at(3), item.getColumnStart());
        assertEquals(GridLine.at(4), item.getRowEnd());
        assertEquals(GridLine.AUTO, item.getColumnEnd());
    }

    @Test
    void stylesheetPlacesItems(@TempDir Path dir) throws IOException {
        Path css = dir.resolve("grid.css");
        Files.writeString(css, ".grid { grid-template-columns: \"100 100 100\"; grid-template-areas: \"'a a b' 'c c b'\"; align-content: start; }\n"
                + ".item-a { grid-area: a; }\n"
                + ".item-b { grid-area: b; }\n"
                + ".wide { grid-area: c; grid-column-end: \"span 1\"; }\n");

        GridItem a = new GridItem(createBox(10, 10));
        GridItem b = new GridItem(createBox(10, 10));
        GridItem c = new GridItem(createBox(10, 10));
        a.getStyleClass().add("item-a");
        b.getStyleClass().add("item-b");
        c.getStyleClass().addAll("wide");
        CssGrid grid = new CssGrid(a, b, c);
        grid.getStyleClass().add("grid");
        Scene scene = new Scene(grid);
        scene.getStylesheets().add(css.toUri().toString());
        grid.applyCss();

        layoutAt(grid, 300, 100);

        assertBounds(a, 0, 0, 200, 10);
        assertBounds(b, 200, 0, 100, 20);
        // The longhand span 1 overrides the shorthand's area end
        assertBounds(c, 0, 10, 100, 10);
    }

    @Test
    void stylesheetShorthandOverridesJavaValueLikeLonghands(@TempDir Path dir) throws IOException {
        Path css = dir.resolve("item.css");
        Files.writeString(css, ".item { grid-column: \"2 / 4\"; }\n");

        GridItem item = new GridItem();
        item.getStyleClass().add("item");
        item.setColumnStart(GridLine.at(1));
        Scene scene = new Scene(item);
        scene.getStylesheets().add(css.toUri().toString());
        item.applyCss();

        assertEquals(GridLine.at(2), item.getColumnStart());
        assertEquals(GridLine.at(4), item.getColumnEnd());
        assertEquals(StyleOrigin.AUTHOR, ((StyleableProperty<?>) item.columnStartProperty()).getStyleOrigin());
    }

    @Test
    void inlineShorthandOverridesJavaValue() {
        GridItem item = new GridItem();
        item.setColumnStart(GridLine.at(1));
        new Scene(item);
        item.setStyle("grid-column: \"2 / 4\";");
        item.applyCss();

        assertEquals(GridLine.at(2), item.getColumnStart());
        assertEquals(GridLine.at(4), item.getColumnEnd());
    }

    @Test
    void removedShorthandOnlyResetsWhatItSet() {
        GridItem item = new GridItem();
        new Scene(item);
        item.setStyle("grid-column: \"2 / 4\";");
        item.applyCss();
        item.setStyle("grid-area: header;");
        item.applyCss();

        assertEquals(GridLine.named("header"), item.getColumnStart());
        assertEquals(GridLine.named("header"), item.getColumnEnd());

        item.setStyle("");
        item.applyCss();
        assertEquals(GridLine.AUTO, item.getColumnStart());
        assertEquals(GridLine.AUTO, item.getRowStart());
    }

    @Test
    void shorthandRemovedByPseudoClassRestoresOtherShorthand(@TempDir Path dir) throws IOException {
        Path css = dir.resolve("hover.css");
        Files.writeString(css, ".card { grid-area: header; }\n.card:hover { grid-column: \"2 / 4\"; }\n");

        GridItem item = new GridItem();
        item.getStyleClass().add("card");
        Scene scene = new Scene(item);
        scene.getStylesheets().add(css.toUri().toString());
        item.applyCss();
        assertEquals(GridLine.named("header"), item.getColumnStart());

        item.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("hover"), true);
        item.applyCss();
        assertEquals(GridLine.at(2), item.getColumnStart());
        assertEquals(GridLine.at(4), item.getColumnEnd());
        assertEquals(GridLine.named("header"), item.getRowStart());

        item.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("hover"), false);
        item.applyCss();
        assertEquals(GridLine.named("header"), item.getColumnStart());
        assertEquals(GridLine.named("header"), item.getColumnEnd());
    }

    @Test
    void enumConverterAcceptsAliasesAndRejectsUnknown() {
        assertEquals(GridContentAlignment.START, CssGrid.CONTENT_ALIGNMENT_CONVERTER.convertString("flex-start"));
        assertEquals(GridContentAlignment.SPACE_EVENLY, CssGrid.CONTENT_ALIGNMENT_CONVERTER.convertString("Space-Evenly"));
        assertEquals(GridAutoFlow.ROW_DENSE, CssGrid.AUTO_FLOW_CONVERTER.convertString("row dense"));
        assertThrows(IllegalArgumentException.class, () -> CssGrid.AUTO_FLOW_CONVERTER.convertString("sideways"));
    }
}
