package one.jpro.platform.playwright;

import com.microsoft.playwright.Page;
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
        // Close the context (not just the page) so each test doesn't leak a BrowserContext.
        if (page != null) page.context().close();
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

        // Asserting "still empty" can't be a single read right after typing — that races the
        // round-trip. Establish happens-before with an ordered probe: the WebSocket is FIFO, so
        // once the click->counter round-trip lands we know the earlier typing was processed too,
        // and the onAction echo legitimately stayed empty (commit hasn't happened). The click
        // blurs the field — fine, onAction commits on Enter, not focus-loss.
        page.locator("#jpro-button").click();
        JProInput.awaitText(page, "#jpro-count", "1");
        assertEquals("", JProInput.getText(page, "#jpro-committed-echo"));

        // Re-focus the field (the typed text is still there) and press Enter to commit.
        JProInput.focus(page, "#jpro-committed");
        JProInput.commit(page);
        JProInput.awaitText(page, "#jpro-committed-echo", "world");
    }

    @Test
    @DisplayName("Clicking a button round-trips to the counter label")
    void clickRoundTrip() {
        page.locator("#jpro-button").click();
        JProInput.awaitText(page, "#jpro-count", "1");
    }

    @Test
    @DisplayName("Pressing Backspace edits the focused field")
    void backspaceEdits() {
        JProInput.typeInto(page, "#jpro-textfield", "helo");
        JProInput.awaitText(page, "#jpro-echo", "helo");
        JProInput.press(page, "Backspace");
        JProInput.awaitText(page, "#jpro-echo", "hel");
    }

    @Test
    @DisplayName("Screenshots write a full-page and an element PNG")
    void screenshots() throws Exception {
        Path full = screenshot(page, "test-app");
        Path element = screenshot(page.locator("#jpro-textfield"), "test-field");
        assertTrue(Files.size(full) > 0, "full-page screenshot should be non-empty");
        assertTrue(Files.size(element) > 0, "element screenshot should be non-empty");
    }
}
