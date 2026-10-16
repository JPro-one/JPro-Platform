package one.jpro.platform.playwright;

import com.microsoft.playwright.TimeoutError;

import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;

/**
 * Tiered-diagnostic wrapper around Playwright waits. Same 4-tier scheme as {@link JProWait}:
 * <ul>
 *   <li>&lt; 50% of budget: silent</li>
 *   <li>50%–100%: WARN to stdout (close to the edge — the budget may need bumping)</li>
 *   <li>100%–200%: AssertionError "completed after budget" — the wait succeeded but slowly,
 *       which on its own would be a flaky failure of a too-tight budget</li>
 *   <li>&gt; 200%: AssertionError "never completed" — looks like the operation is genuinely
 *       stuck rather than just slow</li>
 * </ul>
 */
public final class PlaywrightTimeouts {

    private PlaywrightTimeouts() {}

    /**
     * Run a Playwright wait operation with a tiered diagnostic budget.
     *
     * <p>The {@code operation} is called once per tier, with that tier's timeout (in ms) to hand
     * to Playwright: 50%, 50%, then 100% of the budget. A call that times out — a Playwright
     * {@link TimeoutError}, or an {@link AssertionError} from a web-first {@code assertThat} —
     * moves on to the next tier, so the operation must be safe to repeat. A failure well before the
     * tier's timeout ran out is not a timeout and is rethrown as is.
     *
     * <p>Example:
     * <pre>{@code
     * withTimeout(15, TimeUnit.SECONDS, "popup marker visible", t ->
     *     popup.locator(".jpro-id-popup-marker").waitFor(
     *         new Locator.WaitForOptions().setState(ATTACHED).setTimeout(t)));
     * }</pre>
     *
     * @param budget    nominal time budget the operation is expected to complete within
     * @param unit      time unit for {@code budget}
     * @param label     short description used in WARN/error output ("popup marker visible")
     * @param operation receives the current tier's timeout (ms) to pass into the Playwright call
     */
    public static void withTimeout(long budget, TimeUnit unit, String label, LongConsumer operation) {
        long budgetMs = unit.toMillis(budget);
        // At least 1ms per tier: Playwright treats a timeout of 0 as "no timeout".
        long[] tiers = {Math.max(1, budgetMs / 2), Math.max(1, budgetMs - budgetMs / 2), Math.max(1, budgetMs)};
        long startMs = System.currentTimeMillis();
        for (int tier = 0; ; tier++) {
            long tierStartNs = System.nanoTime();
            try {
                operation.accept(tiers[tier]);
            } catch (TimeoutError | AssertionError e) {
                // 10% slack: Playwright's timer may fire slightly before ours measures the full tier.
                if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - tierStartNs) < tiers[tier] * 9 / 10) throw e;
                if (tier < tiers.length - 1) continue;
                long elapsedMs = System.currentTimeMillis() - startMs;
                throw new AssertionError(label + " never completed (timed out after "
                        + elapsedMs + "ms, 2x budget of " + budgetMs + "ms).", e);
            }
            long elapsedMs = System.currentTimeMillis() - startMs;
            if (tier == 1) {
                System.out.println("WARNING: " + label + " took " + elapsedMs + "ms of "
                        + budgetMs + "ms budget.");
            } else if (tier == 2) {
                throw new AssertionError(label + " completed after " + elapsedMs
                        + "ms, exceeding budget of " + budgetMs + "ms (timeout likely too tight).");
            }
            return;
        }
    }
}
