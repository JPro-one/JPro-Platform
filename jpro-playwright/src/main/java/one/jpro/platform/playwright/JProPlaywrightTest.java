package one.jpro.platform.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Base class for Playwright tests that drive a JPro application in a headless browser.
 *
 * <p>Provides the boilerplate every JPro Playwright test needs:
 * <ul>
 *   <li>A shared headless Chromium ({@link #setupBrowser()} / {@link #teardownBrowser()}).</li>
 *   <li>A {@link #PORT} (from {@code -Djpro.test.port} or a free port in 9100–9200) and the
 *       derived {@link #BASE_URL}.</li>
 *   <li>{@link #startServer(String, String...)} / {@link #stopServer(String)} that launch and
 *       stop a JPro server via its Gradle {@code jproStart} / {@code jproStop} tasks, and
 *       {@link #stopAndAssertNoServerErrors(String, File)} that also fails the test on any
 *       exception in the server logs.</li>
 *   <li>{@link #waitForRunning(Page)} — wait until the {@code jpro-app} element reports
 *       {@code data-status="running"} before interacting.</li>
 * </ul>
 *
 * <p>For typing and reading text back, see {@link JProInput}. Selecting nodes requires
 * {@code jpro.mirrorCSSToDOM = true} in the app's {@code jpro.conf} — then a node with
 * {@code setId("foo")} is reachable as {@code "#jpro-foo"}.
 */
public abstract class JProPlaywrightTest {

    protected static final int PORT = resolvePort();
    protected static final String BASE_URL = "http://localhost:" + PORT;

    private static final File PROJECT_ROOT = findProjectRoot();

    protected static Playwright playwright;
    protected static Browser browser;

    @BeforeAll
    static void setupBrowser() {
        playwright = Playwright.create();
        boolean headless = !"false".equals(System.getProperty("jpro.test.headless"));
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(headless));
    }

    @AfterAll
    static void teardownBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    /** A fresh page in its own browser context. */
    protected Page newPage() {
        return browser.newContext().newPage();
    }

    /** Repository root (directory containing {@code gradlew}). */
    protected static File projectRoot() {
        return PROJECT_ROOT;
    }

    /**
     * Capture a full-page PNG of the rendered app and return its absolute path (also logged).
     * Since tests run headless, this is usually the only way to actually look at the app — call
     * it in a {@code catch}/teardown to snapshot a failure. The directory defaults to
     * {@code build/playwright-screenshots} and is overridable with {@code -Djpro.test.screenshotDir}.
     */
    protected static Path screenshot(Page page, String name) {
        Path path = screenshotPath(name);
        page.screenshot(new Page.ScreenshotOptions().setPath(path).setFullPage(true));
        return logScreenshot(path);
    }

    /**
     * Capture a PNG of a single element (e.g. {@code screenshot(page.locator("#jpro-textfield"),
     * "field")}) and return its absolute path. Same output directory as {@link #screenshot(Page, String)}.
     */
    protected static Path screenshot(Locator locator, String name) {
        Path path = screenshotPath(name);
        locator.screenshot(new Locator.ScreenshotOptions().setPath(path));
        return logScreenshot(path);
    }

    private static Path screenshotPath(String name) {
        String dir = System.getProperty("jpro.test.screenshotDir", "build/playwright-screenshots");
        return Path.of(dir, name + ".png");
    }

    private static Path logScreenshot(Path path) {
        Path absolute = path.toAbsolutePath();
        System.out.println("[jpro-screenshot] " + absolute);
        return absolute;
    }

    /** Wait for the {@code jpro-app} element to report {@code data-status="running"}. */
    protected void waitForRunning(Page page) {
        PlaywrightTimeouts.withTimeout(30, TimeUnit.SECONDS, "jpro-app reaches running", t ->
                page.locator("jpro-app[data-status='running']").waitFor(
                        new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED).setTimeout(t)));
    }

    // ---- Server lifecycle helpers ----

    /**
     * Start a JPro server by running an arbitrary {@code command} (build-tool agnostic) from the
     * project root, clearing {@code logsDir} first and blocking until {@code /status/alive} answers
     * 200. Build the command with {@link #gradleCommand}, or pass your own (Maven, Docker, ...).
     *
     * @param logsDir the server's log directory, cleared before start (e.g. {@code <module>/logs})
     * @param command the full start command, including the port (the helpers add it for you)
     */
    protected static void startServer(File logsDir, String... command)
            throws IOException, InterruptedException {
        if (logsDir != null) deleteRecursively(logsDir);

        System.out.println("[jpro-server] Starting " + String.join(" ", command) + " ...");
        int exit = runCommand(command);
        if (exit != 0) {
            throw new RuntimeException("Failed to start JPro server (exit code " + exit + ")");
        }
        waitForServerReady(60_000);
        System.out.println("[jpro-server] Server is ready on port " + PORT);
    }

    /** Stop a JPro server by running an arbitrary {@code command} from the project root. */
    protected static void stopServer(String... command) throws IOException, InterruptedException {
        System.out.println("[jpro-server] Stopping " + String.join(" ", command) + " ...");
        int exit = runCommand(command);
        if (exit != 0) {
            System.err.println("[jpro-server] Warning: stop command exited with code " + exit);
        }
        System.out.println("[jpro-server] Server stopped.");
    }

    /**
     * Stop the server, dump its logs, then assert the logs contain no errors. The dump runs in a
     * {@code finally} so logs appear on CI even if the stop command throws or the assertion fails.
     *
     * @param logsDir     the server's log directory (typically {@code <module>/logs})
     * @param stopCommand the full stop command (see {@link #gradleCommand})
     */
    protected static void stopAndAssertNoServerErrors(File logsDir, String... stopCommand)
            throws IOException, InterruptedException {
        try {
            stopServer(stopCommand);
        } finally {
            ServerLogAssertions.dumpServerLogs(logsDir);
        }
        ServerLogAssertions.assertNoServerErrors(logsDir);
    }

    /**
     * Build a Gradle command for {@code task} using the project's {@code gradlew} wrapper, passing
     * the test port as {@code -Pjpro.test.port=<PORT>}. Extra args are appended. For Maven or any
     * other tool, pass your own command to {@link #startServer} (include {@code -Djpro.test.port}).
     */
    protected static String[] gradleCommand(String task, String... extraArgs) {
        String wrapper = isWindows() ? "gradlew.bat" : "gradlew";
        String gradlew = new File(PROJECT_ROOT, wrapper).getAbsolutePath();
        List<String> cmd = new ArrayList<>(List.of(gradlew, task, "-Pjpro.test.port=" + PORT));
        cmd.addAll(Arrays.asList(extraArgs));
        return cmd.toArray(new String[0]);
    }

    private static int runCommand(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .directory(PROJECT_ROOT)
                .inheritIO()
                .start();
        return process.waitFor();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static void waitForServerReady(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                HttpURLConnection conn = (HttpURLConnection)
                        URI.create(BASE_URL + "/status/alive").toURL().openConnection();
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);
                if (conn.getResponseCode() == 200) return;
            } catch (IOException ignored) {
                // server not up yet
            }
            Thread.sleep(500);
        }
        throw new RuntimeException("JPro server did not become ready within " + timeoutMs + "ms");
    }

    private static int resolvePort() {
        Integer explicit = Integer.getInteger("jpro.test.port");
        if (explicit != null) return explicit;

        int minPort = 9100;
        int maxPort = 9200;
        int range = maxPort - minPort + 1;
        int offset = ThreadLocalRandom.current().nextInt(range);
        for (int i = 0; i < range; i++) {
            int port = minPort + (offset + i) % range;
            if (isPortFree(port)) return port;
        }
        throw new RuntimeException("No free port found in range " + minPort + "-" + maxPort);
    }

    private static boolean isPortFree(int port) {
        try (ServerSocket ss = new ServerSocket()) {
            ss.setReuseAddress(false);
            ss.bind(new InetSocketAddress("localhost", port));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void deleteRecursively(File f) throws IOException {
        if (!f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursively(c);
            }
        }
        if (!f.delete()) throw new IOException("Could not delete " + f);
    }

    private static File findProjectRoot() {
        File dir = new File(System.getProperty("user.dir"));
        while (dir != null) {
            if (new File(dir, "gradlew").exists()) return dir;
            dir = dir.getParentFile();
        }
        return new File(System.getProperty("user.dir"));
    }
}
