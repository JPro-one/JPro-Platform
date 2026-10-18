package one.jpro.platform.file;

import com.microsoft.playwright.FileChooser;
import com.microsoft.playwright.Page;
import one.jpro.platform.playwright.BrowserErrorCollector;
import one.jpro.platform.playwright.CdpFileDrop;
import one.jpro.platform.playwright.JProPlaywrightTest;
import one.jpro.platform.playwright.JProWait;
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
        page.locator(".jpro-id-anyZone").waitFor();
    }

    @AfterEach
    void closePage() {
        if (page != null) page.context().close();
        if (browserErrors != null) browserErrors.assertNoErrors();
    }

    @Test
    void anyFilterAcceptsFileWithoutExtensionAndUploads() {
        choose(".jpro-id-anyZone", "noext");
        awaitText(".jpro-id-selected", "noext");
        page.locator(".jpro-id-upload").click();
        awaitText(".jpro-id-status", "COMPLETED");
        awaitText(".jpro-id-future", "SUCCESS");
        assertEquals("1.0", text(".jpro-id-progress"));
        long size = FILES.resolve("noext").toFile().length();
        assertEquals(size + "/" + size, text(".jpro-id-sizes"));
    }

    @Test
    void directoryPickerIsUnsupportedInTheBrowser() {
        assertEquals("UnsupportedOperationException", text(".jpro-id-directoryPicker"));
    }

    @Test
    void pickerIgnoresOtherExtensions() {
        choose(".jpro-id-pngZone", "photo.jpg");
        JProWait.ensureStaysValue(2000, "", () -> text(".jpro-id-selected"));
        choose(".jpro-id-pngZone", "cursor.png");
        awaitText(".jpro-id-selected", "cursor.png");
    }

    @Test
    void dropperIgnoresOtherExtensions() {
        CdpFileDrop.drop(page.locator(".jpro-id-pngZone"), FILES.resolve("photo.jpg"));
        JProWait.ensureStaysValue(2000, "", () -> text(".jpro-id-selected"));
        CdpFileDrop.drop(page.locator(".jpro-id-pngZone"), FILES.resolve("cursor.png"));
        awaitText(".jpro-id-selected", "cursor.png");
    }

    @Test
    void cancelledUploadCanBeRetried() {
        // Cancelling aborts the client's XHR, which the browser reports as a failed request.
        browserErrors.ignoreMatching(s -> s.contains("net::ERR_ABORTED"));
        choose(".jpro-id-anyZone", "cursor.png");
        awaitText(".jpro-id-selected", "cursor.png");
        page.locator(".jpro-id-uploadCancel").click();
        awaitText(".jpro-id-status", "CANCELLED");
        awaitText(".jpro-id-future", "CancellationException");
        assertEquals("0.0", text(".jpro-id-progress"));
        page.locator(".jpro-id-upload").click();
        awaitText(".jpro-id-status", "COMPLETED");
        awaitText(".jpro-id-future", "SUCCESS");
        assertEquals("1.0", text(".jpro-id-progress"));
    }

    private void choose(String zone, String filename) {
        Path file = FILES.resolve(filename);
        assertTrue(file.toFile().exists(), "Test file must exist: " + file);
        FileChooser chooser = page.waitForFileChooser(() -> page.locator(zone).click());
        chooser.setFiles(file);
    }

    private String text(String selector) {
        return page.locator(selector).textContent().trim();
    }

    private void awaitText(String selector, String expected) {
        JProWait.waitUntil(15_000, () -> text(selector).contains(expected),
                () -> "'" + selector + "' should contain '" + expected + "' but is '" + text(selector) + "'");
    }
}
