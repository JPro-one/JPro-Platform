package one.jpro.auth.test;

import one.jpro.auth.http.HttpServer;
import one.jpro.auth.oath2.OAuth2Options;
import one.jpro.auth.oath2.provider.GoogleAuthenticationProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Google Authentication Provider tests.
 *
 * @author Besmir Beqiri
 */
public class GoogleAuthenticationProviderTest {

    @Test
    public void configWithClientIdAndClientSecret() {
        try (HttpServer httpServer = HttpServer.create()) {
            GoogleAuthenticationProvider provider =
                    new GoogleAuthenticationProvider(httpServer, "clientId", "clientSecret");
            OAuth2Options options = provider.getOptions();

            assertEquals("https://accounts.google.com", options.getSite());
            assertEquals("https://oauth2.googleapis.com/token", options.getTokenPath());
            assertEquals("https://accounts.google.com/o/oauth2/v2/auth", options.getAuthorizationPath());
            assertEquals("https://www.googleapis.com/oauth2/v1/userinfo", options.getUserInfoPath());
            assertEquals("https://www.googleapis.com/oauth2/v3/certs", options.getJwkPath());
            assertEquals("https://oauth2.googleapis.com/tokeninfo", options.getIntrospectionPath());
            assertEquals("https://oauth2.googleapis.com/revoke", options.getRevocationPath());
        }
    }

    @Test
    public void autoConfigViaOpenIDConnectDiscoveryService() throws ExecutionException, InterruptedException {
        try (HttpServer httpServer = HttpServer.create()) {
            GoogleAuthenticationProvider.discover(httpServer, new OAuth2Options().setClientId("clientId"))
                    .thenAccept(provider -> {
                        OAuth2Options options = provider.getOptions();

                        assertEquals("https://accounts.google.com", options.getJWTOptions().getIssuer());
                        assertEquals("https://oauth2.googleapis.com/token", options.getTokenPath());
                        assertEquals("https://accounts.google.com/o/oauth2/v2/auth", options.getAuthorizationPath());
                        assertEquals("https://openidconnect.googleapis.com/v1/userinfo", options.getUserInfoPath());
                        assertEquals("https://www.googleapis.com/oauth2/v3/certs", options.getJwkPath());
                        assertEquals("https://oauth2.googleapis.com/revoke", options.getRevocationPath());
                        assertEquals(List.of("code", "token", "id_token", "code token", "code id_token",
                                        "token id_token", "code token id_token", "none"),
                                options.getSupportedResponseTypes());
                        assertEquals(List.of("public"), options.getSupportedSubjectTypes());
                        assertEquals(List.of("RS256"), options.getSupportedIdTokenSigningAlgValues());
                        assertEquals(List.of("openid", "email", "profile"), options.getSupportedScopes());
                        assertEquals(List.of("client_secret_post", "client_secret_basic"),
                                options.getSupportedTokenEndpointAuthMethods());
                        assertEquals(List.of("aud", "email", "email_verified", "exp", "family_name",
                                        "given_name", "iat", "iss", "locale", "name", "picture", "sub"),
                                options.getSupportedClaims());
                        assertEquals(List.of("plain", "S256"), options.getSupportedCodeChallengeMethods());
                        assertEquals(List.of("authorization_code", "refresh_token",
                                "urn:ietf:params:oauth:grant-type:device_code",
                                "urn:ietf:params:oauth:grant-type:jwt-bearer"), options.getSupportedGrantTypes());
                    }).get();
        }
    }
}
