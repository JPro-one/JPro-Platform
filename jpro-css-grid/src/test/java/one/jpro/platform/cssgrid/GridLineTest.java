package one.jpro.platform.cssgrid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GridLineTest {

    @Test
    void parsesAllForms() {
        assertEquals(GridLine.AUTO, GridLine.parse("auto"));
        assertEquals(GridLine.AUTO, GridLine.parse(" "));
        assertEquals(GridLine.at(3), GridLine.parse("3"));
        assertEquals(GridLine.at(-1), GridLine.parse("-1"));
        assertEquals(GridLine.span(2), GridLine.parse("span 2"));
        assertEquals(GridLine.span(2), GridLine.parse("  SPAN   2 "));
        assertEquals(GridLine.named("header-start"), GridLine.parse("header-start"));
        assertEquals(GridLine.named("spanner"), GridLine.parse("spanner"));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> GridLine.parse("0"));
        assertThrows(IllegalArgumentException.class, () -> GridLine.parse("span"));
        assertThrows(IllegalArgumentException.class, () -> GridLine.parse("span 0"));
        assertThrows(IllegalArgumentException.class, () -> GridLine.parse("1 / 2"));
        assertThrows(IllegalArgumentException.class, () -> GridLine.at(0));
        assertThrows(IllegalArgumentException.class, () -> GridLine.span(0));
    }

    @Test
    void toStringRoundTrips() {
        for (String s : new String[]{"auto", "3", "-2", "span 4", "main"}) {
            assertEquals(s, GridLine.parse(s).toString());
        }
    }
}
