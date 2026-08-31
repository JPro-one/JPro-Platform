package one.jpro.platform.file;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.playwright.CDPSession;
import com.microsoft.playwright.FileChooser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import one.jpro.platform.playwright.BrowserErrorCollector;
import one.jpro.platform.playwright.JProPlaywrightTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * End-to-end tests of the web implementation ({@code WebFileOpenPicker}, {@code WebFileDropper},
 * {@code WebFileSource}, {@code FileUploadProgress}) against the {@code upload-playwright} example
 * app running under JPro.
 */
public class WebUploadPlaywrightTest extends JProPlaywrightTest {

    private static final File LOGS_DIR = new File(projectRoot(), "jpro-file/example/logs");
    private static final Path FILES = Paths.get("src/test/resources/upload").toAbsolutePath();

    @BeforeAll
    static void startJProServer() throws Exception {
        startServer(LOGS_DIR, gradleCommand(":jpro-file:example:jproStart", "-Psample=upload-playwright"));
    }

    @AfterAll
    static void stopJProServer() throws Exception {
        stopAndAssertNoServerErrors(LOGS_DIR, gradleCommand(":jpro-file:example:jproStop"));
    }

    private Page page;
    private BrowserErrorCollector browserErrors;

    @BeforeEach
    void openPage() {
        page = newPage();
        browserErrors = new BrowserErrorCollector(page);
        page.navigate(BASE_URL + "/");
        waitForRunning(page);
        page.locator("#jpro-anyZone").waitFor();
    }

    @AfterEach
    void closePage() {
        if (page != null) page.context().close();
        if (browserErrors != null) browserErrors.assertNoErrors();
    }

    @Test
    void anyFilterAcceptsFileWithoutExtensionAndUploads() {
        choose("#jpro-anyZone", "noext");
        awaitText("#jpro-selected", "noext");
        page.locator("#jpro-upload").click();
        awaitText("#jpro-status", "COMPLETED");
        awaitText("#jpro-future", "SUCCESS");
        assertEquals("1.0", text("#jpro-progress"));
        long size = FILES.resolve("noext").toFile().length();
        assertEquals(size + "/" + size, text("#jpro-sizes"));
    }

    @Test
    void pickerIgnoresOtherExtensions() {
        choose("#jpro-pngZone", "photo.jpg");
        page.waitForTimeout(2000);
        assertEquals("", text("#jpro-selected"), "A .jpg must not be selected by a .PNG picker");
        choose("#jpro-pngZone", "cursor.png");
        awaitText("#jpro-selected", "cursor.png");
    }

    @Test
    void dropperIgnoresOtherExtensions() {
        CDPSession cdp = page.context().newCDPSession(page);
        BoundingBox box = page.locator("#jpro-pngZone").boundingBox();
        double x = box.x + box.width / 2, y = box.y + box.height / 2;
        drop(cdp, x, y, FILES.resolve("photo.jpg"));
        page.waitForTimeout(2000);
        assertEquals("", text("#jpro-selected"), "A .jpg must not be dropped on a .PNG dropper");
        drop(cdp, x, y, FILES.resolve("cursor.png"));
        awaitText("#jpro-selected", "cursor.png");
    }

    @Test
    void cancelledUploadCanBeRetried() {
        // Cancelling aborts the client's XHR, which the browser reports as a failed request.
        browserErrors.ignoreMatching(s -> s.contains("net::ERR_ABORTED"));
        choose("#jpro-anyZone", "cursor.png");
        awaitText("#jpro-selected", "cursor.png");
        page.locator("#jpro-uploadCancel").click();
        awaitText("#jpro-status", "CANCELLED");
        awaitText("#jpro-future", "CancellationException");
        assertEquals("0.0", text("#jpro-progress"));
        page.locator("#jpro-upload").click();
        awaitText("#jpro-status", "COMPLETED");
        awaitText("#jpro-future", "SUCCESS");
        assertEquals("1.0", text("#jpro-progress"));
    }

    private void choose(String zone, String filename) {
        Path file = FILES.resolve(filename);
        assertTrue(file.toFile().exists(), "Test file must exist: " + file);
        FileChooser chooser = page.waitForFileChooser(() -> page.locator(zone).click());
        chooser.setFiles(file);
    }

    private static void drop(CDPSession cdp, double x, double y, Path file) {
        for (String type : new String[]{"dragEnter", "dragOver", "drop"}) {
            JsonArray files = new JsonArray();
            files.add(file.toAbsolutePath().toString());
            JsonObject data = new JsonObject();
            data.add("items", new JsonArray());
            data.add("files", files);
            data.addProperty("dragOperationsMask", 1);
            JsonObject params = new JsonObject();
            params.addProperty("type", type);
            params.addProperty("x", x);
            params.addProperty("y", y);
            params.add("data", data);
            cdp.send("Input.dispatchDragEvent", params);
        }
    }

    private String text(String selector) {
        return page.locator(selector).textContent().trim();
    }

    private void awaitText(String selector, String expected) {
        long deadline = System.currentTimeMillis() + 30000;
        String t = "";
        while (System.currentTimeMillis() < deadline) {
            t = text(selector);
            if (t.contains(expected)) return;
            page.waitForTimeout(250);
        }
        fail("Expected '" + selector + "' to contain '" + expected + "' but got: " + t);
    }
}
