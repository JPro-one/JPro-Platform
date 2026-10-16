package one.jpro.platform.playwright;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class JProWaitTest {

    private static BooleanSupplier trueAfter(long ms) {
        long start = System.currentTimeMillis();
        return () -> System.currentTimeMillis() - start >= ms;
    }

    private static final Supplier<String> THROWING_STATE = () -> {
        throw new RuntimeException("diagnostic failed");
    };

    @Test
    void slowSuccessSurvivesThrowingState() {
        assertDoesNotThrow(() -> JProWait.waitUntil(1000, trueAfter(600), THROWING_STATE));
    }

    @Test
    void lateSuccessStillReportsWhenStateThrows() {
        AssertionError err = assertThrows(AssertionError.class, () ->
                JProWait.waitUntil(500, trueAfter(700), THROWING_STATE));
        assertTrue(err.getMessage().contains("exceeding budget"), err.getMessage());
        assertTrue(err.getMessage().contains("diagnostic failed"), err.getMessage());
    }

    @Test
    void neverTrueReportsTheState() {
        AssertionError err = assertThrows(AssertionError.class, () ->
                JProWait.waitUntil(50, () -> false, () -> "still loading"));
        assertTrue(err.getMessage().contains("never true"), err.getMessage());
        assertTrue(err.getMessage().contains("still loading"), err.getMessage());
    }

    @Test
    void waitForValueReportsExpectedAndActual() {
        AssertionError err = assertThrows(AssertionError.class, () ->
                JProWait.waitForValue(50, "done", () -> "pending"));
        assertTrue(err.getMessage().contains("expected <done> but was <pending>"), err.getMessage());
    }

    @Test
    void waitForValuePassesOnceTheValueArrives() {
        AtomicInteger calls = new AtomicInteger();
        assertDoesNotThrow(() -> JProWait.waitForValue(1000, 3, calls::incrementAndGet));
    }

    @Test
    void stableValuePassesForTheWholeDuration() {
        long start = System.currentTimeMillis();
        JProWait.ensureStaysValue(200, "x", () -> "x");
        assertTrue(System.currentTimeMillis() - start >= 200);
    }

    @Test
    void flickerFailsWithTheTimeAndValue() {
        BooleanSupplier flipped = trueAfter(100);
        AssertionError err = assertThrows(AssertionError.class, () ->
                JProWait.ensureStaysValue(500, "a", () -> flipped.getAsBoolean() ? "b" : "a"));
        assertTrue(err.getMessage().contains("stopped holding after"), err.getMessage());
        assertTrue(err.getMessage().contains("expected <a> but was <b>"), err.getMessage());
    }
}
