package one.jpro.platform.auth.core.oauth2;

import one.jpro.platform.auth.core.authentication.User;
import one.jpro.platform.auth.core.jwt.TokenCredentials;
import one.jpro.platform.auth.core.oauth2.provider.GoogleAuthenticationProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OIDC discovery: the issuer of the discovery document must be the site it was loaded from.
 */
public class OAuth2DiscoveryTests {

    private static final String SITE = "http://idp.invalid";

    private FakeIdp idp;
    private String issuer = SITE;

    @BeforeEach
    void setUp() throws IOException {
        idp = new FakeIdp(request -> new FakeIdp.Response(200, request.line().contains("/.well-known/openid-configuration")
                ? "{\"issuer\":\"" + issuer + "\",\"jwks_uri\":\"" + SITE + "/certs\"}"
                : "{\"azp\":\"clientId\",\"aud\":\"clientId\",\"email\":\"g@example.com\",\"expires_in\":\"3599\"}"));
    }

    @AfterEach
    void tearDown() throws IOException {
        idp.close();
    }

    private OAuth2Options options() {
        return new OAuth2Options()
                .setSite(SITE)
                .setClientId("clientId")
                .setHttpClientConfigurator(idp.viaProxy());
    }

    @Test
    void issuerOfAnotherSiteIsRejected() {
        issuer = "http://evil.invalid";

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> new OAuth2AuthenticationProvider(null, options()).discover().get(10, TimeUnit.SECONDS));

        assertTrue(ex.getCause().getMessage().contains("Issuer validation failed"), ex.getCause().getMessage());
    }

    @Test
    void issuerWithTrailingSlashIsAccepted() throws Exception {
        issuer = SITE + "/";

        var provider = new OAuth2AuthenticationProvider(null, options()).discover().get(10, TimeUnit.SECONDS);

        assertEquals(SITE + "/", provider.getOptions().getJWTOptions().getIssuer());
    }

    @Test
    void issuerOfAnotherSiteIsAcceptedWithoutValidation() throws Exception {
        issuer = "http://other.invalid";

        var provider = new OAuth2AuthenticationProvider(null, options().setValidateIssuer(false))
                .discover().get(10, TimeUnit.SECONDS);

        assertEquals("http://other.invalid", provider.getOptions().getJWTOptions().getIssuer());
    }

    @Test
    void discoveredGoogleProviderUsesTokenInfo() throws Exception {
        var provider = GoogleAuthenticationProvider.discover(null,
                options().setIntrospectionPath(SITE + "/tokeninfo")).get(10, TimeUnit.SECONDS);

        User user = provider.authenticate(new TokenCredentials("ya29.opaque")).get(10, TimeUnit.SECONDS);

        assertEquals("g@example.com", user.getName());
        assertTrue(idp.requestLines().contains("POST " + SITE + "/tokeninfo HTTP/1.1"), idp.requestLines()::toString);
    }
}
