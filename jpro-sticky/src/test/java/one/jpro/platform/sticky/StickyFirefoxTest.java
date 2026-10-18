package one.jpro.platform.sticky;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import one.jpro.platform.playwright.JProPlaywrightTest;
import one.jpro.platform.playwright.JProWait;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

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
        page.locator(".jpro-id-sticky-header").waitFor(
                new Locator.WaitForOptions().setTimeout(180_000));
        JProWait.waitUntil(() -> page.locator("[data-jpro-sticky-el]").count() > 0, () -> "no pin bound");
        page.evaluate("() => window.scrollBy(0, 400)");
        // the bug parked pinned nodes about a document height down (~6300px).
        JProWait.waitUntil(() -> pinnedOnScreen(page), () -> positions(page));
        JProWait.ensureStaysTrue(1500, () -> pinnedOnScreen(page), () -> positions(page));

        page.context().close();
    }

    private static final String[] PINNED = {".jpro-id-sticky-header", ".jpro-id-toast", ".jpro-id-bottom-bar",
            ".jpro-id-fab", ".jpro-id-overlay"};

    private static boolean pinnedOnScreen(Page page) {
        for (String id : PINNED) {
            if (page.locator(id).boundingBox().y >= page.viewportSize().height) return false;
        }
        return page.locator(".jpro-id-sticky-header").boundingBox().y < PINNED_MAX_TOP_PX;
    }

    private static String positions(Page page) {
        StringBuilder state = new StringBuilder("tops (issue #127; on screen, header top < " + PINNED_MAX_TOP_PX + "):");
        for (String id : PINNED) state.append(' ').append(id).append('=').append(page.locator(id).boundingBox().y);
        return state.toString();
    }
}
