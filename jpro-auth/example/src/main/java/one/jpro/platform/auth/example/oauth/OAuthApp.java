package one.jpro.platform.auth.example.oauth;

import atlantafx.base.theme.CupertinoLight;
import one.jpro.platform.auth.core.AuthAPI;
import one.jpro.platform.auth.core.oauth2.provider.OpenIDAuthenticationProvider;
import one.jpro.platform.auth.example.oauth.page.*;
import one.jpro.platform.auth.routing.OAuth2Filter;
import one.jpro.platform.routing.Filter;
import one.jpro.platform.routing.Redirect;
import one.jpro.platform.routing.Response;
import one.jpro.platform.routing.Route;
import one.jpro.platform.routing.dev.DevFilter;
import one.jpro.platform.routing.dev.StatisticsFilter;
import simplefx.experimental.parts.FXFuture;

import java.net.URL;
import java.util.Optional;

import static one.jpro.platform.routing.RouteUtils.getNode;

/**
 * A showcase application to show how to use the Authorization module in general
 * combined with the Routing module and various supported authentication providers.
 *
 * @author Besmir Beqiri
 */
public class OAuthApp extends BaseOAuthApp {

    @Override
    public Route createRoute() {
        Optional.ofNullable(CupertinoLight.class.getResource(new CupertinoLight().getUserAgentStylesheet()))
                .map(URL::toExternalForm)
                .ifPresent(getScene()::setUserAgentStylesheet);
        getScene().getStylesheets().add(OAuthApp.class
                .getResource("/one/jpro/platform/auth/example/css/login.css").toExternalForm());

        // Google Auth provider
        final var googleAuth = AuthAPI.googleAuth()
                .clientId(GOOGLE_CLIENT_ID)
                .clientSecret(GOOGLE_CLIENT_SECRET)
                .redirectUri("/auth/google")
                .create(getStage());

        // Microsoft Auth provider
        final var microsoftAuth = AuthAPI.microsoftAuth()
                .clientId(AZURE_CLIENT_ID)
                .clientSecret(AZURE_CLIENT_SECRET)
                .tenant("common")
                .redirectUri("/auth/microsoft")
                .create(getStage());

        // Keycloak Auth provider
        final var keycloakAuth = AuthAPI.keycloakAuth()
                .site("http://localhost:8080/realms/{realm}")
                .clientId(KEYCLOAK_CLIENT_ID)
                .clientSecret(KEYCLOAK_CLIENT_SECRET)
                .realm("myrealm")
                .redirectUri("/auth/keycloak")
                .create(getStage());

        return Route.empty()
                .and(getNode("/", (r) -> new LoginPage(this)))
                .path("/user", Route.empty()
                        .and(getNode("/console", (r) -> new SignedInUserPage(this)))
                        .and(getNode("/auth-info", (r) -> new AuthInfoPage(this)))
                        .and(getNode("/introspect-token", (r) -> new IntrospectionTokenPage(this)))
                        .and(getNode("/refresh-token", (r) -> new RefreshTokenPage(this)))
                        .and(getNode("/revoke-token", (r) -> new LoginPage(this)))
                        .and(getNode("/user-info", (r) -> new UserInfoPage(this)))
                        .and(getNode("/logout", (r) -> new LoginPage(this))))
                .path("/auth", Route.empty()
                        .and(getNode("/error", (r) -> new ErrorPage(this))))
                .path("/provider", Route.empty()
                        .and(getNode("/google", (r) -> new AuthProviderPage(this, googleAuth)))
                        .and(getNode("/microsoft", (r) -> new AuthProviderPage(this, microsoftAuth)))
                        .and(getNode("/keycloak", (r) -> new AuthProviderPage(this, keycloakAuth)))
                        .path("/discovery", Route.empty()
                                .and(getNode("/google", (r) -> new AuthProviderDiscoveryPage(this, googleAuth)))
                                .and(getNode("/microsoft", (r) -> new AuthProviderDiscoveryPage(this, microsoftAuth)))
                                .and(getNode("/keycloak", (r) -> new AuthProviderDiscoveryPage(this, keycloakAuth)))))
                .filter(DevFilter.create())
                .filter(StatisticsFilter.create())
                .filter(oauth2Filter(googleAuth))
                .filter(oauth2Filter(microsoftAuth))
                .filter(oauth2Filter(keycloakAuth));
    }

    /**
     * Creates and configures an OAuth2 filter using the provided authentication provider and credentials.
     * This filter manages the authentication flow, handling both successful authentication and errors.
     * On successful authentication, it sets the user and authentication provider, and redirects to the user console.
     * In case of an error, it sets the error details and redirects to the authentication error path.
     *
     * @param openIDAuthProvider The OAuth2 authentication provider used for the authentication process.
     * @return A {@link Filter} object configured for OAuth2 authentication flow.
     */
    private Filter oauth2Filter(OpenIDAuthenticationProvider openIDAuthProvider) {
        return OAuth2Filter.create(openIDAuthProvider, user -> {
            setUser(user);
            setAuthProvider(openIDAuthProvider);
            return Response.redirect(USER_CONSOLE_PATH);
        }, error -> {
            setError(error);
            return Response.redirect(AUTH_ERROR_PATH);
        });
    }
}
