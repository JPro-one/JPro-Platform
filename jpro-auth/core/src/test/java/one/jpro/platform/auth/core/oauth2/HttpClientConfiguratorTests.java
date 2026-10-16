package one.jpro.platform.auth.core.oauth2;

import one.jpro.platform.auth.core.api.FluentKeycloakAuthAPI;
import one.jpro.platform.auth.core.jwt.TokenCredentials;
import one.jpro.platform.auth.core.oauth2.provider.OpenIDAuthenticationProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the configured {@link HttpClient.Builder} callback is used for all OAuth2 requests.
 */
public class HttpClientConfiguratorTests {

    private static final String SITE = "http://idp.invalid/realms/test";

    private ServerSocket proxy;
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private Consumer<HttpClient.Builder> viaProxy;

    @BeforeEach
    void startProxy() throws IOException {
        proxy = new ServerSocket(0);
        viaProxy = builder -> builder.proxy(ProxySelector.of(new InetSocketAddress("localhost", proxy.getLocalPort())));
        Thread thread = new Thread(() -> {
            while (!proxy.isClosed()) {
                try (Socket socket = proxy.accept()) {
                    handle(socket);
                } catch (IOException ignored) {
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    @AfterEach
    void stopProxy() throws IOException {
        proxy.close();
    }

    private void handle(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        String requestLine = in.readLine();
        requests.add(requestLine);
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            // skip headers
        }
        String body = requestLine.contains("/.well-known/openid-configuration")
                ? "{\"issuer\":\"" + SITE + "\",\"jwks_uri\":\"" + SITE + "/certs\"}"
                : "{\"keys\":[]}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        OutputStream out = socket.getOutputStream();
        out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: " + bytes.length
                + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(bytes);
        out.flush();
    }

    @Test
    void discoveryAndJwksFetchUseConfiguredHttpClient() throws Exception {
        OAuth2Options options = new OAuth2Options()
                .setSite(SITE)
                .setClientId("clientId")
                .setHttpClientConfigurator(viaProxy);

        OpenIDAuthenticationProvider provider = new OAuth2AuthenticationProvider(null, options)
                .discover().get(10, TimeUnit.SECONDS);
        assertTrue(requests.contains("GET " + SITE + "/.well-known/openid-configuration HTTP/1.1"), requests::toString);

        try {
            provider.authenticate(new TokenCredentials("header.payload.signature"))
                    .handle((user, ex) -> null).get(10, TimeUnit.SECONDS);
        } catch (RuntimeException ignored) {
            // the fake key set is empty, only the request matters
        }
        assertTrue(requests.contains("GET " + SITE + "/certs HTTP/1.1"), requests::toString);
    }

    @Test
    void fluentKeycloakAuthUsesConfiguredHttpClient() throws Exception {
        new FluentKeycloakAuthAPI()
                .site("http://idp.invalid/realms/{realm}")
                .realm("test")
                .clientId("clientId")
                .configureHttpClient(viaProxy)
                .create(null)
                .discover().get(10, TimeUnit.SECONDS);
        assertTrue(requests.contains("GET " + SITE + "/.well-known/openid-configuration HTTP/1.1"), requests::toString);
    }
}
