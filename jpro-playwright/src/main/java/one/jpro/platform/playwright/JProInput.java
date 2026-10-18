package one.jpro.platform.playwright;

import com.microsoft.playwright.Page;

/**
 * Keyboard input against a JPro app, using the sequence that works reliably.
 *
 * <p>A JPro text field renders a DOM {@code <input>} inside its element. Either focus the field, wait until
 * that input has the focus and type with the real keyboard ({@link #typeInto}), or set the whole value at once
 * with {@code page.locator(".jpro-id-foo input").fill(text)}. {@code fill()} on the field's element itself
 * fails, as it is no input.
 *
 * <p>Each input is an asynchronous round-trip to the JVM and back, so read results with
 * {@link #awaitText} rather than a single read. Selecting a field needs
 * {@code jpro.mirrorCSSToDOM = true}; a node with {@code setId("foo")} is then {@code ".jpro-id-foo"}.
 */
public final class JProInput {

    private JProInput() {}

    private static final String FOCUS_INSIDE = "sel => { const el = document.querySelector(sel);"
            + " const a = document.activeElement; return !!el && !!a && el !== a && el.contains(a); }";

    /** Click the element at {@code selector}, wait until its input has the focus, then type {@code text}. */
    public static void typeInto(Page page, String selector, String text) {
        focus(page, selector);
        page.keyboard().type(text);
    }

    /** Click the field and wait until its input has the focus, without typing — for driving {@link #press}. */
    public static void focus(Page page, String selector) {
        page.locator(selector).click();
        JProWait.waitForFunction(page, JProWait.SETTLE_BUDGET_MS, "focus inside " + selector, FOCUS_INSIDE, selector);
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
     * Poll {@code selector} until its trimmed text equals {@code expected} (see {@link JProWait#waitForValue}).
     * Throws {@link AssertionError} with the last observed value on timeout.
     */
    public static void awaitText(Page page, String selector, String expected) {
        JProWait.waitForValue(expected, () -> getText(page, selector));
    }
}
