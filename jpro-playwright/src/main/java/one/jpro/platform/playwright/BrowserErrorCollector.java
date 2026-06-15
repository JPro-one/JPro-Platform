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
 *   Page page = ...;
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
                record("[console.error] " + msg.text());
            }
        });
        page.onPageError(err -> record("[pageerror] " + err));
        page.onRequestFailed(req -> record("[network-failed] " + req.url() + " — " + req.failure()));
    }

    /**
     * Ignore any future errors whose formatted form matches the given predicate. Useful for
     * filtering out known-benign noise (e.g. {@code /favicon.ico}).
     */
    public BrowserErrorCollector ignoreMatching(Predicate<String> predicate) {
        this.ignoreFilter = this.ignoreFilter.or(predicate);
        return this;
    }

    /** Snapshot of errors collected so far. */
    public List<String> getErrors() {
        synchronized (errors) {
            return List.copyOf(errors);
        }
    }

    /** Fails with AssertionError listing every collected error (after ignoreMatching filters). */
    public void assertNoErrors() {
        List<String> snapshot = getErrors();
        if (!snapshot.isEmpty()) {
            throw new AssertionError("Browser errors:\n" + String.join("\n", snapshot));
        }
    }

    private void record(String error) {
        if (!ignoreFilter.test(error)) {
            errors.add(error);
        }
    }
}
