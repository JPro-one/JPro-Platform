package one.jpro.auth.oath2.provider;

import one.jpro.auth.http.HttpServer;
import one.jpro.auth.oath2.OAuth2AuthenticationProvider;
import one.jpro.auth.oath2.OAuth2Flow;
import one.jpro.auth.oath2.OAuth2Options;
import one.jpro.auth.oath2.PubSecKeyOptions;
import org.json.JSONObject;

import java.util.concurrent.CompletableFuture;

/**
 * Simplified factory to create an {@link OAuth2AuthenticationProvider} for Keycloak.
 *
 * @author Besmir Beqiri
 */
public class KeycloakAuthenticationProvider extends OAuth2AuthenticationProvider {

    /**
     * Create an {@link OAuth2AuthenticationProvider} for Keycloak.
     *
     * @param httpServer the HTTP server
     * @param options custom OAuth2 options
     */
    public KeycloakAuthenticationProvider(final HttpServer httpServer, final OAuth2Options options) {
        super(httpServer, options);
    }

    /**
     * Create an {@link OAuth2AuthenticationProvider} for Keycloak.
     *
     * @param httpServer the HTTP server
     * @param config the json configuration exported from Keycloak admin console
     */
    public KeycloakAuthenticationProvider(final HttpServer httpServer, final JSONObject config) {
        this(httpServer, OAuth2Flow.AUTH_CODE, config);
    }

    /**
     * Create an {@link OAuth2AuthenticationProvider} for Keycloak.
     *
     * @param httpServer the HTTP server
     * @param flow   the OAuth2 flow to use
     * @param config the JSON configuration exported from Keycloak admin console
     */
    public KeycloakAuthenticationProvider(final HttpServer httpServer, final OAuth2Flow flow, final JSONObject config) {
        super(httpServer, configure(flow, config));
    }

    /**
     * Create an {@link OAuth2Options} from a JSON configuration exported from Keycloak admin console.
     *
     * @param flow   the OAuth2 flow to use
     * @param config the json configuration exported from Keycloak admin console
     * @return the OAuth2 options
     */
    private static OAuth2Options configure(final OAuth2Flow flow, final JSONObject config) {
        final OAuth2Options options = new OAuth2Options();
        options.setFlow(flow);

        // retrieve client_id
        if (config.has("resource")) {
            options.setClientId(config.getString("resource"));
        }

        // keycloak conversion to OAuth2 options
        if (config.has("auth-server-url")) {
            options.setSite(config.getString("auth-server-url"));
        }

        // retrieve client_secret
        if (config.has("credentials") && config.getJSONObject("credentials").has("secret")) {
            options.setClientSecret(config.getJSONObject("credentials").getString("secret"));
        }

        if (config.has("realm")) {
            final String realm = config.getString("realm");
            options.setTenant(realm); // realm has the same role as the tenant

            options.setAuthorizationPath("/protocol/openid-connect/auth");
            options.setTokenPath("/protocol/openid-connect/token");
            options.setRevocationPath("/protocol/openid-connect/revoke");
            options.setUserInfoPath("/protocol/openid-connect/userinfo");
            options.setLogoutPath("/protocol/openid-connect/logout");
            // keycloak follows the RFC7662 (https://www.rfc-editor.org/rfc/rfc7662)
            options.setIntrospectionPath("/protocol/openid-connect/token/introspect");
            // keycloak follows the RFC7517 (https://www.rfc-editor.org/rfc/rfc7517)
            options.setJwkPath("/protocol/openid-connect/certs");
        }

        if (config.has("realm-public-key")) {
            options.addPubSecKeys(new PubSecKeyOptions()
                    .setAlgorithm("RS256")
                    .setBuffer(
                            // wrap the key with the right boundaries:
                            "-----BEGIN PUBLIC KEY-----\n" +
                                    config.getString("realm-public-key") +
                                    "\n-----END PUBLIC KEY-----\n"
                    ));
        }
        return options;
    }

    /**
     * Create an {@link OAuth2AuthenticationProvider} for OpenID Connect Discovery. The discovery will use the default
     * site in the configuration options and attempt to load the well-known descriptor. If a site is provided, then
     * it will be used to do the lookup.
     *
     * @param httpServer the HTTP server
     * @param options custom OAuth2 options
     * @return a future with the instantiated {@link OAuth2AuthenticationProvider}
     */
    public static CompletableFuture<OAuth2AuthenticationProvider> discover(final HttpServer httpServer,
                                                                           final OAuth2Options options) {
        return new KeycloakAuthenticationProvider(httpServer, options).discover();
    }
}
