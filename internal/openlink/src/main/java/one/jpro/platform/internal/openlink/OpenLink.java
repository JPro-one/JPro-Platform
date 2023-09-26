package one.jpro.platform.internal.openlink;

import one.jpro.platform.internal.openlink.util.PlatformUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

/**
 * Launches the default browser of the platform as a separate application process. The browser
 * will be opened with the provided URL.
 *
 * @author Besmir Beqiri
 */
public interface OpenLink {

    Logger logger = LoggerFactory.getLogger(OpenLink.class);

    /**
     * Opens the given URL in the browser.
     *
     * @param url the URL to open
     */
    static void openURL(@NotNull URL url) {
        openURL(url.toString());
    }

    /**
     * Opens the given URL string in the browser.
     *
     * @param url the URL string to open
     */
    static void openURL(@NotNull String url) {
        try {
            if (PlatformUtils.isMac()) {
                Runtime.getRuntime().exec("open " + url);
            } else if (PlatformUtils.isWindows()) {
                Runtime.getRuntime().exec("start \"" + url + "\"");
            } else if (PlatformUtils.isLinux()) {
                Runtime.getRuntime().exec("xdg-open " + url);
            }
        } catch (IOException ex) {
            logger.error("Unable to open the browser!", ex);
            throw new UnsupportedOperationException("Unable to open the browser!", ex);
        }
    }
}