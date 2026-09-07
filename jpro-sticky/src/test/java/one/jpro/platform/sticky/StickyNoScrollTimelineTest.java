package one.jpro.platform.sticky;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import one.jpro.platform.playwright.JProPlaywrightTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression guard for issue #127 on engines <em>without</em> scroll-driven animations.
 *
 * <p>Firefox does not support {@code animation-timeline: scroll()} (nor does Safari &lt; 26), and the
 * rule {@code WebScrollImpl} emits is actively harmful there if it is emitted anyway: the engine drops
 * the unknown {@code animation-timeline}, {@code animation-duration: auto} resolves to {@code 0s}, and
 * {@code animation-fill-mode: both} snaps the node to the {@code to} keyframe — parking every pinned
 * element a document-height below the viewport, i.e. invisible. This test drives the real app in
 * Firefox and asserts pinned elements are on screen and pinned by the server-side fallback.
 *
 * <p>Requires a Firefox installed via {@code ./gradlew :jpro-sticky:installPlaywrightFirefox}.
 */
public class StickyNoScrollTimelineTest extends JProPlaywrightTest {

    private static final File LOGS_DIR = new File(projectRoot(), "jpro-sticky/example/logs");

    /** How far a pinned element may sit below the viewport top before we call it "not pinned". */
    private static final double PINNED_MAX_TOP_PX = 120;

    private static Playwright ffPlaywright;
    private static Browser firefox;

    @BeforeAll
    static void startAll() throws Exception {
        startServer(LOGS_DIR, gradleCommand(":jpro-sticky:example:jproStart"));
        ffPlaywright = Playwright.create();
        firefox = ffPlaywright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void stopAll() throws Exception {
        if (firefox != null) firefox.close();
        if (ffPlaywright != null) ffPlaywright.close();
        stopServer(gradleCommand(":jpro-sticky:example:jproStop"));
    }

    @Test
    @DisplayName("#127: without scroll-timeline support, pinned nodes stay on screen (not parked off-page)")
    void pinnedNodesStayOnScreenWithoutScrollTimelineSupport() {
        Page page = firefox.newContext(
                new Browser.NewContextOptions().setViewportSize(420, 860)).newPage();
        page.navigate(BASE_URL);
        // Firefox boots the app slower than the shared waitForRunning budget allows; wait plainly.
        page.locator("#jpro-sticky-header").waitFor(
                new Locator.WaitForOptions().setTimeout(180_000));
        page.waitForTimeout(3000);

        // Precondition: this really is an engine without scroll-driven animations, so the guard is
        // what is under test rather than the compositor path.
        assertTrue(Boolean.FALSE.equals(page.evaluate(
                        "() => CSS.supports('animation-timeline','scroll()')")),
                "expected Firefox to lack scroll-driven animations; if it gained them, "
                        + "this test no longer covers the unsupported path");

        page.evaluate("() => window.scrollBy(0, 400)");
        page.waitForTimeout(1500);

        // The bug parked pinned nodes ~a document height down (~6300px). Assert they are pinned near
        // the viewport top instead -- the server-side fallback's job.
        for (String id : new String[]{"#jpro-sticky-header", "#jpro-toast", "#jpro-bottom-bar",
                "#jpro-fab", "#jpro-overlay"}) {
            double top = page.locator(id).boundingBox().y;
            assertTrue(top < page.viewportSize().height,
                    id + " is parked off-screen at top=" + top + " (issue #127)");
        }

        double headerTop = page.locator("#jpro-sticky-header").boundingBox().y;
        assertTrue(headerTop < PINNED_MAX_TOP_PX,
                "sticky header should be pinned near the viewport top, was at " + headerTop);

        page.context().close();
    }
}
