package one.jpro.platform.playwright;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ServerLogAssertionsTest {

    @TempDir
    Path logsDir;

    private static final String STARTED = "[INFO] Server started at http://localhost:8090\n";

    private void writeLog(String name, String content) throws Exception {
        Files.writeString(logsDir.resolve(name), content);
    }

    @Test
    void failsOnEmptyDir() {
        AssertionError err = assertThrows(AssertionError.class,
                () -> ServerLogAssertions.assertNoServerErrors(logsDir.toFile()));
        assertTrue(err.getMessage().contains("did the server actually run"),
                "Actual: " + err.getMessage());
    }

    @Test
    void failsOnMissingDir() {
        File missing = new File(logsDir.toFile(), "does-not-exist");
        AssertionError err = assertThrows(AssertionError.class,
                () -> ServerLogAssertions.assertNoServerErrors(missing));
        assertTrue(err.getMessage().contains("does not exist"),
                "Actual: " + err.getMessage());
    }

    @Test
    void failsOnLogsWithoutStartedMarker() throws Exception {
        writeLog("jpro.info.log", "[INFO] something happened but no marker\n");
        AssertionError err = assertThrows(AssertionError.class,
                () -> ServerLogAssertions.assertNoServerErrors(logsDir.toFile()));
        assertTrue(err.getMessage().contains("Server started"),
                "Actual: " + err.getMessage());
    }

    @Test
    void passesOnCleanLogs() throws Exception {
        writeLog("jpro.info.log",
                STARTED +
                "2026-04-20 [INFO] JavaFX jar: ...\n" +
                "2026-04-20 [INFO] Listening on port 8090\n");
        ServerLogAssertions.assertNoServerErrors(logsDir.toFile());
    }

    @Test
    void failsOnErrorLevel() throws Exception {
        writeLog("jpro.info.log", STARTED + "2026-04-20 [ERROR] something bad\n");
        AssertionError err = assertThrows(AssertionError.class,
                () -> ServerLogAssertions.assertNoServerErrors(logsDir.toFile()));
        assertTrue(err.getMessage().contains("jpro.info.log"));
        assertTrue(err.getMessage().contains("ERROR"));
    }

    @Test
    void failsOnCamelCaseExceptionClass() throws Exception {
        writeLog("jpro.info.log", STARTED);
        writeLog("jpro.warn.log", "java.io.IOException: Pipe closed\n");
        AssertionError err = assertThrows(AssertionError.class,
                () -> ServerLogAssertions.assertNoServerErrors(logsDir.toFile()));
        assertTrue(err.getMessage().contains("IOException"));
    }

    @Test
    void failsOnStackFrame() throws Exception {
        writeLog("jpro.console.log",
                STARTED + "[INFO] foo\n\tat com.example.Bar.baz(Bar.java:42)\n");
        AssertionError err = assertThrows(AssertionError.class,
                () -> ServerLogAssertions.assertNoServerErrors(logsDir.toFile()));
        assertTrue(err.getMessage().contains("Bar.java:42"));
    }

    @Test
    void failsOnCausedBy() throws Exception {
        writeLog("jpro.info.log", STARTED + "Caused by: java.lang.RuntimeException: boom\n");
        AssertionError err = assertThrows(AssertionError.class,
                () -> ServerLogAssertions.assertNoServerErrors(logsDir.toFile()));
        assertTrue(err.getMessage().toLowerCase().contains("caused by"));
    }

    @Test
    void activityLogCountsForMarkerButIsNotScanned() throws Exception {
        writeLog("jpro.activity.log", STARTED + "2026-04-20 [ERROR] audit noise\n");
        ServerLogAssertions.assertNoServerErrors(logsDir.toFile());
    }

    @Test
    void scansOnlyDotLogFiles() throws Exception {
        writeLog("jpro.info.log", STARTED + "[INFO] clean\n");
        writeLog("notes.txt", "java.lang.NullPointerException\n");
        ServerLogAssertions.assertNoServerErrors(logsDir.toFile());
    }

    @Test
    void dumpServerLogs_dumpsAllExceptActivity() throws Exception {
        writeLog("jpro.info.log", "[INFO] info-payload-marker\n");
        writeLog("jpro.console.log", "[INFO] console-payload-marker\n");
        writeLog("jpro.activity.log", "[INFO] activity-payload-marker\n");

        String captured = captureStdout(() ->
                ServerLogAssertions.dumpServerLogs(logsDir.toFile()));

        assertTrue(captured.contains("info-payload-marker"),
                "info.log content should be dumped. Captured: " + captured);
        assertTrue(captured.contains("console-payload-marker"),
                "console.log content should be dumped. Captured: " + captured);
        assertFalse(captured.contains("activity-payload-marker"),
                "activity.log should be skipped. Captured: " + captured);
    }

    @Test
    void dumpServerLogs_neverThrowsOnMissingDir() {
        File missing = new File(logsDir.toFile(), "does-not-exist");
        assertDoesNotThrow(() -> ServerLogAssertions.dumpServerLogs(missing));
    }

    @Test
    void dumpServerLogs_neverThrowsOnEmptyDir() {
        assertDoesNotThrow(() -> ServerLogAssertions.dumpServerLogs(logsDir.toFile()));
    }

    @Test
    void dumpServerLogs_neverThrowsOnNull() {
        assertDoesNotThrow(() -> ServerLogAssertions.dumpServerLogs(null));
    }

    @Test
    void collectsMatchesAcrossMultipleFiles() throws Exception {
        writeLog("jpro.info.log", STARTED + "[INFO] normal\n[ERROR] boom\n");
        writeLog("jpro.warn.log", "java.lang.IllegalStateException: x\n");
        AssertionError err = assertThrows(AssertionError.class,
                () -> ServerLogAssertions.assertNoServerErrors(logsDir.toFile()));
        assertTrue(err.getMessage().contains("jpro.info.log"));
        assertTrue(err.getMessage().contains("jpro.warn.log"));
    }

    private String captureStdout(Runnable body) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
        try {
            body.run();
        } finally {
            System.setOut(original);
        }
        return buf.toString(StandardCharsets.UTF_8);
    }
}
