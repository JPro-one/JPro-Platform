package one.jpro.platform.playwright;

import com.microsoft.playwright.Page;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Condition-based waits for Playwright tests, as a replacement for fixed {@code page.waitForTimeout(...)}
 * sleeps. Waits poll every 10ms against a budget with the same 4 tiers as {@link PlaywrightTimeouts}:
 * <ul>
 *   <li>&lt; 50% of budget: silent</li>
 *   <li>50%–100%: WARN to stdout</li>
 *   <li>100%–200%: AssertionError — the condition became true, but after the budget</li>
 *   <li>&gt; 200%: AssertionError — the condition was never true</li>
 * </ul>
 * Failures include a description of the current state, so a headless run still says what it saw.
 */
public final class JProWait {

    /** Default budget for a UI state to settle after an interaction. */
    public static final long SETTLE_BUDGET_MS = 5000;

    private static final long POLL_MS = 10;

    private JProWait() {}

    /** Polls until {@code condition} holds, with the {@link #SETTLE_BUDGET_MS} budget. */
    public static void waitUntil(BooleanSupplier condition, Supplier<String> state) {
        waitUntil(SETTLE_BUDGET_MS, condition, state);
    }

    /**
     * Polls until {@code condition} holds.
     *
     * @param budgetMs  the time the condition is expected to become true within
     * @param condition the condition to wait for
     * @param state     describes the current state for failure and warning messages
     */
    public static void waitUntil(long budgetMs, BooleanSupplier condition, Supplier<String> state) {
        long startMs = System.currentTimeMillis();
        while (true) {
            if (condition.getAsBoolean()) {
                long elapsedMs = System.currentTimeMillis() - startMs;
                if (elapsedMs < budgetMs / 2) {
                    return;
                } else if (elapsedMs < budgetMs) {
                    System.out.println("WARNING: wait took " + elapsedMs + "ms of " + budgetMs
                            + "ms budget. State: " + describe(state));
                    return;
                }
                throw new AssertionError("Condition became true after " + elapsedMs + "ms, exceeding budget of "
                        + budgetMs + "ms. State: " + describe(state));
            }
            long elapsedMs = System.currentTimeMillis() - startMs;
            if (elapsedMs >= budgetMs * 2) {
                throw new AssertionError("Condition was never true (waited " + elapsedMs + "ms, 2x budget of "
                        + budgetMs + "ms). State: " + describe(state));
            }
            sleep();
        }
    }

    /** Polls until {@code actual} returns {@code expected}, with the {@link #SETTLE_BUDGET_MS} budget. */
    public static <T> void waitForValue(T expected, Supplier<T> actual) {
        waitForValue(SETTLE_BUDGET_MS, expected, actual);
    }

    /** Polls until {@code actual} returns {@code expected}. */
    public static <T> void waitForValue(long budgetMs, T expected, Supplier<T> actual) {
        AtomicReference<T> seen = new AtomicReference<>();
        waitUntil(budgetMs, () -> {
            seen.set(actual.get());
            return Objects.equals(expected, seen.get());
        }, () -> "expected <" + expected + "> but was <" + seen.get() + ">");
    }

    /**
     * Polls for {@code durationMs} and fails as soon as {@code condition} stops holding — the
     * counterpart of {@link #waitUntil}, for asserting that something does <em>not</em> change.
     */
    public static void ensureStaysTrue(long durationMs, BooleanSupplier condition, Supplier<String> state) {
        long startMs = System.currentTimeMillis();
        while (true) {
            if (!condition.getAsBoolean()) {
                long elapsedMs = System.currentTimeMillis() - startMs;
                throw new AssertionError("Condition stopped holding after " + elapsedMs + "ms of "
                        + durationMs + "ms. State: " + describe(state));
            }
            if (System.currentTimeMillis() - startMs >= durationMs) return;
            sleep();
        }
    }

    /** Polls for {@code durationMs} that {@code actual} keeps returning {@code expected}. */
    public static <T> void ensureStaysValue(long durationMs, T expected, Supplier<T> actual) {
        AtomicReference<T> seen = new AtomicReference<>();
        ensureStaysTrue(durationMs, () -> {
            seen.set(actual.get());
            return Objects.equals(expected, seen.get());
        }, () -> "expected <" + expected + "> but was <" + seen.get() + ">");
    }

    /** {@code page.waitForFunction} with a tiered budget (see {@link PlaywrightTimeouts}). */
    public static void waitForFunction(Page page, long budgetMs, String label, String expression, Object arg) {
        PlaywrightTimeouts.withTimeout(budgetMs, TimeUnit.MILLISECONDS, label, t ->
                page.waitForFunction(expression, arg, new Page.WaitForFunctionOptions().setTimeout(t)));
    }

    private static String describe(Supplier<String> state) {
        try {
            return state.get();
        } catch (Exception e) {
            return "(could not evaluate: " + e.getMessage() + ")";
        }
    }

    private static void sleep() {
        try {
            TimeUnit.MILLISECONDS.sleep(POLL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
