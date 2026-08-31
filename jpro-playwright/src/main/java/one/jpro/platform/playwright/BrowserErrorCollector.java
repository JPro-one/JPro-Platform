package one.jpro.platform.playwright;

import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Collects browser-side errors observed on a Playwright {@link Page} across three channels:
 * <ul>
 *   <li>{@code console.error(...)} from the page's JS</li>
 *   <li>Uncaught JavaScript exceptions (equivalent to {@code window.onerror})</li>
 *   <li>Network-level request failures (DNS / connection / abort — not HTTP 4xx/5xx responses)</li>
 * </ul>
 *
 * Usage:
 * <pre>{@code
 *   BrowserErrorCollector errors = new BrowserErrorCollector(page);
 *   // drive the test ...
 *   errors.assertNoErrors();
 * }</pre>
 *
 * Construct once per Page, early enough that listeners are installed before the page does
 * anything interesting. Thread-safe: Playwright dispatches events asynchronously.
 */
public final class BrowserErrorCollector {

    private final List<String> errors = Collections.synchronizedList(new ArrayList<>());
    private volatile Predicate<String> ignoreFilter = s -> false;

    public BrowserErrorCollector(Page page) {
        page.onConsoleMessage(msg -> {
            if ("error".equals(msg.type())) {
                errors.add("[console.error] " + msg.text());
            }
        });
        page.onPageError(err -> errors.add("[pageerror] " + err));
        page.onRequestFailed(req -> errors.add("[network-failed] " + req.url() + " — " + req.failure()));
    }

    /**
     * Ignores errors matching the predicate (in addition to earlier ones) when asserting,
     * e.g. an expected {@code net::ERR_ABORTED} after cancelling a request.
     */
    public BrowserErrorCollector ignoreMatching(Predicate<String> predicate) {
        this.ignoreFilter = this.ignoreFilter.or(predicate);
        return this;
    }

    /** Fails with an AssertionError listing every collected error (after ignoreMatching filters). */
    public void assertNoErrors() {
        synchronized (errors) {
            List<String> remaining = errors.stream().filter(e -> !ignoreFilter.test(e)).toList();
            if (!remaining.isEmpty()) {
                throw new AssertionError("Browser errors:\n" + String.join("\n", remaining));
            }
        }
    }
}
