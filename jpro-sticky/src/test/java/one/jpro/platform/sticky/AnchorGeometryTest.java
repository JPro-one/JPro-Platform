package one.jpro.platform.sticky;

import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.function.DoubleUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link AnchorGeometry} — the shared resolver that both the web ({@link ScrollOverride})
 * and desktop ({@link FXFixedImpl}) paths use to turn a {@link ScrollAnchor} plus an available box into
 * concrete geometry. Because it is one pure function, the same anchor and size yield identical geometry
 * on both platforms; these tests pin the geometry down mode-by-mode, axis-by-axis (STICKY_DESIGN.md §18).
 * <p>
 * {@link #resolve} needs no toolkit (it is arithmetic over doubles and a height function); the
 * {@link #naturalWidth}/{@link #naturalHeight} helpers touch {@link Region}, so those run on the FX thread.
 *
 * @author Tobias Horak
 */
class AnchorGeometryTest {

    private static final double EPS = 1e-9;

    // A fixed available box and node dimensions used across the resolve() cases.
    private static final double AVAIL_W = 1000;
    private static final double AVAIL_H = 800;
    private static final double NATURAL_W = 200;
    private static final double FLOW_X = 30;
    private static final double FLOW_W = 300;
    /** Constant natural height, independent of resolved width, for the position-focused cases. */
    private static final DoubleUnaryOperator H_50 = w -> 50;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FxTestSupport.startToolkit();
    }

    private static AnchorGeometry resolveFixed(ScrollAnchor anchor) {
        return AnchorGeometry.resolve(anchor, AVAIL_W, AVAIL_H, NATURAL_W, H_50, FLOW_X, FLOW_W, true);
    }

    // ---------------------------------------------------------------------
    // NATURAL — keeps the flow slot; fixed takes natural width, sticky the flow width
    // ---------------------------------------------------------------------

    @Test
    void naturalAxesFixedTakeNaturalWidthAtFlowX() {
        AnchorGeometry g = resolveFixed(ScrollAnchor.of());
        assertEquals(FLOW_X, g.x, EPS);
        assertEquals(0, g.y0, EPS);           // bare NATURAL pin line is the top
        assertEquals(NATURAL_W, g.nodeW, EPS); // fixed => natural width, not the flow width
        assertEquals(50, g.nodeH, EPS);
    }

    @Test
    void naturalAxesStickyKeepFlowWidth() {
        AnchorGeometry g = AnchorGeometry.resolve(ScrollAnchor.of(), AVAIL_W, AVAIL_H,
                NATURAL_W, H_50, FLOW_X, FLOW_W, false);
        assertEquals(FLOW_X, g.x, EPS);
        assertEquals(FLOW_W, g.nodeW, EPS); // sticky keeps its full flow width
    }

    // ---------------------------------------------------------------------
    // PIN_START / PIN_END — one edge, natural size on that axis
    // ---------------------------------------------------------------------

    @Test
    void pinTopSetsPinLineToStartInset() {
        assertEquals(8, resolveFixed(ScrollAnchor.of().top(8)).y0, EPS);
    }

    @Test
    void pinBottomOffsetsFromAvailableHeight() {
        AnchorGeometry g = resolveFixed(ScrollAnchor.of().bottom(10));
        assertEquals(AVAIL_H - 50 - 10, g.y0, EPS); // 740
        assertEquals(50, g.nodeH, EPS);
    }

    @Test
    void pinLeftSetsXToStartInset() {
        AnchorGeometry g = resolveFixed(ScrollAnchor.of().left(12));
        assertEquals(12, g.x, EPS);
        assertEquals(NATURAL_W, g.nodeW, EPS);
    }

    @Test
    void pinRightOffsetsFromAvailableWidth() {
        AnchorGeometry g = resolveFixed(ScrollAnchor.of().right(20));
        assertEquals(AVAIL_W - NATURAL_W - 20, g.x, EPS); // 780
        assertEquals(NATURAL_W, g.nodeW, EPS);
    }

    // ---------------------------------------------------------------------
    // STRETCH — span the axis, resizing between the two insets
    // ---------------------------------------------------------------------

    @Test
    void stretchHorizontalResizesBetweenInsets() {
        AnchorGeometry g = resolveFixed(ScrollAnchor.of().left(12).right(20));
        assertEquals(12, g.x, EPS);
        assertEquals(AVAIL_W - 12 - 20, g.nodeW, EPS); // 968
    }

    @Test
    void stretchVerticalResizesBetweenInsets() {
        AnchorGeometry g = resolveFixed(ScrollAnchor.of().top(5).bottom(15));
        assertEquals(5, g.y0, EPS);
        assertEquals(AVAIL_H - 5 - 15, g.nodeH, EPS); // 780
    }

    @Test
    void fullscreenStretchesBothAxesToFillBox() {
        AnchorGeometry g = resolveFixed(ScrollAnchor.of().all(0));
        assertEquals(0, g.x, EPS);
        assertEquals(0, g.y0, EPS);
        assertEquals(AVAIL_W, g.nodeW, EPS);
        assertEquals(AVAIL_H, g.nodeH, EPS);
    }

    // ---------------------------------------------------------------------
    // CENTER — natural size, centered, offset shifts toward the end edge
    // ---------------------------------------------------------------------

    @Test
    void centerHorizontalCentersNaturalWidth() {
        AnchorGeometry g = resolveFixed(ScrollAnchor.of().centerX());
        assertEquals((AVAIL_W - NATURAL_W) / 2.0, g.x, EPS); // 400
    }

    @Test
    void centerHorizontalShiftMovesTowardRight() {
        AnchorGeometry g = resolveFixed(ScrollAnchor.of().centerX(30));
        assertEquals((AVAIL_W - NATURAL_W) / 2.0 + 30, g.x, EPS); // 430
    }

    @Test
    void centerVerticalCentersNaturalHeight() {
        AnchorGeometry g = resolveFixed(ScrollAnchor.of().centerY());
        assertEquals((AVAIL_H - 50) / 2.0, g.y0, EPS); // 375
    }

    // ---------------------------------------------------------------------
    // Height is computed at the RESOLVED width, not the natural width
    // ---------------------------------------------------------------------

    @Test
    void heightIsEvaluatedAtResolvedWidth() {
        // Horizontal STRETCH resolves nodeW to the full box; a width-dependent height must see it.
        DoubleUnaryOperator halfOfWidth = w -> w * 0.5;
        AnchorGeometry g = AnchorGeometry.resolve(ScrollAnchor.of().left(0).right(0), AVAIL_W, AVAIL_H,
                NATURAL_W, halfOfWidth, FLOW_X, FLOW_W, true);
        assertEquals(AVAIL_W, g.nodeW, EPS);
        assertEquals(AVAIL_W * 0.5, g.nodeH, EPS); // 500, from the resolved width not NATURAL_W
    }

    @Test
    void stretchNeverGoesNegativeWhenInsetsExceedBox() {
        // Insets wider than the box clamp the span to zero rather than a negative size.
        AnchorGeometry g = AnchorGeometry.resolve(ScrollAnchor.of().left(700).right(700), AVAIL_W, AVAIL_H,
                NATURAL_W, H_50, FLOW_X, FLOW_W, true);
        assertEquals(0, g.nodeW, EPS);
    }

    // ---------------------------------------------------------------------
    // needsAvailableSize — only end/center/stretch anchors depend on the box
    // ---------------------------------------------------------------------

    @Test
    void naturalAndStartPinsDoNotNeedAvailableSize() {
        assertFalse(AnchorGeometry.needsAvailableSize(ScrollAnchor.of()));
        assertFalse(AnchorGeometry.needsAvailableSize(ScrollAnchor.of().top(8)));
        assertFalse(AnchorGeometry.needsAvailableSize(ScrollAnchor.of().top(0).left(0)));
    }

    @Test
    void endCenterAndStretchNeedAvailableSize() {
        assertTrue(AnchorGeometry.needsAvailableSize(ScrollAnchor.of().bottom(0)));
        assertTrue(AnchorGeometry.needsAvailableSize(ScrollAnchor.of().right(0)));
        assertTrue(AnchorGeometry.needsAvailableSize(ScrollAnchor.of().centerX()));
        assertTrue(AnchorGeometry.needsAvailableSize(ScrollAnchor.of().centerY()));
        assertTrue(AnchorGeometry.needsAvailableSize(ScrollAnchor.of().left(0).right(0)));
        assertTrue(AnchorGeometry.needsAvailableSize(ScrollAnchor.of().all(0)));
    }

    // ---------------------------------------------------------------------
    // naturalWidth / naturalHeight — max of pref and min, layoutBounds for non-Regions
    // ---------------------------------------------------------------------

    @Test
    void naturalSizeHonoursMinOverPref() {
        FxTestSupport.onFx(() -> {
            Region r = new Region();
            r.setPrefWidth(100);
            r.setMinWidth(150);   // min wins
            r.setPrefHeight(40);
            r.setMinHeight(60);   // min wins
            assertEquals(150, AnchorGeometry.naturalWidth(r), EPS);
            assertEquals(60, AnchorGeometry.naturalHeight(r, 150), EPS);
        });
    }

    @Test
    void naturalSizeFallsBackToLayoutBoundsForNonRegion() {
        FxTestSupport.onFx(() -> {
            Rectangle rect = new Rectangle(80, 40);
            assertEquals(80, AnchorGeometry.naturalWidth(rect), EPS);
            assertEquals(40, AnchorGeometry.naturalHeight(rect, 80), EPS);
        });
    }
}
