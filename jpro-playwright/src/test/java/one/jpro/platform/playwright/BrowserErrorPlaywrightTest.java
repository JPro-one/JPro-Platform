package one.jpro.platform.playwright;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link BrowserErrorCollector} against the example app's {@code /jserror} route, which
 * emits a {@code console.error} on load.
 *
 * <p>JPro forwards browser console errors into the server log, so this class intentionally stops
 * the server <em>without</em> asserting a clean log (unlike {@link TextInputPlaywrightTest}) — the
 * browser error it provokes is expected to appear there.
 */
public class BrowserErrorPlaywrightTest extends JProPlaywrightTest {

    private static final String START_TASK = ":jpro-playwright:example:jproStart";
    private static final String STOP_TASK = ":jpro-playwright:example:jproStop";
    private static final File LOGS_DIR = new File(projectRoot(), "jpro-playwright/example/logs");

    @BeforeAll
    static void startJProServer() throws Exception {
        startServer(LOGS_DIR, gradleCommand(START_TASK));
    }

    @AfterAll
    static void stopJProServer() throws Exception {
        stopServer(gradleCommand(STOP_TASK)); // no clean-log assertion: the test emits a browser error
    }

    private Page page;

    @BeforeEach
    void openPage() {
        page = newPage();
    }

    @AfterEach
    void closePage() {
        if (page != null) page.context().close();
    }

    @Test
    @DisplayName("BrowserErrorCollector captures a console.error from the page")
    void capturesConsoleError() {
        BrowserErrorCollector errors = new BrowserErrorCollector(page);
        page.navigate(BASE_URL + "/jserror");
        waitForRunning(page);

        // The console.error arrives asynchronously — poll until the collector reports it.
        AssertionError caught = null;
        for (int i = 0; i < 20 && caught == null; i++) {
            try {
                errors.assertNoErrors();
                page.waitForTimeout(150);
            } catch (AssertionError e) {
                caught = e;
            }
        }
        assertNotNull(caught, "expected BrowserErrorCollector to capture the console.error");
        assertTrue(caught.getMessage().contains("intentional test error"), caught.getMessage());
    }
}
