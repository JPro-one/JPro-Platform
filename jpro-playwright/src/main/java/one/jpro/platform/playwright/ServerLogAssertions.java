package one.jpro.platform.playwright;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Scans JPro server log files for error / throwable indicators and fails the caller with an
 * AssertionError if any are found. Intended to be called from tests after the server has been
 * stopped (so logs are fully flushed).
 */
public final class ServerLogAssertions {

    private ServerLogAssertions() {}

    /**
     * Keyword and stack-frame patterns that indicate an error or thrown Throwable in a log line.
     * Case-insensitive for the keywords. {@code \w*NAME\b} matches CamelCase class names such as
     * {@code IOException} / {@code AssertionError} where a strict {@code \b\b} pattern wouldn't
     * (no word boundary between the leading part and the keyword).
     */
    public static final List<Pattern> ERROR_PATTERNS = List.of(
            Pattern.compile("(?i)\\bcaused by\\b"),
            Pattern.compile("(?i)\\w*throwable\\b"),
            Pattern.compile("(?i)\\w*exception\\b"),
            Pattern.compile("(?i)\\w*error\\b"),
            // Stack-frame suffix — covers cases where a frame line carries no keyword but the
            // Foo.java:N) / Bar.scala:N) / Baz.kt:N) signature is unambiguous.
            Pattern.compile("\\b\\w+\\.(?:java|scala|kt):\\d+\\)")
    );

    /**
     * Marker the server prints once startup has completed successfully. Used as the sanity
     * signal that {@code logsDir} really is the current run's log directory.
     */
    static final String SERVER_STARTED_MARKER = "Server started";

    /**
     * Asserts that the log files under {@code logsDir} don't contain any errors, exceptions
     * or throwables. {@code jpro.activity.log} is skipped (intentional audit stream).
     *
     * <p>Also performs a sanity check: {@code logsDir} must exist and at least one log file
     * must contain the {@value #SERVER_STARTED_MARKER} marker. Without this, a missing /
     * empty / stale log dir would let the assertion pass trivially even if the server
     * never actually reached running state (e.g. a wrong path, or an earlier run's leftover).
     *
     * @throws AssertionError if the sanity check fails or any log file contains a matching line.
     */
    public static void assertNoServerErrors(File logsDir) {
        assertNoServerErrors(logsDir, true);
    }

    /**
     * Pass {@code requireServerStarted = false} for tests that intentionally exercise a
     * startup path which exits before {@link #SERVER_STARTED_MARKER} is ever emitted.
     */
    public static void assertNoServerErrors(File logsDir, boolean requireServerStarted) {
        if (!logsDir.isDirectory()) {
            throw new AssertionError("Expected server log directory does not exist: " + logsDir);
        }
        File[] allLogs = logsDir.listFiles((dir, name) -> name.endsWith(".log"));
        if (allLogs == null || allLogs.length == 0) {
            throw new AssertionError("No .log files under " + logsDir
                    + " — did the server actually run?");
        }

        List<String> problems = new ArrayList<>();
        boolean sawServerStarted = false;
        for (File logFile : allLogs) {
            List<String> lines = readLines(logFile);
            if (!sawServerStarted && lines.stream().anyMatch(l -> l.contains(SERVER_STARTED_MARKER))) {
                sawServerStarted = true;
            }
            // activity.log counts for the marker lookup above but is skipped by the error scan.
            if (logFile.getName().equals("jpro.activity.log")) continue;
            List<String> matches = lines.stream()
                    .filter(ServerLogAssertions::lineLooksLikeError)
                    .toList();
            if (!matches.isEmpty()) {
                problems.add(logFile.getName() + " contains " + matches.size()
                        + " error/throwable line(s):\n" + String.join("\n", matches));
            }
        }

        if (requireServerStarted && !sawServerStarted) {
            throw new AssertionError("No '" + SERVER_STARTED_MARKER + "' marker found under "
                    + logsDir + " — wrong path, or the server did not reach running state.");
        }

        if (!problems.isEmpty()) {
            throw new AssertionError("Server log contains errors or throwables:\n\n"
                    + String.join("\n\n", problems));
        }
    }

    private static boolean lineLooksLikeError(String line) {
        for (Pattern p : ERROR_PATTERNS) {
            if (p.matcher(line).find()) return true;
        }
        return false;
    }

    /**
     * Prints every {@code .log} file under {@code logsDir} to {@code System.out},
     * skipping {@code jpro.activity.log}. A detached JPro server has no live stdout to
     * inherit, so dumping the files is the only way to surface what it actually logged.
     *
     * <p>Intended to be called from a {@code finally} around server shutdown so logs
     * appear on both success and failure — never throws.
     */
    public static void dumpServerLogs(File logsDir) {
        if (logsDir == null || !logsDir.isDirectory()) {
            System.out.println("[no server logs to dump under " + logsDir + "]");
            return;
        }
        File[] logs = logsDir.listFiles((dir, name) -> name.endsWith(".log"));
        if (logs == null || logs.length == 0) {
            System.out.println("[no .log files under " + logsDir + "]");
            return;
        }
        Arrays.sort(logs, Comparator.comparing(File::getName));
        for (File logFile : logs) {
            if (logFile.getName().equals("jpro.activity.log")) continue;
            System.out.println("\n===== " + logFile.getName() + " =====");
            try {
                for (String line : Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8)) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                System.out.println("[could not read " + logFile + ": " + e + "]");
            }
            System.out.println("===== end " + logFile.getName() + " =====");
        }
    }

    private static List<String> readLines(File logFile) {
        try {
            return Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not read " + logFile, e);
        }
    }
}
