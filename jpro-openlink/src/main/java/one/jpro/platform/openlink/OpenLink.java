package one.jpro.platform.openlink;

import javafx.stage.Stage;
import one.jpro.platform.openlink.util.PlatformUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

/**
 * Launches the default browser of the platform as a separate application process. The browser
 * will be opened with the provided URL.
 *
 * @author Besmir Beqiri
 */
public final class OpenLink {

    private static final Logger logger = LoggerFactory.getLogger(OpenLink.class);

    /**
     * Opens the given URL in the browser.
     *
     * @param url the URL to open
     */
    public void openURL(@NotNull URL url) {
        openURL(url.toString());
    }

    /**
     * Opens the given URL string in the browser.
     *
     * @param url the URL string to open
     */
    public void openURL(@NotNull String url) {
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
        }
    }
}
