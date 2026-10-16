package one.jpro.platform.playwright;

import com.microsoft.playwright.TimeoutError;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;

import static org.junit.jupiter.api.Assertions.*;

class PlaywrightTimeoutsTest {

    private final List<Long> timeouts = new ArrayList<>();

    /** A wait that times out (after its full timeout, like Playwright) until call number {@code call}. */
    private LongConsumer succeedsOnCall(int call, Throwable timeout) {
        return t -> {
            timeouts.add(t);
            if (timeouts.size() < call) {
                sleep(t);
                if (timeout instanceof Error e) throw e;
                throw (RuntimeException) timeout;
            }
        };
    }

    private LongConsumer succeedsOnCall(int call) {
        return succeedsOnCall(call, new TimeoutError("timed out"));
    }

    @Test
    void firstTierIsSilent() {
        assertDoesNotThrow(() -> PlaywrightTimeouts.withTimeout(100, TimeUnit.MILLISECONDS, "op", succeedsOnCall(1)));
        assertEquals(List.of(50L), timeouts);
    }

    @Test
    void secondTierOnlyWarns() {
        assertDoesNotThrow(() -> PlaywrightTimeouts.withTimeout(100, TimeUnit.MILLISECONDS, "op", succeedsOnCall(2)));
        assertEquals(List.of(50L, 50L), timeouts);
    }

    @Test
    void thirdTierFailsAsTooSlow() {
        AssertionError e = assertThrows(AssertionError.class, () ->
                PlaywrightTimeouts.withTimeout(100, TimeUnit.MILLISECONDS, "op", succeedsOnCall(3)));
        assertTrue(e.getMessage().contains("completed after"), e.getMessage());
        assertEquals(List.of(50L, 50L, 100L), timeouts);
    }

    @Test
    void neverCompletingFails() {
        AssertionError e = assertThrows(AssertionError.class, () ->
                PlaywrightTimeouts.withTimeout(100, TimeUnit.MILLISECONDS, "op", succeedsOnCall(4)));
        assertTrue(e.getMessage().contains("never completed"), e.getMessage());
        assertInstanceOf(TimeoutError.class, e.getCause());
        assertEquals(3, timeouts.size());
    }

    @Test
    void timedOutWebFirstAssertionIsRetried() {
        assertDoesNotThrow(() -> PlaywrightTimeouts.withTimeout(100, TimeUnit.MILLISECONDS, "op",
                succeedsOnCall(2, new org.opentest4j.AssertionFailedError("Locator expected to have text"))));
        assertEquals(2, timeouts.size());
    }

    @Test
    void tinyBudgetNeverPassesZeroWhichPlaywrightTreatsAsNoTimeout() {
        assertThrows(AssertionError.class, () ->
                PlaywrightTimeouts.withTimeout(1, TimeUnit.MILLISECONDS, "op", succeedsOnCall(4)));
        assertEquals(List.of(1L, 1L, 1L), timeouts);
    }

    @Test
    void failureBeforeTheTimeoutIsNotRetried() {
        AssertionError cause = new AssertionError("wrong value");
        AssertionError e = assertThrows(AssertionError.class, () ->
                PlaywrightTimeouts.withTimeout(100, TimeUnit.MILLISECONDS, "op", t -> { timeouts.add(t); throw cause; }));
        assertSame(cause, e);
        assertEquals(1, timeouts.size());
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
