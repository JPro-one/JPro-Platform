package one.jpro.platform.internal.openlink;

import one.jpro.platform.internal.util.PlatformUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

/**
 * Launches the default browser of the platform as a separate application process. The browser
 * will be opened with the provided URL.
 *
 * @author Besmir Beqiri
 */
public interface OpenLink {

    Logger logger = org.slf4j.LoggerFactory.getLogger(OpenLink.class);

    /**
     * Opens the given URL in the browser.
     *
     * @param url the URL to open
     */
    static void openURL(@NotNull URL url) {
        Objects.requireNonNull(url, "URL cannot be null.");
        openURL(url.toString());
    }

    /**
     * Opens the given URL string in the browser.
     *
     * @param url the URL string to open
     */
    static void openURL(@NotNull String url) {
        Objects.requireNonNull(url, "URL cannot be null.");

        if (PlatformUtils.isDesktop()) {
            try {
                List<String> command =
                        PlatformUtils.isMac() ? List.of("open", url) :
                                PlatformUtils.isWindows() ? List.of("rundll32", "url.dll,FileProtocolHandler", url) :
                                        List.of("xdg-open", url);
                logger.debug("Opening URL with command {}", command);
                Runtime.getRuntime().exec(command.toArray(String[]::new));
            } catch (IOException ex) {
                throw new UnsupportedOperationException("Unable to open the browser.", ex);
            }
        } else {
            throw new UnsupportedOperationException("Platform not supported.");
        }
    }
}
