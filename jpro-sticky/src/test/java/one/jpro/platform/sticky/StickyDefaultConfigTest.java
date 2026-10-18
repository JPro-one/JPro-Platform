package one.jpro.platform.sticky;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import one.jpro.platform.playwright.BrowserErrorCollector;
import one.jpro.platform.playwright.JProPlaywrightTest;
import one.jpro.platform.playwright.JProWait;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the example with JPro's default {@code jpro.mirrorCSSToDOM = false}, under which no FX id or
 * style class reaches the DOM. The web pin must bind through element handles alone, so it works in
 * an app that never set the flag.
 */
public class StickyDefaultConfigTest extends JProPlaywrightTest {

    private static final File LOGS_DIR = new File(projectRoot(), "jpro-sticky/example/logs");

    @BeforeAll
    static void startJProServer() throws Exception {
        startServer(LOGS_DIR, gradleCommand(":jpro-sticky:example:jproStart",
                "-Pjpro.test.mirrorCSSToDOM=false"));
    }

    @AfterAll
    static void stopJProServer() throws Exception {
        stopAndAssertNoServerErrors(LOGS_DIR, gradleCommand(":jpro-sticky:example:jproStop"));
    }

    @Test
    @DisplayName("Pins bind and hold without mirrored DOM ids")
    @SuppressWarnings("unchecked")
    void pinsBindWithoutMirroredIds() {
        Page page = newPage();
        BrowserErrorCollector browserErrors = new BrowserErrorCollector(page);
        page.navigate(BASE_URL);
        waitForRunning(page);
        page.locator("[data-jpro-sticky-el]").first().waitFor(
                new Locator.WaitForOptions().setTimeout(30_000));

        assertEquals(0, page.locator(".jpro-id-sticky-header").count(),
                "the sample's ids must be absent, or this test runs with the flag on");

        // every installed pin binds its element, and every bound element is position: sticky
        JProWait.waitUntil(() -> {
            int sheets = page.locator("style[data-jpro-sticky]").count();
            return sheets > 0 && sheets == page.locator("[data-jpro-sticky-el]").count() && sheets == stickyCount(page);
        }, () -> "sheets=" + page.locator("style[data-jpro-sticky]").count()
                + " bound=" + page.locator("[data-jpro-sticky-el]").count() + " sticky=" + stickyCount(page));
        int bound = page.locator("[data-jpro-sticky-el]").count();
        JProWait.ensureStaysValue(1000, bound, () -> page.locator("[data-jpro-sticky-el]").count());

        String tops = "() => [...document.querySelectorAll('[data-jpro-sticky-el]')]"
                + ".map(e => e.getBoundingClientRect().top)";
        List<Number> before = (List<Number>) page.evaluate(tops);
        page.evaluate("() => window.scrollBy(0, 400)");
        BooleanSupplier held = () -> {
            List<Number> after = (List<Number>) page.evaluate(tops);
            for (int i = 0; i < Math.min(before.size(), after.size()); i++) {
                if (Math.abs(before.get(i).doubleValue()) < 1 && Math.abs(after.get(i).doubleValue()) < 1) {
                    return before.size() == after.size();
                }
            }
            return false;
        };
        Supplier<String> heldState = () -> "a pin at the viewport top should hold across the scroll: "
                + before + " -> " + page.evaluate(tops);
        JProWait.waitUntil(held, heldState);
        JProWait.ensureStaysTrue(1000, held, heldState);

        // the section header sits deep in the page and pins under the 48px page header, so its rule
        // carries a non-zero span offset. a wrong offset would put it anywhere but there.
        page.evaluate("() => window.scrollTo(0, 2400)");
        JProWait.waitUntil(() -> ((List<Number>) page.evaluate(tops)).stream()
                        .anyMatch(t -> Math.abs(t.doubleValue() - 48) < 1),
                () -> "the section header should pin at 48px: " + page.evaluate(tops));

        page.context().close();
        browserErrors.assertNoErrors();
    }

    private static int stickyCount(Page page) {
        return ((Number) page.evaluate("() => [...document.querySelectorAll('[data-jpro-sticky-el]')]"
                + ".filter(e => getComputedStyle(e).position === 'sticky').length")).intValue();
    }
}
