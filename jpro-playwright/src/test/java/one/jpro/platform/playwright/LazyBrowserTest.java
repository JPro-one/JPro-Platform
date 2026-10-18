package one.jpro.platform.playwright;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A test that brings its own browser (e.g. one that always needs Firefox) doesn't launch the default one too.
 */
class LazyBrowserTest extends JProPlaywrightTest {

    @Test
    void noBrowserIsLaunchedBeforeTheFirstPage() {
        assertNull(browser);
    }
}
