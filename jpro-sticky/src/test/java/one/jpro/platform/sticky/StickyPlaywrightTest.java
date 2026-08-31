package one.jpro.platform.sticky;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import one.jpro.platform.playwright.BrowserErrorCollector;
import one.jpro.platform.playwright.JProInput;
import one.jpro.platform.playwright.JProPlaywrightTest;
import one.jpro.platform.playwright.JProScroll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for {@code jpro-sticky} against its {@code :jpro-sticky:example} app
 * ({@code ScrollSample}) in a real browser, built on the {@code jpro-playwright} helper library.
 * These cover the half of the feature the module's headless unit tests cannot: the native compositor
 * pin and picking as the page actually scrolls (its {@code WebScrollImplTest} stubs the browser and
 * states outright that "the compositor motion itself is driven by injected CSS in the browser and
 * cannot run headless").
 *
 * <p>The assertion tool is viewport geometry: a pinned sticky/fixed element holds its
 * {@link JProScroll#top} across a scroll while in-flow content moves; picking is proved by clicking
 * a pinned button and watching its server-side click counter increment. Requires a Chromium
 * installed via {@code ./gradlew :jpro-sticky:installPlaywright}.
 */
public class StickyPlaywrightTest extends JProPlaywrightTest {

    private static final String START_TASK = ":jpro-sticky:example:jproStart";
    private static final String STOP_TASK = ":jpro-sticky:example:jproStop";
    private static final File LOGS_DIR = new File(projectRoot(), "jpro-sticky/example/logs");

    /** A pinned element must not drift more than this many px across a scroll. */
    private static final double PIN_TOLERANCE_PX = 3;
    /** An in-flow element must move at least this far to prove the page really scrolled. */
    private static final double SCROLLED_MIN_PX = 50;

    @BeforeAll
    static void startJProServer() throws Exception {
        startServer(LOGS_DIR, gradleCommand(START_TASK));
    }

    @AfterAll
    static void stopJProServer() throws Exception {
        stopAndAssertNoServerErrors(LOGS_DIR, gradleCommand(STOP_TASK));
    }

    private Page page;
    private BrowserErrorCollector browserErrors;

    @BeforeEach
    void openPage() {
        page = newPage();
        browserErrors = new BrowserErrorCollector(page);
        page.navigate(BASE_URL);
        waitForRunning(page);
        // Let the scene lay out before the first geometry read.
        page.waitForTimeout(500);
    }

    @AfterEach
    void closePage() {
        if (page != null) page.context().close();
        if (browserErrors != null) browserErrors.assertNoErrors();
    }

    @Test
    @DisplayName("Sticky page header stays pinned to the top while content scrolls under it")
    void stickyHeaderStaysPinnedWhileContentScrolls() {
        double headerBefore = JProScroll.awaitTop(page, "#jpro-sticky-header");
        double witnessBefore = JProScroll.awaitTop(page, "#jpro-scrollpane");

        JProScroll.scrollBy(page, 150);

        double headerAfter = JProScroll.awaitTop(page, "#jpro-sticky-header");
        double witnessAfter = JProScroll.awaitTop(page, "#jpro-scrollpane");

        assertTrue(witnessBefore - witnessAfter >= SCROLLED_MIN_PX,
                "page should have scrolled: in-flow #jpro-scrollpane moved from " + witnessBefore
                        + " to " + witnessAfter);
        assertTrue(Math.abs(headerAfter - headerBefore) <= PIN_TOLERANCE_PX,
                "sticky header should stay pinned: top moved from " + headerBefore
                        + " to " + headerAfter);
    }

    @Test
    @DisplayName("Clicking the sticky header while it is pinned lands on the server-pinned element")
    void clickLandsOnPinnedStickyHeader() {
        JProScroll.scrollBy(page, 200);

        // The header is pinned at the top; a click must reach the reparented, server-pinned button.
        page.locator("#jpro-sticky-header").click();
        JProInput.awaitText(page, "#jpro-sticky-header", "Sticky page header  (clicks: 1)");
    }

    @Test
    @DisplayName("A click reaches the in-flow button beneath the mouse-transparent fullscreen overlay")
    void clickReachesButtonBeneathOverlay() {
        // The in-flow "flow button" sits below the sticky header and under the fixed full-viewport
        // overlay (setFixedFullscreen + setMouseTransparent). A plain click must land on it and
        // round-trip to its counter. This exercises both halves at once: the header's placeholder
        // reserves its slot so the button is not hidden behind the header, and the overlay is mirrored
        // to pointer-events:none so it does not swallow the click on its way to the content beneath.
        page.locator("#jpro-flow-button").click();
        JProInput.awaitText(page, "#jpro-flow-button", "Flow button (not pinned)  (clicks: 1)");

        // And the overlay really is the transparent layer in between (guards the pointer-events mapping).
        String overlayPe = (String) page.evaluate(
                "() => getComputedStyle(document.getElementById('jpro-overlay')).pointerEvents");
        assertEquals("none", overlayPe, "fullscreen mouse-transparent overlay must be pointer-events:none");
    }

    @Test
    @DisplayName("Fixed toast holds its viewport position across a large scroll")
    void fixedToastStaysPinned() {
        double before = JProScroll.awaitTop(page, "#jpro-toast");

        JProScroll.scrollBy(page, 400);

        double after = JProScroll.awaitTop(page, "#jpro-toast");
        assertTrue(Math.abs(after - before) <= PIN_TOLERANCE_PX,
                "fixed toast should not move: top went from " + before + " to " + after);
    }

    @Test
    @DisplayName("Bounded section sub-header pins at its offset, then releases at the section end")
    void boundedSectionHeaderPinsThenReleases() {
        // Scroll down until the section sub-header reaches its 48px pin and holds there.
        double pinned = scrollUntilPinnedNear(page, "#jpro-section-header", 48);
        assertTrue(Math.abs(pinned - 48) <= PIN_TOLERANCE_PX + 2,
                "section sub-header should pin near 48px, was " + pinned);

        // Scroll well past the section's end; the sub-header must release — either move above the pin
        // with its content, or scroll off the top entirely (no box).
        for (int i = 0; i < 12; i++) JProScroll.scrollBy(page, 300);

        Double released = tryTop(page, "#jpro-section-header");
        assertTrue(released == null || released < 48 - PIN_TOLERANCE_PX,
                "section sub-header should have released, but is still pinned at " + released);
    }

    @Test
    @DisplayName("ScrollPane sub-header stays pinned to the pane top while the pane scrolls (desktop-shared path)")
    void scrollPaneStickyHeaderPinsWhileScrollingWithin() {
        // Scroll inside the ScrollPane until its sub-header pins to the pane's viewport top.
        JProScroll.scrollWithin(page, "#jpro-scrollpane", 250);
        double pinned = JProScroll.awaitTop(page, "#jpro-scrollpane-header");

        // Keep scrolling the pane; a sticky sub-header holds its position rather than scrolling away.
        JProScroll.scrollWithin(page, "#jpro-scrollpane", 200);
        double stillPinned = JProScroll.awaitTop(page, "#jpro-scrollpane-header");

        assertTrue(Math.abs(stillPinned - pinned) <= PIN_TOLERANCE_PX,
                "ScrollPane sub-header should stay pinned within the pane: moved from " + pinned
                        + " to " + stillPinned);
    }

    @Test
    @DisplayName("Screenshots capture the pinned state (full page and header element)")
    void screenshots() throws Exception {
        JProScroll.scrollBy(page, 250);
        Path full = screenshot(page, "sticky-app");
        Path element = screenshot(page.locator("#jpro-sticky-header"), "sticky-header");
        assertTrue(Files.size(full) > 0, "full-page screenshot should be non-empty");
        assertTrue(Files.size(element) > 0, "element screenshot should be non-empty");
    }

    // ---- helpers ----

    /** {@link JProScroll#top} or {@code null} if the element currently has no box (off-screen). */
    private static Double tryTop(Page page, String selector) {
        BoundingBox box = page.locator(selector).boundingBox();
        return box == null ? null : box.y;
    }

    /**
     * Scroll the page down in steps until {@code selector} settles near {@code expectedTop} (its pin),
     * and return that top. Fails if it never pins within a bounded number of steps.
     */
    private static double scrollUntilPinnedNear(Page page, String selector, double expectedTop) {
        for (int i = 0; i < 40; i++) {
            Double t = tryTop(page, selector);
            if (t != null && Math.abs(t - expectedTop) <= PIN_TOLERANCE_PX + 2) return t;
            JProScroll.scrollBy(page, 150);
        }
        throw new AssertionError("'" + selector + "' never pinned near " + expectedTop
                + "px after scrolling; last top " + tryTop(page, selector));
    }
}
