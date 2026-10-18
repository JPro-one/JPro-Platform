package one.jpro.platform.auth.core.oauth2;

import one.jpro.platform.auth.core.api.FluentKeycloakAuthAPI;
import one.jpro.platform.auth.core.jwt.TokenCredentials;
import one.jpro.platform.auth.core.oauth2.provider.OpenIDAuthenticationProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the configured {@link HttpClient.Builder} callback is used for all OAuth2 requests.
 */
public class HttpClientConfiguratorTests {

    private static final String SITE = "http://idp.invalid/realms/test";

    private FakeIdp idp;

    @BeforeEach
    void startIdp() throws IOException {
        idp = new FakeIdp(request -> new FakeIdp.Response(200,
                request.line().contains("/.well-known/openid-configuration")
                        ? "{\"issuer\":\"" + SITE + "\",\"jwks_uri\":\"" + SITE + "/certs\"}"
                        : "{\"keys\":[]}"));
    }

    @AfterEach
    void stopIdp() throws IOException {
        idp.close();
    }

    @Test
    void discoveryAndJwksFetchUseConfiguredHttpClient() throws Exception {
        OAuth2Options options = new OAuth2Options()
                .setSite(SITE)
                .setClientId("clientId")
                .setHttpClientConfigurator(idp.viaProxy());

        OpenIDAuthenticationProvider provider = new OAuth2AuthenticationProvider(null, options)
                .discover().get(10, TimeUnit.SECONDS);
        assertTrue(idp.requestLines().contains("GET " + SITE + "/.well-known/openid-configuration HTTP/1.1"),
                idp.requestLines()::toString);

        try {
            provider.authenticate(new TokenCredentials(b64("{\"alg\":\"RS256\",\"kid\":\"k1\"}") + "."
                            + b64("{\"sub\":\"1\"}") + ".c2ln"))
                    .handle((user, ex) -> null).get(10, TimeUnit.SECONDS);
        } catch (RuntimeException ignored) {
            // the fake key set is empty, only the request matters
        }
        assertTrue(idp.requestLines().contains("GET " + SITE + "/certs HTTP/1.1"), idp.requestLines()::toString);
    }

    private static String b64(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void fluentKeycloakAuthUsesConfiguredHttpClient() throws Exception {
        new FluentKeycloakAuthAPI()
                .site("http://idp.invalid/realms/{realm}")
                .realm("test")
                .clientId("clientId")
                .configureHttpClient(idp.viaProxy())
                .create(null)
                .discover().get(10, TimeUnit.SECONDS);
        assertTrue(idp.requestLines().contains("GET " + SITE + "/.well-known/openid-configuration HTTP/1.1"),
                idp.requestLines()::toString);
    }
}
