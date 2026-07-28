package one.jpro.platform.scroll;

import one.jpro.platform.scroll.ScrollAnchor.Axis;
import one.jpro.platform.scroll.ScrollAnchor.Mode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link ScrollAnchor}: the per-axis resolution (NATURAL / PIN_START / PIN_END /
 * CENTER / STRETCH), wither immutability, and the conflict validation. Pure value-object tests —
 * no JavaFX toolkit or JPro session required.
 *
 * @author Tobias Horak
 */
class ScrollAnchorTest {

    // ---------------------------------------------------------------------
    // Default: NATURAL on both axes
    // ---------------------------------------------------------------------

    @Test
    void ofIsNaturalOnBothAxes() {
        ScrollAnchor a = ScrollAnchor.of();
        assertEquals(Mode.NATURAL, a.horizontal().mode);
        assertEquals(Mode.NATURAL, a.vertical().mode);
    }

    // ---------------------------------------------------------------------
    // Single-edge pins
    // ---------------------------------------------------------------------

    @Test
    void topPinsVerticalStart() {
        Axis v = ScrollAnchor.of().top(12).vertical();
        assertEquals(Mode.PIN_START, v.mode);
        assertEquals(12, v.start);
    }

    @Test
    void bottomPinsVerticalEnd() {
        Axis v = ScrollAnchor.of().bottom(24).vertical();
        assertEquals(Mode.PIN_END, v.mode);
        assertEquals(24, v.end);
    }

    @Test
    void leftPinsHorizontalStart() {
        Axis h = ScrollAnchor.of().left(8).horizontal();
        assertEquals(Mode.PIN_START, h.mode);
        assertEquals(8, h.start);
    }

    @Test
    void rightPinsHorizontalEnd() {
        Axis h = ScrollAnchor.of().right(8).horizontal();
        assertEquals(Mode.PIN_END, h.mode);
        assertEquals(8, h.end);
    }

    @Test
    void pinsAreIndependentPerAxis() {
        ScrollAnchor corner = ScrollAnchor.of().bottom(24).right(16);
        assertEquals(Mode.PIN_END, corner.vertical().mode);
        assertEquals(24, corner.vertical().end);
        assertEquals(Mode.PIN_END, corner.horizontal().mode);
        assertEquals(16, corner.horizontal().end);
    }

    // ---------------------------------------------------------------------
    // Center
    // ---------------------------------------------------------------------

    @Test
    void centerXCentersHorizontalWithZeroShift() {
        Axis h = ScrollAnchor.of().centerX().horizontal();
        assertEquals(Mode.CENTER, h.mode);
        assertEquals(0, h.start);
    }

    @Test
    void centerYCarriesShift() {
        Axis v = ScrollAnchor.of().centerY(-30).vertical();
        assertEquals(Mode.CENTER, v.mode);
        assertEquals(-30, v.start);
    }

    @Test
    void toastIsPinnedTopAndCenteredX() {
        ScrollAnchor toast = ScrollAnchor.of().top(16).centerX();
        assertEquals(Mode.PIN_START, toast.vertical().mode);
        assertEquals(Mode.CENTER, toast.horizontal().mode);
    }

    // ---------------------------------------------------------------------
    // Stretch: two edges on the same axis
    // ---------------------------------------------------------------------

    @Test
    void topPlusBottomStretchesVertical() {
        Axis v = ScrollAnchor.of().top(4).bottom(8).vertical();
        assertEquals(Mode.STRETCH, v.mode);
        assertEquals(4, v.start);
        assertEquals(8, v.end);
    }

    @Test
    void leftPlusRightStretchesHorizontal() {
        Axis h = ScrollAnchor.of().left(4).right(8).horizontal();
        assertEquals(Mode.STRETCH, h.mode);
        assertEquals(4, h.start);
        assertEquals(8, h.end);
    }

    @Test
    void stretchIsOrderIndependent() {
        Axis a = ScrollAnchor.of().top(4).bottom(8).vertical();
        Axis b = ScrollAnchor.of().bottom(8).top(4).vertical();
        assertEquals(a.mode, b.mode);
        assertEquals(a.start, b.start);
        assertEquals(a.end, b.end);
    }

    @Test
    void barIsStretchOneAxisPinOther() {
        ScrollAnchor bar = ScrollAnchor.of().top(0).left(0).right(0);
        assertEquals(Mode.PIN_START, bar.vertical().mode);
        assertEquals(Mode.STRETCH, bar.horizontal().mode);
    }

    @Test
    void allStretchesBothAxes() {
        ScrollAnchor full = ScrollAnchor.of().all(6);
        assertEquals(Mode.STRETCH, full.horizontal().mode);
        assertEquals(Mode.STRETCH, full.vertical().mode);
        assertEquals(6, full.vertical().start);
        assertEquals(6, full.vertical().end);
        assertEquals(6, full.horizontal().start);
        assertEquals(6, full.horizontal().end);
    }

    // ---------------------------------------------------------------------
    // Conflicts: center vs edge on the same axis
    // ---------------------------------------------------------------------

    @Test
    void centerThenEdgeOnSameAxisThrows() {
        assertThrows(IllegalStateException.class, () -> ScrollAnchor.of().centerX().left(0));
        assertThrows(IllegalStateException.class, () -> ScrollAnchor.of().centerX().right(0));
        assertThrows(IllegalStateException.class, () -> ScrollAnchor.of().centerY().top(0));
        assertThrows(IllegalStateException.class, () -> ScrollAnchor.of().centerY().bottom(0));
    }

    @Test
    void edgeThenCenterOnSameAxisThrows() {
        assertThrows(IllegalStateException.class, () -> ScrollAnchor.of().left(0).centerX());
        assertThrows(IllegalStateException.class, () -> ScrollAnchor.of().right(0).centerX());
        assertThrows(IllegalStateException.class, () -> ScrollAnchor.of().top(0).centerY());
        assertThrows(IllegalStateException.class, () -> ScrollAnchor.of().bottom(0).centerY());
    }

    @Test
    void stretchedAxisThenCenterThrows() {
        assertThrows(IllegalStateException.class,
                () -> ScrollAnchor.of().top(0).bottom(0).centerY());
    }

    @Test
    void centerOnTheOtherAxisIsFine() {
        // Pinning one axis and centering the perpendicular axis is the toast case — not a conflict.
        ScrollAnchor toast = ScrollAnchor.of().top(0).centerX();
        assertEquals(Mode.PIN_START, toast.vertical().mode);
        assertEquals(Mode.CENTER, toast.horizontal().mode);
    }

    // ---------------------------------------------------------------------
    // Immutability + overrides
    // ---------------------------------------------------------------------

    @Test
    void withersReturnNewInstances() {
        ScrollAnchor base = ScrollAnchor.of();
        ScrollAnchor pinned = base.top(0);
        assertNotSame(base, pinned);
        // The original is untouched.
        assertEquals(Mode.NATURAL, base.vertical().mode);
    }

    @Test
    void resettingSameEdgeOverridesOffset() {
        Axis v = ScrollAnchor.of().top(5).top(20).vertical();
        assertEquals(Mode.PIN_START, v.mode);
        assertEquals(20, v.start);
    }
}
