package one.jpro.auth.http.impl;

import com.jpro.webapi.WebAPI;
import one.jpro.auth.http.HttpServer;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Objects;

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
        return (idx >= 0) ? serverName.substring(0, idx) : serverName;
    }

    @Override
    public int getServerPort() {
        final String serverName = webAPI.getServer();
        final int idx = serverName.indexOf(':');
        return (idx >= 0) ? Integer.parseInt(serverName.substring(idx + 1)) : -1;
    }

    @Override
    public String getFullRequestedURL() {
        return URI.create(webAPI.getServerName()).toString();
    }

    @Override
    public void openURL(@NotNull URI uri) {
        webAPI.openURL(uri.toString());
    }
}
