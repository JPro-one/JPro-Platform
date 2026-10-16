package one.jpro.platform.auth.core.http.impl;

import com.jpro.webapi.WebAPI;
import one.jpro.platform.auth.core.http.HttpServer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of the {@link HttpServer} interface when running
 * the application via JPro server.
 *
 * @author Besmir Beqiri
 */
public class JProServerImpl implements HttpServer {

    @NotNull
    private final WebAPI webAPI;

    /**
     * Creates a new instance with the specified WebAPI.
     *
     * @param webAPI The WebAPI instance to use.
     * @throws NullPointerException if the webAPI parameter is null.
     */
    public JProServerImpl(@NotNull final WebAPI webAPI) {
        this.webAPI = Objects.requireNonNull(webAPI, "WebAPI cannot be null");
    }

    @Override
    public void start() {
        // do nothing
    }

    @Override
    public void stop() {
        // do nothing
    }

    @Override
    public String getServerHost() {
        final String serverName = webAPI.getServer();
        final int idx = serverName.indexOf(':');
        return  (idx >= 0) ? serverName.substring(0, idx) : serverName;
    }

    @Override
    public int getServerPort() {
        final String serverName = webAPI.getServer();
        final int idx = serverName.indexOf(':');
        return (idx >= 0) ? Integer.parseInt(serverName.substring(idx + 1)) : -1;
    }

    /**
     * Returns the URL of the most recently opened browser tab of this instance, so a callback
     * that reconnects to a running instance is seen.
     */
    @Override
    public String getFullRequestedURL() {
        final List<WebAPI.View> views = webAPI.getViews();
        return views.isEmpty() ? webAPI.getBrowserURL() : views.get(views.size() - 1).getBrowserURL();
    }

    @Override
    public CompletableFuture<String> openURL(@NotNull final String url) {
        return CompletableFuture.runAsync(() -> webAPI.openURL(url))
                .thenCompose(v -> CompletableFuture.completedFuture(null));
    }
}
