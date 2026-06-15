package one.jpro.platform.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Helpers for simulating keyboard input against a JPro application and reading the
 * result back, encoding the timing rules that make typing into JPro reliable.
 *
 * <p><b>Why this exists.</b> A JavaFX text control rendered by JPro is <i>not</i> a
 * native DOM {@code <input>}. When the control gains focus, JPro wires up a hidden
 * input element in the browser over the WebSocket and routes keystrokes from it to
 * the JavaFX node on the server. Two consequences follow, and missing either one
 * makes tests fail in confusing, flaky ways:
 *
 * <ol>
 *   <li><b>You must focus first and wait.</b> Click (or otherwise focus) the field,
 *       then wait for the hidden-input wiring to settle <i>before</i> typing.
 *       Keystrokes sent during that window are dropped. {@link #typeInto} bakes in
 *       this click → settle → type sequence.</li>
 *   <li><b>You must drive the real keyboard, not the DOM.</b> Use {@link Page#keyboard()}
 *       ({@code type}/{@code press}). Playwright's {@code Locator.fill()} sets a DOM
 *       value directly — for a JPro control that value never reaches JavaFX, so the
 *       text silently never appears on the server.</li>
 * </ol>
 *
 * <p>Every input is also a full browser → WebSocket → JVM → re-render → browser
 * round-trip, so the typed text appears on the JavaFX side only after a variable
 * delay. Never assert immediately — use {@link #awaitText} to poll for the expected
 * value.
 *
 * <p>Selecting the field: with {@code jpro.mirrorCSSToDOM = true} a JavaFX node with
 * {@code setId("foo")} is reachable as the CSS selector {@code "#jpro-foo"} (the
 * {@code jpro-} prefix is added by JPro). See the module README.
 */
public final class JProInput {

    private JProInput() {}

    /**
     * Time (ms) to wait after focusing a field before typing, so JPro's hidden input
     * is wired up. 300ms is the value proven across the JPro test suites; bump it via
     * {@link #typeInto(Page, String, String, double)} for slow/loaded environments.
     */
    public static final double DEFAULT_FOCUS_SETTLE_MS = 300;

    /** Default number of polls {@link #awaitText} performs while waiting for a round-trip. */
    public static final int DEFAULT_POLL_ATTEMPTS = 20;

    /** Default delay (ms) between {@link #awaitText} polls. */
    public static final double DEFAULT_POLL_INTERVAL_MS = 150;

    /**
     * Click the element matching {@code selector}, wait {@link #DEFAULT_FOCUS_SETTLE_MS}
     * for the hidden input to settle, then type {@code text} via the real keyboard.
     */
    public static void typeInto(Page page, String selector, String text) {
        typeInto(page, selector, text, DEFAULT_FOCUS_SETTLE_MS);
    }

    /**
     * Like {@link #typeInto(Page, String, String)} but with an explicit focus-settle delay.
     */
    public static void typeInto(Page page, String selector, String text, double settleMs) {
        focus(page, selector, settleMs);
        page.keyboard().type(text);
    }

    /**
     * Click the field and wait for the hidden input to settle, without typing yet.
     * Use this when you want to drive individual {@link #press} calls afterwards.
     */
    public static void focus(Page page, String selector) {
        focus(page, selector, DEFAULT_FOCUS_SETTLE_MS);
    }

    /** Like {@link #focus(Page, String)} with an explicit settle delay. */
    public static void focus(Page page, String selector, double settleMs) {
        page.locator(selector).click();
        page.waitForTimeout(settleMs);
    }

    /** Type {@code text} into whatever currently has focus (no click/settle). */
    public static void type(Page page, String text) {
        page.keyboard().type(text);
    }

    /**
     * Press a single key (Playwright key syntax, e.g. {@code "Enter"}, {@code "Backspace"},
     * {@code "Control+A"}, {@code "ArrowLeft"}).
     */
    public static void press(Page page, String key) {
        page.keyboard().press(key);
    }

    /**
     * Press Enter to commit. JavaFX {@code TextFormatter}s run their converter on commit
     * (Enter / focus-loss), not on every keystroke — so to observe formatted text you must
     * commit first.
     */
    public static void commit(Page page) {
        press(page, "Enter");
    }

    /** Current trimmed text content of {@code selector}, or {@code null} if absent. */
    public static String getText(Page page, String selector) {
        String content = page.locator(selector).textContent();
        return content == null ? null : content.trim();
    }

    /**
     * Poll the element at {@code selector} until its trimmed text equals {@code expected},
     * using the default attempts/interval. Throws {@link AssertionError} if it never matches
     * — the message includes the last observed value, which is the usual clue (e.g. focus was
     * lost, {@code fill()} was used instead of typing, or mirrorCSSToDOM is off).
     */
    public static void awaitText(Page page, String selector, String expected) {
        awaitText(page, selector, expected, DEFAULT_POLL_ATTEMPTS, DEFAULT_POLL_INTERVAL_MS);
    }

    /** Like {@link #awaitText(Page, String, String)} with explicit polling parameters. */
    public static void awaitText(Page page, String selector, String expected, int attempts, double intervalMs) {
        Locator locator = page.locator(selector);
        String observed = null;
        for (int i = 0; i < attempts; i++) {
            String content = locator.textContent();
            observed = content == null ? null : content.trim();
            if (expected.equals(observed)) return;
            page.waitForTimeout(intervalMs);
        }
        throw new AssertionError("Expected '" + expected + "' at '" + selector
                + "' after the round-trip, but last observed '" + observed + "' (waited "
                + (long) (attempts * intervalMs) + "ms).");
    }
}
