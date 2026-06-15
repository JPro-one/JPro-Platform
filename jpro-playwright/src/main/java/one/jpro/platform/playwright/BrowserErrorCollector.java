package one.jpro.platform.playwright;

import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public BrowserErrorCollector(Page page) {
        page.onConsoleMessage(msg -> {
            if ("error".equals(msg.type())) {
                errors.add("[console.error] " + msg.text());
            }
        });
        page.onPageError(err -> errors.add("[pageerror] " + err));
        page.onRequestFailed(req -> errors.add("[network-failed] " + req.url() + " — " + req.failure()));
    }

    /** Fails with an AssertionError listing every collected error. */
    public void assertNoErrors() {
        synchronized (errors) {
            if (!errors.isEmpty()) {
                throw new AssertionError("Browser errors:\n" + String.join("\n", errors));
            }
        }
    }
}
