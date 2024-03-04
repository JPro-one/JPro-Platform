package one.jpro.platform.auth.example.oauth;

import atlantafx.base.theme.CupertinoLight;
import com.jpro.webapi.WebAPI;
import javafx.collections.ObservableMap;
import one.jpro.platform.auth.core.AuthAPI;
import one.jpro.platform.auth.core.oauth2.provider.OpenIDAuthenticationProvider;
import one.jpro.platform.auth.example.oauth.page.*;
import one.jpro.platform.auth.routing.AuthOAuth2Filter;
import one.jpro.platform.auth.routing.UserAPI;
import one.jpro.platform.routing.Filter;
import one.jpro.platform.routing.Response;
import one.jpro.platform.routing.Route;
import one.jpro.platform.routing.dev.DevFilter;
import one.jpro.platform.routing.dev.StatisticsFilter;
import one.jpro.platform.session.SessionManager;

import java.net.URL;
import java.util.Optional;
import static one.jpro.platform.routing.Route.get;

/**
 * A showcase application to show how to use the Authorization module in general
 * combined with the Routing module and various supported authentication providers.
 *
 * @author Besmir Beqiri
 */
public class OAuthApp extends BaseOAuthApp {

    private static final SessionManager sessionManager = new SessionManager("oauth-app");

    ObservableMap<String, String> session;
    public UserAPI userAPI;

    @Override
    public Route createRoute() {
        session = (WebAPI.isBrowser()) ? sessionManager.getSession(getWebAPI())
                : sessionManager.getSession("user-session");
        userAPI = new UserAPI(session);

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
                .and(get("/", request -> Response.node(new LoginPage(this))))
                .path("/user", Route.empty()
                        .and(get("/console", request -> Response.node(new SignedInUserPage(this))))
                        .and(get("/auth-info", request -> Response.node(new AuthInfoPage(this))))
                        .and(get("/introspect-token", request -> Response.node(new IntrospectionTokenPage(this))))
                        .and(get("/refresh-token", request -> Response.node(new RefreshTokenPage(this))))
                        .and(get("/revoke-token", request -> Response.node(new LoginPage(this))))
                        .and(get("/user-info", request -> Response.node(new UserInfoPage(this))))
                        .and(get("/logout", request -> Response.node(new LoginPage(this)))))
                .path("/auth", Route.empty()
                        .and(get("/error", request -> Response.node(new ErrorPage(this)))))
                .path("/provider", Route.empty()
                        .and(get("/google", request -> Response.node(new AuthProviderPage(this, googleAuth))))
                        .and(get("/microsoft", request -> Response.node(new AuthProviderPage(this, microsoftAuth))))
                        .and(get("/keycloak", request -> Response.node(new AuthProviderPage(this, keycloakAuth))))
                        .path("/discovery", Route.empty()
                                .and(get("/google", request -> Response.node(new AuthProviderDiscoveryPage(this, googleAuth))))
                                .and(get("/microsoft", request -> Response.node(new AuthProviderDiscoveryPage(this, microsoftAuth))))
                                .and(get("/keycloak", request -> Response.node(new AuthProviderDiscoveryPage(this, keycloakAuth))))))
                .filter(oauth2Filter(googleAuth))
                .filter(oauth2Filter(microsoftAuth))
                .filter(oauth2Filter(keycloakAuth))
                .filter(StatisticsFilter.create())
                .filter(DevFilter.create());
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
        return AuthOAuth2Filter.create(openIDAuthProvider, userAPI, user -> {
            setAuthProvider(openIDAuthProvider);
            return Response.redirect(USER_CONSOLE_PATH);
        }, error -> {
            setError(error);
            return Response.redirect(AUTH_ERROR_PATH);
        });
    }
}
