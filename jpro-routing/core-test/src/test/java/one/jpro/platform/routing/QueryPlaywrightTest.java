package one.jpro.platform.routing;

import com.microsoft.playwright.Page;
import one.jpro.platform.playwright.BrowserErrorCollector;
import one.jpro.platform.playwright.JProInput;
import one.jpro.platform.playwright.JProPlaywrightTest;
import one.jpro.platform.playwright.JProWait;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * Verifies in a real browser that {@link Request} query parameters arrive URL-decoded, both for the
 * URL the app is opened with and for a link followed inside the app.
 */
public class QueryPlaywrightTest extends JProPlaywrightTest {

    private static final File LOGS_DIR = new File(projectRoot(), "jpro-routing/example/logs");

    @BeforeAll
    static void startJProServer() throws Exception {
        startServer(LOGS_DIR, gradleCommand(":jpro-routing:example:jproStart", "-Psample=query-playwright"));
    }

    @AfterAll
    static void stopJProServer() throws Exception {
        stopAndAssertNoServerErrors(LOGS_DIR, gradleCommand(":jpro-routing:example:jproStop"));
    }

    private Page page;
    private BrowserErrorCollector browserErrors;

    @BeforeEach
    void openPage() {
        page = newPage();
        browserErrors = new BrowserErrorCollector(page);
    }

    @AfterEach
    void closePage() {
        if (page != null) page.context().close();
        if (browserErrors != null) browserErrors.assertNoErrors();
    }

    private void awaitText(String id, String expected) {
        JProWait.waitForValue(expected, () -> JProInput.getText(page, ".jpro-id-" + id));
    }

    @Test
    void initialUrlQueryIsDecoded() {
        page.navigate(BASE_URL + "/query?code=4%2F0AX%3D&scope=openid+email%20profile&flag&state=ab%3D%3D&u=%C3%BC");
        waitForRunning(page);

        awaitText("keys", "code,flag,scope,state,u");
        awaitText("param-code", "[4/0AX=]");
        awaitText("param-scope", "[openid email profile]");
        awaitText("param-flag", "[]");
        awaitText("param-state", "[ab==]");
        awaitText("param-u", "[ü]");
    }

    @Test
    void linkInsideTheAppDecodesItsQuery() {
        page.navigate(BASE_URL + "/query");
        waitForRunning(page);
        awaitText("keys", "");

        page.locator(".jpro-id-encoded-link").click();

        awaitText("keys", "next,space");
        awaitText("param-next", "[a&b]");
        awaitText("param-space", "[x y]");
    }
}
