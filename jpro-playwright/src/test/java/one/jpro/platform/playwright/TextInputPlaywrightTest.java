package one.jpro.platform.playwright;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end tests against the {@code :jpro-playwright:example} JPro app, doubling as worked
 * examples of {@link JProInput} keyboard handling. Requires a Chromium installed via
 * {@code ./gradlew :jpro-playwright:installPlaywright}.
 */
public class TextInputPlaywrightTest extends JProPlaywrightTest {

    private static final String START_TASK = ":jpro-playwright:example:jproStart";
    private static final String STOP_TASK = ":jpro-playwright:example:jproStop";
    private static final File LOGS_DIR = new File(projectRoot(), "jpro-playwright/example/logs");

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
        page.navigate(BASE_URL + "/textinput");
        waitForRunning(page);
    }

    @AfterEach
    void closePage() {
        if (page != null) page.close();
        if (browserErrors != null) browserErrors.assertNoErrors();
    }

    @Test
    @DisplayName("Typing into a JPro TextField round-trips to the bound label")
    void typingRoundTripsToBoundLabel() {
        JProInput.typeInto(page, "#jpro-textfield", "hello");
        JProInput.awaitText(page, "#jpro-echo", "hello");
    }

    @Test
    @DisplayName("onAction label updates only after Enter is pressed to commit")
    void commitOnEnter() {
        JProInput.typeInto(page, "#jpro-committed", "world");

        // Before commit the onAction handler hasn't fired — the echo stays empty.
        assertEquals("", JProInput.getText(page, "#jpro-committed-echo"));

        JProInput.commit(page);
        JProInput.awaitText(page, "#jpro-committed-echo", "world");
    }

    @Test
    @DisplayName("Clicking a button round-trips to the counter label")
    void clickRoundTrip() {
        page.locator("#jpro-button").click();
        JProInput.awaitText(page, "#jpro-count", "1");
    }
}
