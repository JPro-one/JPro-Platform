package one.jpro.auth.test;

import one.jpro.auth.http.HttpServer;
import one.jpro.auth.oath2.OAuth2Options;
import one.jpro.auth.oath2.provider.MicrosoftAuthenticationProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Microsoft Authentication Provider tests.
 *
 * @author Besmir Beqiri
 */
public class MicrosoftAuthenticationProviderTest {

    @Test
    public void configWithClientIdAndClientSecretAndTenant() {
        try (HttpServer httpServer = HttpServer.create()) {
            MicrosoftAuthenticationProvider provider = new MicrosoftAuthenticationProvider(httpServer,
                    "clientId", "clientSecret",
                    MicrosoftAuthenticationProvider.COMMON_TENANT);
            OAuth2Options options = provider.getOptions();

            assertEquals("https://login.microsoftonline.com/common/v2.0", options.getSite());
            assertEquals("https://login.microsoftonline.com/common/oauth2/v2.0/token", options.getTokenPath());
            assertEquals("https://login.microsoftonline.com/common/oauth2/v2.0/authorize", options.getAuthorizationPath());
            assertEquals("https://graph.microsoft.com/oidc/userinfo", options.getUserInfoPath());
            assertEquals("https://login.microsoftonline.com/common/discovery/v2.0/keys", options.getJwkPath());
            assertEquals("https://login.microsoftonline.com/common/oauth2/v2.0/logout", options.getLogoutPath());
            assertEquals("SHA-256", options.getJWTOptions().getNonceAlgorithm());
        }
    }

    @Test
    public void autoConfigViaOpenIDConnectDiscoveryService() throws ExecutionException, InterruptedException {
        try (HttpServer authServer = HttpServer.create()) {
            MicrosoftAuthenticationProvider.discover(authServer, new OAuth2Options()
                            .setClientId("clientId")
                            .setTenant("common"))
                    .thenAccept(provider -> {
                        OAuth2Options options = provider.getOptions();

                        assertEquals("https://login.microsoftonline.com/{tenantid}/v2.0", options.getJWTOptions().getIssuer());
                        assertEquals("https://login.microsoftonline.com/common/oauth2/v2.0/token", options.getTokenPath());
                        assertEquals("https://login.microsoftonline.com/common/oauth2/v2.0/authorize", options.getAuthorizationPath());
                        assertEquals("https://graph.microsoft.com/oidc/userinfo", options.getUserInfoPath());
                        assertEquals("https://login.microsoftonline.com/common/discovery/v2.0/keys", options.getJwkPath());
                        assertEquals("https://login.microsoftonline.com/common/oauth2/v2.0/logout", options.getLogoutPath());
                        assertEquals(List.of("code", "id_token", "code id_token", "id_token token"),
                                options.getSupportedResponseTypes());
                        assertEquals(List.of("query", "fragment", "form_post"), options.getSupportedResponseModes());
                        assertEquals(List.of("pairwise"), options.getSupportedSubjectTypes());
                        assertEquals(List.of("RS256"), options.getSupportedIdTokenSigningAlgValues());
                        assertEquals(List.of("openid", "profile", "email", "offline_access"), options.getSupportedScopes());
                        assertEquals(List.of("client_secret_post", "private_key_jwt", "client_secret_basic"),
                                options.getSupportedTokenEndpointAuthMethods());
                        assertEquals(List.of("sub", "iss", "cloud_instance_name", "cloud_instance_host_name",
                                        "cloud_graph_host_name", "msgraph_host", "aud", "exp", "iat",
                                        "auth_time", "acr", "nonce", "preferred_username", "name", "tid",
                                        "ver", "at_hash", "c_hash", "email"),
                                options.getSupportedClaims());
                    }).get();
        }
    }
}
