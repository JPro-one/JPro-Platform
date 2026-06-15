package one.jpro.platform.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Keyboard input against a JPro app, using the sequence that works reliably.
 *
 * <p>A JavaFX control rendered by JPro is not a native DOM input, so two rules apply (both
 * verified against the example app, not assumed):
 * <ul>
 *   <li>Focus the field and wait briefly before typing, and type with the real keyboard
 *       ({@code page.keyboard()}). {@link #typeInto} does this.</li>
 *   <li>{@code Locator.fill()} does not work — Playwright rejects it because the element is not
 *       an {@code <input>}/{@code <textarea>}/{@code [contenteditable]}.</li>
 * </ul>
 *
 * <p>Each input is an asynchronous round-trip to the JVM and back, so read results with
 * {@link #awaitText} rather than a single read. Selecting a field needs
 * {@code jpro.mirrorCSSToDOM = true}; a node with {@code setId("foo")} is then {@code "#jpro-foo"}.
 */
public final class JProInput {

    private JProInput() {}

    private static final double FOCUS_SETTLE_MS = 300;
    private static final int POLL_ATTEMPTS = 20;
    private static final double POLL_INTERVAL_MS = 150;

    /** Click the element at {@code selector}, let it settle, then type {@code text}. */
    public static void typeInto(Page page, String selector, String text) {
        focus(page, selector);
        page.keyboard().type(text);
    }

    /** Click the field and wait for it to settle, without typing — for driving {@link #press}. */
    public static void focus(Page page, String selector) {
        page.locator(selector).click();
        page.waitForTimeout(FOCUS_SETTLE_MS);
    }

    /**
     * Press a single key on the focused field (Playwright key syntax, e.g. {@code "Enter"},
     * {@code "Backspace"}, {@code "ArrowLeft"}, {@code "Control+A"}).
     */
    public static void press(Page page, String key) {
        page.keyboard().press(key);
    }

    /** Press Enter. JavaFX {@code onAction} / {@code TextFormatter} commit on Enter, not per keystroke. */
    public static void commit(Page page) {
        press(page, "Enter");
    }

    /** Current trimmed text content of {@code selector}, or {@code null} if absent. */
    public static String getText(Page page, String selector) {
        String content = page.locator(selector).textContent();
        return content == null ? null : content.trim();
    }

    /**
     * Poll {@code selector} until its trimmed text equals {@code expected}. Throws
     * {@link AssertionError} with the last observed value on timeout. (Equivalent to Playwright's
     * auto-retrying {@code assertThat(locator).hasText(expected)}.)
     */
    public static void awaitText(Page page, String selector, String expected) {
        Locator locator = page.locator(selector);
        String observed = null;
        for (int i = 0; i < POLL_ATTEMPTS; i++) {
            String content = locator.textContent();
            observed = content == null ? null : content.trim();
            if (expected.equals(observed)) return;
            page.waitForTimeout(POLL_INTERVAL_MS);
        }
        throw new AssertionError("Expected '" + expected + "' at '" + selector
                + "' after the round-trip, but last observed '" + observed + "' (waited "
                + (long) (POLL_ATTEMPTS * POLL_INTERVAL_MS) + "ms).");
    }
}
