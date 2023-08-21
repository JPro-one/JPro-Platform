package one.jpro.auth.oath2.provider;

import one.jpro.auth.http.HttpServer;
import one.jpro.auth.jwt.JWTOptions;
import one.jpro.auth.oath2.OAuth2AuthenticationProvider;
import one.jpro.auth.oath2.OAuth2Flow;
import one.jpro.auth.oath2.OAuth2Options;

import java.util.concurrent.CompletableFuture;

/**
 * Simplified factory to create an {@link OAuth2AuthenticationProvider} for Microsoft.
 *
 * @author Besmir Beqiri
 */
public class MicrosoftAuthenticationProvider extends OAuth2AuthenticationProvider {

    public static final String COMMON_TENANT = "common";
    public static final String CONSUMERS_TENANT = "consumers";
    public static final String ORGANIZATIONS_TENANT = "organizations";

    /**
     * Create an {@link OAuth2AuthenticationProvider} for Microsoft.
     *
     * @param httpServer the HTTP server
     * @param options    custom OAuth2 options
     */
    public MicrosoftAuthenticationProvider(HttpServer httpServer, OAuth2Options options) {
        super(httpServer, options);
    }

    /**
     * Create an {@link OAuth2AuthenticationProvider} for Microsoft.
     *
     * @param httpServer   the HTTP server
     * @param clientId     the client id given to you by Microsoft
     * @param clientSecret the client secret given to you by Microsoft
     * @param tenant       the guid of your application
     */
    public MicrosoftAuthenticationProvider(HttpServer httpServer, String clientId, String clientSecret, String tenant) {
        super(httpServer, new OAuth2Options()
                .setFlow(OAuth2Flow.AUTH_CODE)
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setTenant(tenant)
                .setSite("https://login.microsoftonline.com/{tenant}/v2.0")
                .setTokenPath("https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token")
                .setAuthorizationPath("https://login.microsoftonline.com/{tenant}/oauth2/v2.0/authorize")
                .setUserInfoPath("https://graph.microsoft.com/oidc/userinfo")
                .setJwkPath("https://login.microsoftonline.com/{tenant}/discovery/v2.0/keys")
                .setLogoutPath("https://login.microsoftonline.com/{tenant}/oauth2/v2.0/logout")
                .setJWTOptions(new JWTOptions().setNonceAlgorithm("SHA-256")));
    }

    /**
     * Create an {@link OAuth2AuthenticationProvider} for OpenID Connect Discovery. The discovery will use the default
     * site in the configuration options and attempt to load the well-known descriptor. If a site is provided, then
     * it will be used to do the lookup.
     *
     * @param httpServer the HTTP server
     * @param options    custom OAuth2 options
     * @return a future with the instantiated {@link OAuth2AuthenticationProvider}
     */
    public static CompletableFuture<OAuth2AuthenticationProvider> discover(HttpServer httpServer, OAuth2Options options) {
        final String site = options.getSite() == null ?
                "https://login.microsoftonline.com/{tenant}/v2.0" : options.getSite();
        final JWTOptions jwtOptions = options.getJWTOptions() == null ?
                new JWTOptions() : new JWTOptions(options.getJWTOptions());
        // Microsoft default nonce algorithm
        if (jwtOptions.getNonceAlgorithm() == null) {
            jwtOptions.setNonceAlgorithm("SHA-256");
        }

        return new MicrosoftAuthenticationProvider(httpServer,
                new OAuth2Options(options)
                        // Microsoft OpenID does not return the same url where the request was sent to
                        .setValidateIssuer(false)
                        .setSite(site)
                        .setJWTOptions(jwtOptions)).discover();
    }
}
