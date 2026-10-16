package one.jpro.platform.auth.core.oauth2;

import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

/**
 * OAuth2Options tests.
 *
 * @author Besmir Beqiri
 */
public class OAuth2OptionsTests {

    @Test
    public void jwtOptionsAreNotSharedBetweenInstances() {
        OAuth2Options first = new OAuth2Options();
        OAuth2Options second = new OAuth2Options();
        first.getJWTOptions().setIssuer("https://first.example");
        OAuth2Options copy = new OAuth2Options(first);
        copy.getJWTOptions().setIssuer("https://copy.example");

        assertNull(second.getJWTOptions().getIssuer());
        assertEquals("https://first.example", first.getJWTOptions().getIssuer());
    }

    @Test
    public void copyConstructorKeepsHttpClientConfigurator() {
        Consumer<HttpClient.Builder> configurator = builder -> {};
        OAuth2Options options = new OAuth2Options().setHttpClientConfigurator(configurator);

        assertSame(configurator, new OAuth2Options(options).getHttpClientConfigurator());
    }

    @Test
    public void trailingSlashShouldBeRemoved() {
        OAuth2Options options = new OAuth2Options()
                .setSite("https://accounts.google.com/");

        assertEquals("https://accounts.google.com", options.getSite());
    }

    @Test
    public void missingOAuth2FlowThrowsException() {
        Exception exception = assertThrowsExactly(IllegalStateException.class, () -> {
            OAuth2Options options = new OAuth2Options()
                    .setFlow(null)
                    .setSite("https://accounts.google.com")
                    .setClientId("clientId");
            options.validate();
        });

        assertEquals("Missing OAuth2 flow: [AUTH_CODE, PASSWORD, CLIENT, AUTH_JWT]", exception.getMessage());
    }

    @Test
    public void missingClientIdThrowsException() {
        Exception exception = assertThrowsExactly(IllegalStateException.class, () -> {
            OAuth2Options options = new OAuth2Options()
                    .setSite("https://accounts.google.com");
            options.validate();
        });

        assertEquals("Missing configuration: [clientId]", exception.getMessage());
    }

    @Test
    public void microsoftAuthConfigWithoutTenantThrowsException() {
        Exception exception = assertThrowsExactly(IllegalStateException.class, () -> {
            OAuth2Options options = new OAuth2Options()
                    .setSite("https://login.microsoftonline.com/{tenant}")
                    .setAuthorizationPath("/oauth2/v2.0/authorize");
            // The tenant value is null, so retrieving the authorization path will throw an exception.
            assertEquals("https://login.microsoftonline.com/{tenant}/oauth2/v2.0/authorize",
                    options.getAuthorizationPath());
        });

        assertEquals("The tenant value is null or blank.", exception.getMessage());
    }

    @Test
    public void microsoftAuthConfigWithTenant() {
        OAuth2Options options = new OAuth2Options()
                .setSite("https://login.microsoftonline.com/{tenant}")
                .setAuthorizationPath("/oauth2/v2.0/authorize")
                .setTenant("common");

        assertEquals("https://login.microsoftonline.com/common",
                options.getSite());
        assertEquals("https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
                options.getAuthorizationPath());
    }
}
