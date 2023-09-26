package one.jpro.platform.openlink.impl;

import one.jpro.platform.openlink.OpenLink;
import one.jpro.platform.openlink.util.PlatformUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

/**
 * Desktop OpenLink implementation.
 *
 * @author Besmir Beqiri
 */
public class DesktopOpenLinkImpl implements OpenLink {

    private static final Logger logger = LoggerFactory.getLogger(DesktopOpenLinkImpl.class);

    @Override
    public void openURL(@NotNull URL url) {
        openURL(url.toString());
    }

    @Override
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
