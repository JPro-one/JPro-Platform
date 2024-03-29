package one.jpro.platform.auth.core.oauth2.provider;

import javafx.stage.Stage;
import one.jpro.platform.auth.core.jwt.JWTOptions;
import one.jpro.platform.auth.core.oauth2.OAuth2AuthenticationProvider;
import one.jpro.platform.auth.core.oauth2.OAuth2Flow;
import one.jpro.platform.auth.core.oauth2.OAuth2Options;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Simplified factory to create an {@link OAuth2AuthenticationProvider} for Microsoft.
 *
 * @author Besmir Beqiri
 */
public class MicrosoftAuthenticationProvider extends OpenIDAuthenticationProvider {

    /**
     * The common tenant identifier for Microsoft OAuth2.
     */
    public static final String COMMON_TENANT = "common";

    /**
     * The consumers tenant identifier for Microsoft OAuth2.
     */
    public static final String CONSUMERS_TENANT = "consumers";

    /**
     * The organizations tenant identifier for Microsoft OAuth2.
     */
    public static final String ORGANIZATIONS_TENANT = "organizations";

    public static final List<String> DEFAULT_SCOPES = List.of("openid", "profile", "email", "offline_access");

    /**
     * Create an {@link OAuth2AuthenticationProvider} for Microsoft.
     *
     * @param stage   the JavaFX application stage
     * @param options the custom set of OAuth2 options which include configuration like
     *                client ID, client secret, scopes, and other OAuth2 parameters.
     */
    public MicrosoftAuthenticationProvider(@Nullable final Stage stage, @NotNull final OAuth2Options options) {
        super(stage, options);
    }

    /**
     * Create an {@link OAuth2AuthenticationProvider} for Microsoft.
     *
     * @param stage        the JavaFX application stage
     * @param clientId     the client ID issued by Microsoft for your application.
     * @param clientSecret the client secret issued by Microsoft for securing your application.
     * @param tenant       the GUID or tenant ID that represents your application's directory.
     */
    public MicrosoftAuthenticationProvider(@Nullable final Stage stage,
                                           @NotNull final String clientId,
                                           @NotNull final String clientSecret,
                                           @NotNull final String tenant) {
        super(stage, new OAuth2Options()
                .setFlow(OAuth2Flow.AUTH_CODE)
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setTenant(tenant)
                .setSupportedScopes(DEFAULT_SCOPES)
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
     * @param stage   the JavaFX application stage
     * @param options custom OAuth2 options
     * @return a future with the instantiated {@link OAuth2AuthenticationProvider}
     */
    public static CompletableFuture<OpenIDAuthenticationProvider> discover(Stage stage, OAuth2Options options) {
        final String site = options.getSite() == null ?
                "https://login.microsoftonline.com/{tenant}/v2.0" : options.getSite();
        final JWTOptions jwtOptions = options.getJWTOptions() == null ?
                new JWTOptions() : new JWTOptions(options.getJWTOptions());
        // Microsoft default nonce algorithm
        if (jwtOptions.getNonceAlgorithm() == null) {
            jwtOptions.setNonceAlgorithm("SHA-256");
        }

        return new MicrosoftAuthenticationProvider(stage,
                new OAuth2Options(options)
                        // Microsoft OpenID does not return the same url where the request was sent to
                        .setValidateIssuer(false)
                        .setSite(site)
                        .setJWTOptions(jwtOptions)).discover();
    }
}
