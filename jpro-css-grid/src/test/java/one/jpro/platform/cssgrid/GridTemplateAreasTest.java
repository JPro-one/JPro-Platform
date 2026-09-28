package one.jpro.platform.cssgrid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GridTemplateAreasTest {

    @Test
    void computesAreas() {
        GridTemplateAreas areas = GridTemplateAreas.of("header header header", "sidebar main main", "sidebar footer .");
        assertEquals(3, areas.getRowCount());
        assertEquals(3, areas.getColumnCount());
        assertEquals(new GridTemplateAreas.Area("header", 1, 2, 1, 4), areas.getArea("header"));
        assertEquals(new GridTemplateAreas.Area("sidebar", 2, 4, 1, 2), areas.getArea("sidebar"));
        assertEquals(new GridTemplateAreas.Area("main", 2, 3, 2, 4), areas.getArea("main"));
        assertEquals(new GridTemplateAreas.Area("footer", 3, 4, 2, 3), areas.getArea("footer"));
        assertNull(areas.getArea("."));
    }

    @Test
    void parsesQuotedGroupsAndLines() {
        GridTemplateAreas expected = GridTemplateAreas.of("a a", "b c");
        assertEquals(expected, GridTemplateAreas.parse("'a a' 'b c'"));
        assertEquals(expected, GridTemplateAreas.parse("\"a a\"  \"b c\""));
        assertEquals(expected, GridTemplateAreas.parse("a a\nb c"));
        assertEquals("'a a' 'b c'", expected.toString());
        assertSame(GridTemplateAreas.NONE, GridTemplateAreas.parse("none"));
    }

    @Test
    void resolvesLineNames() {
        GridTemplateAreas areas = GridTemplateAreas.of("header header", "sidebar main");
        assertEquals(1, areas.resolveLine("header", true, true));
        assertEquals(3, areas.resolveLine("header", true, false));
        assertEquals(2, areas.resolveLine("main", true, true));
        assertEquals(2, areas.resolveLine("main", false, true));
        assertEquals(3, areas.resolveLine("main-end", false, true));
        assertEquals(2, areas.resolveLine("sidebar-start", false, false));
        assertEquals(0, areas.resolveLine("unknown", true, true));
    }

    @Test
    void rejectsInvalidAreas() {
        assertThrows(IllegalArgumentException.class, () -> GridTemplateAreas.of("a a", "a b"));
        assertThrows(IllegalArgumentException.class, () -> GridTemplateAreas.of("a b", "b a"));
        assertThrows(IllegalArgumentException.class, () -> GridTemplateAreas.of("a b", "c"));
        assertThrows(IllegalArgumentException.class, () -> GridTemplateAreas.of("a", ""));
        assertThrows(IllegalArgumentException.class, () -> GridTemplateAreas.of("1a"));
    }
}
