package one.jpro.platform.cssgrid;

import javafx.css.CssMetaData;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GridItemTest extends CssGridTestBase {

    @Test
    void cssMetaDataContainsGridItemProperties() {
        List<String> names = GridItem.getClassCssMetaData().stream()
                .map(CssMetaData::getProperty)
                .collect(Collectors.toList());

        assertTrue(names.containsAll(List.of(
                "grid-area", "grid-column", "grid-row",
                "grid-column-start", "grid-column-end", "grid-row-start", "grid-row-end",
                "justify-self", "align-self", "order")));
        assertTrue(names.indexOf("grid-area") < names.indexOf("grid-row-start"));
    }

    @Test
    void defaultValues() {
        GridItem item = new GridItem();
        assertEquals(GridLine.AUTO, item.getColumnStart());
        assertEquals(GridLine.AUTO, item.getColumnEnd());
        assertEquals(GridLine.AUTO, item.getRowStart());
        assertEquals(GridLine.AUTO, item.getRowEnd());
        assertNull(item.getJustifySelf());
        assertNull(item.getAlignSelf());
        assertEquals(0, item.getOrder());
    }

    @Test
    void settersPropagateToConstraints() {
        GridItem item = new GridItem();
        item.setColumn(2, 4);
        item.setRowSpan(3);
        item.setJustifySelf(GridItemAlignment.END);
        item.setAlignSelf(GridItemAlignment.CENTER);
        item.setOrder(-1);

        assertEquals(GridLine.at(2), CssGrid.getColumnStart(item));
        assertEquals(GridLine.at(4), CssGrid.getColumnEnd(item));
        assertEquals(GridLine.span(3), CssGrid.getRowStart(item));
        assertEquals(GridLine.AUTO, CssGrid.getRowEnd(item));
        assertEquals(GridItemAlignment.END, CssGrid.getJustifySelf(item));
        assertEquals(GridItemAlignment.CENTER, CssGrid.getAlignSelf(item));
        assertEquals(-1, CssGrid.getOrder(item));

        item.setArea("main");
        assertEquals(GridLine.named("main"), CssGrid.getRowEnd(item));

        item.clearPlacement();
        assertEquals(GridLine.AUTO, CssGrid.getColumnStart(item));
        assertEquals(GridLine.AUTO, CssGrid.getRowStart(item));
    }

    @Test
    void gridItemLaysOutLikeAnyChild() {
        GridItem a = new GridItem(createBox(10, 10));
        GridItem b = new GridItem(createBox(10, 10));
        b.setColumn(1, 3);
        CssGrid grid = new CssGrid(a, b);
        grid.setTemplateColumns("100 100");
        grid.setAlignContent(GridContentAlignment.START);

        layoutAt(grid, 200, 100);

        assertBounds(a, 0, 0, 100, 10);
        assertBounds(b, 0, 10, 200, 10);
    }

    @Test
    void shorthandParsing() {
        assertArrayEquals(new GridLine[]{GridLine.at(1), GridLine.at(3)}, GridItem.parseShorthand("1 / 3", 2));
        assertArrayEquals(new GridLine[]{GridLine.at(2), GridLine.AUTO}, GridItem.parseShorthand("2", 2));
        assertArrayEquals(new GridLine[]{GridLine.named("a"), GridLine.named("a")}, GridItem.parseShorthand("a", 2));
        assertArrayEquals(new GridLine[]{GridLine.span(2), GridLine.AUTO}, GridItem.parseShorthand("span 2", 2));
        assertArrayEquals(new GridLine[]{GridLine.AUTO, GridLine.AUTO}, GridItem.parseShorthand(null, 2));
        assertArrayEquals(new GridLine[]{GridLine.named("h"), GridLine.named("h"), GridLine.named("h"), GridLine.named("h")},
                GridItem.parseShorthand("h", 4));
        assertArrayEquals(new GridLine[]{GridLine.at(1), GridLine.named("x"), GridLine.AUTO, GridLine.named("x")},
                GridItem.parseShorthand("1 / x", 4));
        assertArrayEquals(new GridLine[]{GridLine.at(1), GridLine.at(2), GridLine.at(3), GridLine.AUTO},
                GridItem.parseShorthand("1 / 2 / 3", 4));
        assertThrows(IllegalArgumentException.class, () -> GridItem.parseShorthand("1 / 2 / 3", 2));
    }

    @Test
    void staticConstraintsWorkOnPlainNodes() {
        Region plain = createBox(10, 10);
        CssGrid.setColumn(plain, 2);
        assertEquals(GridLine.at(2), CssGrid.getColumnStart(plain));
        CssGrid.clearPlacement(plain);
        assertEquals(GridLine.AUTO, CssGrid.getColumnStart(plain));
    }
}
