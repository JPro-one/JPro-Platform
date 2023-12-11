package one.jpro.platform.auth.example.login;

import atlantafx.base.theme.CupertinoLight;
import one.jpro.platform.auth.AuthAPI;
import one.jpro.platform.auth.example.login.page.*;
import one.jpro.platform.auth.oauth2.OAuth2Credentials;
import one.jpro.platform.routing.Route;
import one.jpro.platform.routing.dev.DevFilter;
import one.jpro.platform.routing.dev.StatisticsFilter;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import static one.jpro.platform.routing.RouteUtils.getNode;
import static one.jpro.platform.routing.RouteUtils.redirect;

/**
 * An example application to show how to use the Authorization module in general
 * combined with the Routing module and various supported authentication providers.
 *
 * @author Besmir Beqiri
 */
public class LoginApp extends BaseLoginApp {

    @Override
    public Route createRoute() {
        Optional.ofNullable(CupertinoLight.class.getResource(new CupertinoLight().getUserAgentStylesheet()))
                .map(URL::toExternalForm)
                .ifPresent(getScene()::setUserAgentStylesheet);
        getScene().getStylesheets().add(LoginApp.class
                .getResource("/one/jpro/platform/auth/example/css/login.css").toExternalForm());

        // Google Auth provider
        final var googleAuth = AuthAPI.googleAuth()
                .clientId(GOOGLE_CLIENT_ID)
                .clientSecret(GOOGLE_CLIENT_SECRET)
                .create(getStage());

        final var googleCredentials = new OAuth2Credentials()
                .setScopes(List.of("openid", "email"))
                .setRedirectUri("/auth/google");

        // Microsoft Auth provider
        final var microsoftAuth = AuthAPI.microsoftAuth()
                .clientId(AZURE_CLIENT_ID)
                .clientSecret(AZURE_CLIENT_SECRET)
                .tenant("common")
                .create(getStage());

        final var microsoftCredentials = new OAuth2Credentials()
                .setScopes(List.of("openid", "email"))
                .setRedirectUri("/auth/microsoft");

        // Keycloak Auth provider
        final var keycloakAuth = AuthAPI.keycloakAuth()
                .site("http://localhost:8080/realms/{realm}")
                .clientId("myclient")
                .clientSecret("5Rx63jCLPmTGhdqNaDDad0mqu5m0aOoN")
                .realm("myrealm")
                .create(getStage());

        final var keycloakCredentials = new OAuth2Credentials()
                .setScopes(List.of("openid", "email"))
                .setRedirectUri("/auth/keycloak");

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
                        .and(redirect("/google", "/user/console"))
                        .and(redirect("/microsoft", "/user/console"))
                        .and(redirect("/keycloak", "/user/console"))
                        .and(getNode("/error", (r) -> new ErrorPage(this))))
                .path("/provider", Route.empty()
                        .and(getNode("/google", (r) -> new AuthProviderPage(this, googleAuth, googleCredentials)))
                        .and(getNode("/microsoft", (r) -> new AuthProviderPage(this, microsoftAuth, microsoftCredentials)))
                        .and(getNode("/keycloak", (r) -> new AuthProviderPage(this, keycloakAuth, keycloakCredentials)))
                        .path("/discovery", Route.empty()
                                .and(getNode("/google", (r) -> new AuthProviderDiscoveryPage(this, googleAuth)))
                                .and(getNode("/microsoft", (r) -> new AuthProviderDiscoveryPage(this, microsoftAuth)))
                                .and(getNode("/keycloak", (r) -> new AuthProviderDiscoveryPage(this, keycloakAuth)))))
                .filter(DevFilter.create())
                .filter(StatisticsFilter.create())
                .filter(oauth2(googleAuth, googleCredentials, this::setUser, this::setError))
                .filter(oauth2(microsoftAuth, microsoftCredentials, this::setUser, this::setError))
                .filter(oauth2(keycloakAuth, keycloakCredentials, this::setUser, this::setError));
    }
}
