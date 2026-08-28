package one.jpro.platform.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.ViewportSize;

/**
 * Scrolling and viewport-geometry probes for JPro apps, the counterpart to {@link JProInput} for
 * scroll-driven features (sticky / fixed positioning). Like {@link JProInput}, the sequences here
 * are the ones verified to work against a real JPro app rather than assumed.
 *
 * <p>Two rules shape this helper:
 * <ul>
 *   <li>JPro scrolls <em>natively</em> in the browser — the page (or an FX {@code ScrollPane}) is a
 *       real scroll container — so scrolling is driven with the real wheel ({@code page.mouse().wheel})
 *       at a hover point, not by a scripted {@code window.scrollTo}. {@link #scrollBy} scrolls the
 *       page; {@link #scrollWithin} scrolls a specific {@code ScrollPane}.</li>
 *   <li>Each wheel is an asynchronous round-trip (native compositor pin + a server sync), so a scroll
 *       is followed by a short settle and geometry is read with {@link #awaitTop}, which waits for the
 *       value to stop moving rather than reading once mid-animation.</li>
 * </ul>
 *
 * <p>Selecting a node needs {@code jpro.mirrorCSSToDOM = true}; a node with {@code setId("foo")} is
 * then reachable as {@code "#jpro-foo"}. Positions are viewport-relative (a pinned sticky/fixed
 * element holds its {@link #top} across a scroll; an in-flow element's {@code top} decreases).
 */
public final class JProScroll {

    private JProScroll() {}

    /** Time for a wheel's compositor pin + server sync to settle before geometry is read. */
    private static final double SETTLE_MS = 350;
    /** Two {@link #top} reads within this many px are treated as "stopped moving". */
    private static final double STABLE_EPSILON_PX = 1.0;
    private static final int STABLE_ATTEMPTS = 20;
    private static final double STABLE_INTERVAL_MS = 100;

    /**
     * Scroll the page by {@code deltaY} px (positive = down) with the real mouse wheel, hovering the
     * viewport centre so the wheel lands on the page rather than a nested {@code ScrollPane}, then
     * settle. Use a negative delta to scroll back up.
     */
    public static void scrollBy(Page page, double deltaY) {
        ViewportSize size = page.viewportSize();
        page.mouse().move(size.width / 2.0, size.height / 2.0);
        page.mouse().wheel(0, deltaY);
        page.waitForTimeout(SETTLE_MS);
    }

    /**
     * Scroll <em>inside</em> the element at {@code selector} (e.g. an FX {@code ScrollPane}) by
     * {@code deltaY} px, by hovering it first so the wheel is delivered to that container, then settle.
     */
    public static void scrollWithin(Page page, String selector, double deltaY) {
        page.locator(selector).hover();
        page.mouse().wheel(0, deltaY);
        page.waitForTimeout(SETTLE_MS);
    }

    /**
     * Scroll the page back to the top by wheeling up well past any plausible content height, then
     * settle. Handy for resetting between assertions.
     */
    public static void scrollToTop(Page page) {
        scrollBy(page, -100_000);
    }

    /**
     * The current viewport-relative top ({@code getBoundingClientRect().y}, transforms included) of
     * {@code selector}. Throws if the element has no box (detached / not rendered).
     */
    public static double top(Page page, String selector) {
        BoundingBox box = page.locator(selector).boundingBox();
        if (box == null) {
            throw new AssertionError("No bounding box for '" + selector + "' (not rendered?).");
        }
        return box.y;
    }

    /**
     * Read {@link #top} repeatedly until it stops moving (two consecutive reads within
     * {@value #STABLE_EPSILON_PX}px) and return the settled value. Prefer this over {@link #top} right
     * after a scroll, so an assertion sees the final pinned/released position, not a mid-animation frame.
     */
    public static double awaitTop(Page page, String selector) {
        double previous = top(page, selector);
        for (int i = 0; i < STABLE_ATTEMPTS; i++) {
            page.waitForTimeout(STABLE_INTERVAL_MS);
            double current = top(page, selector);
            if (Math.abs(current - previous) <= STABLE_EPSILON_PX) return current;
            previous = current;
        }
        return previous;
    }

    /** The current viewport-relative bottom of {@code selector} (top + height). */
    public static double bottom(Page page, String selector) {
        BoundingBox box = page.locator(selector).boundingBox();
        if (box == null) {
            throw new AssertionError("No bounding box for '" + selector + "' (not rendered?).");
        }
        return box.y + box.height;
    }
}
