package one.jpro.platform.cssgrid;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GridTrackListTest {

    @Test
    void parsesSimpleSizes() {
        List<GridTrack> tracks = GridTrackList.parse("200 1fr 2.5fr auto 50% 10px min-content max-content").getTracks();
        assertEquals(List.of(
                GridTrack.px(200), GridTrack.fr(1), GridTrack.fr(2.5), GridTrack.auto(), GridTrack.percent(50),
                GridTrack.px(10), GridTrack.minContent(), GridTrack.maxContent()), tracks);
    }

    @Test
    void parsesMinmaxAndRepeat() {
        GridTrackList list = GridTrackList.parse("minmax(100, 1fr) repeat(2, 50 auto) repeat(auto-fill, minmax(200px, 1fr))");
        assertEquals(List.of(
                GridTrack.minmax(GridTrack.px(100), GridTrack.fr(1)),
                GridTrack.repeat(2, GridTrack.px(50), GridTrack.auto()),
                GridTrack.autoFill(GridTrack.minmax(GridTrack.px(200), GridTrack.fr(1)))), list.getTracks());
        assertTrue(list.hasAutoRepeat());
    }

    @Test
    void parsesNoneAndEmpty() {
        assertTrue(GridTrackList.parse("none").isEmpty());
        assertTrue(GridTrackList.parse("  ").isEmpty());
        assertSame(GridTrackList.NONE, GridTrackList.parse("none"));
    }

    @Test
    void isCaseInsensitiveAndWhitespaceTolerant() {
        assertEquals(GridTrackList.parse("1fr auto"), GridTrackList.parse("  1FR   AUTO "));
        assertEquals(GridTrackList.parse("repeat(auto-fit, 100)"), GridTrackList.parse("REPEAT( auto-fit ,100 )"));
    }

    @Test
    void toStringRoundTrips() {
        String css = "200 1fr auto minmax(100, 2fr) repeat(3, 50) repeat(auto-fill, minmax(200, 1fr)) 50%";
        assertEquals(css, GridTrackList.parse(css).toString());
        assertEquals("none", GridTrackList.NONE.toString());
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> GridTrackList.parse("1em"));
        assertThrows(IllegalArgumentException.class, () -> GridTrackList.parse("foo"));
        assertThrows(IllegalArgumentException.class, () -> GridTrackList.parse("minmax(1fr, 100)"));
        assertThrows(IllegalArgumentException.class, () -> GridTrackList.parse("repeat(0, 100)"));
        assertThrows(IllegalArgumentException.class, () -> GridTrackList.parse("repeat(2, repeat(2, 100))"));
        assertThrows(IllegalArgumentException.class, () -> GridTrackList.parse("repeat(auto-fill, auto)"));
        assertThrows(IllegalArgumentException.class, () -> GridTrackList.parse("repeat(auto-fill, 100) repeat(auto-fit, 100)"));
        assertThrows(IllegalArgumentException.class, () -> GridTrackList.parse("100 200 )"));
        assertThrows(IllegalArgumentException.class, () -> GridTrackList.parse("-100"));
    }

    @Test
    void factoryValidation() {
        assertThrows(IllegalArgumentException.class, () -> GridTrack.px(-1));
        assertThrows(IllegalArgumentException.class, () -> GridTrack.minmax(GridTrack.fr(1), GridTrack.auto()));
        assertThrows(IllegalArgumentException.class, () -> GridTrack.repeat(1));
        assertThrows(IllegalArgumentException.class, () -> GridTrack.autoFit(GridTrack.fr(1)));
        assertDoesNotThrow(() -> GridTrack.autoFit(GridTrack.minmax(GridTrack.px(10), GridTrack.fr(1))));
    }
}
