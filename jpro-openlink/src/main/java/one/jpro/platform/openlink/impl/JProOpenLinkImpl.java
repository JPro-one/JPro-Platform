package one.jpro.platform.openlink.impl;

import com.jpro.webapi.WebAPI;
import one.jpro.platform.openlink.OpenLink;
import org.jetbrains.annotations.NotNull;

import java.net.URL;

/**
 * JPro OpenLink implementation.
 *
 * @author Besmir Beqiri
 */
public class JProOpenLinkImpl implements OpenLink {

    @NotNull
    private final WebAPI webAPI;

    public JProOpenLinkImpl(@NotNull WebAPI webAPI) {
        this.webAPI = webAPI;
    }

    @Override
    public void openURL(@NotNull URL url) {
        webAPI.openURL(url.toString());
    }

    @Override
    public void openURL(@NotNull String url) {
        webAPI.openURL(url);
    }
}
