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
 * Regression guard for issue #127, which was reported against Firefox: every pinned node sat a
 * document height below the viewport, out of sight. This drives the real app in Firefox and asserts
 * the pinned elements are on screen, with the sticky header at its pin.
 *
 * <p>Requires a Firefox installed via {@code ./gradlew :jpro-sticky:installPlaywrightFirefox}.
 */
public class StickyFirefoxTest extends JProPlaywrightTest {

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
    @DisplayName("#127: pinned nodes stay on screen in Firefox (not parked off-page)")
    void pinnedNodesStayOnScreenInFirefox() {
        Page page = firefox.newContext(
                new Browser.NewContextOptions().setViewportSize(420, 860)).newPage();
        page.navigate(BASE_URL);
        // Firefox boots the app slower than the shared waitForRunning budget allows; wait plainly.
        page.locator("#jpro-sticky-header").waitFor(
                new Locator.WaitForOptions().setTimeout(180_000));
        page.waitForTimeout(3000);

        page.evaluate("() => window.scrollBy(0, 400)");
        page.waitForTimeout(1500);

        // the bug parked pinned nodes about a document height down (~6300px).
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
