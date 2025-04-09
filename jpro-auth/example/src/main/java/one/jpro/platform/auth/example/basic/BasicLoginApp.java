package one.jpro.platform.auth.example.basic;

import atlantafx.base.theme.CupertinoLight;
import com.jpro.webapi.WebAPI;
import javafx.collections.ObservableMap;
import one.jpro.platform.auth.core.AuthAPI;
import one.jpro.platform.auth.core.basic.InMemoryUserManager;
import one.jpro.platform.auth.core.basic.UserManager;
import one.jpro.platform.auth.core.basic.UsernamePasswordCredentials;
import one.jpro.platform.auth.core.basic.provider.BasicAuthenticationProvider;
import one.jpro.platform.auth.example.basic.page.ErrorPage;
import one.jpro.platform.auth.example.basic.page.LoginPage;
import one.jpro.platform.auth.example.basic.page.SignedInPage;
import one.jpro.platform.auth.example.oauth.OAuthApp;
import one.jpro.platform.auth.routing.AuthBasicFilter;
import one.jpro.platform.auth.routing.UserSession;
import one.jpro.platform.routing.Response;
import one.jpro.platform.routing.Route;
import one.jpro.platform.routing.RouteApp;
import one.jpro.platform.routing.dev.DevFilter;
import one.jpro.platform.session.SessionManager;

import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The {@link BasicLoginApp} class is a specialized implementation of {@link RouteApp} designed for handling
 * user authentication and session management in a web application context. This class incorporates
 * {@link BasicAuthenticationProvider} to facilitate basic authentication using username and password credentials.
 * Key Features:
 * <ul>
 *   <li>Authentication: Utilizes {@link BasicAuthenticationProvider} for authenticating users. It defines specific
 *   roles such as "USER" and "ADMIN" and creates an authentication provider instance accordingly.</li>
 *   <li>Credentials Management: Manages user credentials using {@link UsernamePasswordCredentials} which are
 *   essential for the authentication process.</li>
 *   <li>Session Management: Employs {@link SessionManager} for maintaining user sessions. This enables the
 *   application to persist user state across different requests. It distinguishes between browser and non-browser
 *   environments for session handling.</li>
 *   <li>Routing: Defines application routing using the {@link Route} class. It sets up various routes like the root
 *   path and authenticated user path, and integrates authentication filters to protect these routes.</li>
 *   <li>Error Handling: Implements error handling within the authentication flow, ensuring that authentication
 *   failures are managed gracefully.</li>
 *   <li>Routing: Defines application routing using the {@link Route} class. It sets up various routes like the root
 *   path and authenticated user path, and integrates authentication filters to protect these routes.</li>
 *   <li>User State Management: Provides methods for retrieving and setting the authenticated user in the session,
 *   thereby managing the user's state throughout the application.</li>
 * </ul>
 *
 * @author Besmir Beqiri
 */
public class BasicLoginApp extends RouteApp {

    private final UserManager userManager = new InMemoryUserManager();

    private final BasicAuthenticationProvider basicAuthProvider = AuthAPI.basicAuth()
            .userManager(userManager)
            .roles("USER", "ADMIN")
            .create();
    private final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials();

    private static final SessionManager sessionManager = new SessionManager("basic-login-app");
    ObservableMap<String, String> session;
    public UserSession userSession;

    public BasicLoginApp() {
        userManager.createUser(new UsernamePasswordCredentials("admin", "password"),
                Set.of(), Map.of("enabled", Boolean.TRUE)).join();
    }

    @Override
    public Route createRoute() {
        session = (WebAPI.isBrowser()) ? sessionManager.getSession(getWebAPI()) :
                sessionManager.getSession("user-session");
        userSession = new UserSession(session);

        Optional.ofNullable(CupertinoLight.class.getResource(new CupertinoLight().getUserAgentStylesheet()))
                .map(URL::toExternalForm)
                .ifPresent(getScene()::setUserAgentStylesheet);
        getScene().getStylesheets().add(OAuthApp.class
                .getResource("/one/jpro/platform/auth/example/css/login.css").toExternalForm());

        return Route.empty()
                .and(Route.get("/", request -> Response.node(new LoginPage(basicAuthProvider, credentials))))
                .when(request -> isUserAuthenticated(), Route.empty()
                        .and(Route.get("/user/signed-in", request -> Response.node(new SignedInPage(this)))))
                .filter(AuthBasicFilter.create(basicAuthProvider, credentials, user -> {
                    userSession.setUser(user);
                    return Response.redirect("/user/signed-in");
                }, error -> Response.node(new ErrorPage(error))))
                .filter(DevFilter.create());
    }

    private boolean isUserAuthenticated() {
        return userSession.getUser() != null;
    }
}
