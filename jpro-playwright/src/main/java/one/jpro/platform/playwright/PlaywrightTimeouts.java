package one.jpro.platform.playwright;

import com.microsoft.playwright.TimeoutError;

import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;

/**
 * Tiered-diagnostic wrapper around Playwright waits. The same 4-tier scheme used across the
 * JPro test suites:
 * <ul>
 *   <li>&lt; 50% of budget: silent</li>
 *   <li>50%–100%: WARN to stdout (close to the edge — the budget may need bumping)</li>
 *   <li>100%–200%: AssertionError "completed after budget" — the wait succeeded but slowly,
 *       which on its own would be a flaky failure of a too-tight budget</li>
 *   <li>&gt; 200%: AssertionError "never completed" (Playwright threw TimeoutError) — looks
 *       like the operation is genuinely stuck rather than just slow</li>
 * </ul>
 */
public final class PlaywrightTimeouts {

    private PlaywrightTimeouts() {}

    /**
     * Run a Playwright wait operation with a tiered diagnostic budget.
     *
     * <p>The {@code operation} receives the actual timeout (in ms) it should hand to
     * Playwright — always 2x the nominal budget — so the underlying wait fails hard
     * at {@code 2 * budget}, and the tier logic interprets the elapsed time afterwards.
     *
     * <p>Example:
     * <pre>{@code
     * withTimeout(15, TimeUnit.SECONDS, "popup marker visible", t ->
     *     popup.locator("#jpro-popup-marker").waitFor(
     *         new Locator.WaitForOptions().setState(ATTACHED).setTimeout(t)));
     * }</pre>
     *
     * @param budget    nominal time budget the operation is expected to complete within
     * @param unit      time unit for {@code budget}
     * @param label     short description used in WARN/error output ("popup marker visible")
     * @param operation receives the doubled timeout (ms) to pass into the Playwright call
     */
    public static void withTimeout(long budget, TimeUnit unit, String label, LongConsumer operation) {
        long budgetMs = unit.toMillis(budget);
        long doubleBudgetMs = budgetMs * 2;
        long startMs = System.currentTimeMillis();
        try {
            operation.accept(doubleBudgetMs);
            long elapsedMs = System.currentTimeMillis() - startMs;
            if (elapsedMs < budgetMs / 2) {
                // < 50%: silent
            } else if (elapsedMs < budgetMs) {
                // 50%-100%: warn
                System.out.println("WARNING: " + label + " took " + elapsedMs + "ms of "
                        + budgetMs + "ms budget.");
            } else {
                // 100%-200%: completed, but past the nominal budget
                throw new AssertionError(label + " completed after " + elapsedMs
                        + "ms, exceeding budget of " + budgetMs + "ms (timeout likely too tight).");
            }
        } catch (TimeoutError e) {
            // > 200%: Playwright gave up at 2x the budget — the operation never completed
            long elapsedMs = System.currentTimeMillis() - startMs;
            throw new AssertionError(label + " never completed (Playwright timed out after "
                    + elapsedMs + "ms, 2x budget of " + budgetMs + "ms).", e);
        }
    }
}
