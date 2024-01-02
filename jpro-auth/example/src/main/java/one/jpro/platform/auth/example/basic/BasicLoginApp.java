package one.jpro.platform.auth.example.basic;

import atlantafx.base.theme.CupertinoLight;
import com.jpro.webapi.WebAPI;
import javafx.collections.ObservableMap;
import one.jpro.platform.auth.core.AuthAPI;
import one.jpro.platform.auth.core.authentication.User;
import one.jpro.platform.auth.core.basic.BasicAuthenticationProvider;
import one.jpro.platform.auth.core.basic.UsernamePasswordCredentials;
import one.jpro.platform.auth.example.basic.page.ErrorPage;
import one.jpro.platform.auth.example.basic.page.LoginPage;
import one.jpro.platform.auth.example.basic.page.SignedInPage;
import one.jpro.platform.auth.example.oauth.OAuthApp;
import one.jpro.platform.auth.routing.AuthFilter;
import one.jpro.platform.routing.Response;
import one.jpro.platform.routing.Route;
import one.jpro.platform.routing.RouteApp;
import one.jpro.platform.routing.dev.DevFilter;
import one.jpro.platform.sessions.SessionManager;
import org.json.JSONObject;

import java.net.URL;
import java.util.Optional;
import java.util.Set;

/**
 * The {@link BasicLoginApp} class is a specialized implementation of {@link RouteApp} designed for handling
 * user authentication and session management in a web application context. This class incorporates
 * {@link BasicAuthenticationProvider} to facilitate basic authentication using username and password credentials.
 * <p>
 * Key Features:
 * - Authentication: Utilizes {@link BasicAuthenticationProvider} for authenticating users. It defines specific
 * roles such as "USER" and "ADMIN" and creates an authentication provider instance accordingly.
 * - Credentials Management: Manages user credentials using {@link UsernamePasswordCredentials} which are essential
 * for the authentication process.
 * - Session Management: Employs {@link SessionManager} for maintaining user sessions. This enables the application
 * to persist user state across different requests. It distinguishes between browser and non-browser environments
 * for session handling.
 * - Routing: Defines application routing using the {@link Route} class. It sets up various routes like the root path
 * and authenticated user path, and integrates authentication filters to protect these routes.
 * - Error Handling: Implements error handling within the authentication flow, ensuring that authentication failures
 * are managed gracefully.
 * - User State Management: Provides methods for retrieving and setting the authenticated user in the session,
 * thereby managing the user's state throughout the application.
 *
 * @author Besmir Beqiri
 */
public class BasicLoginApp extends RouteApp {

    private final BasicAuthenticationProvider basicAuthProvider = AuthAPI.basicAuth()
            .roles(Set.of("USER", "ADMIN"))
            .create();
    private final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials();

    private static final SessionManager sessionManager = new SessionManager("basic-login-app");
    ObservableMap<String, String> session;

    @Override
    public Route createRoute() {
        session = (WebAPI.isBrowser()) ? sessionManager.getSession(getWebAPI())
                : sessionManager.getSession("user-session");

        Optional.ofNullable(CupertinoLight.class.getResource(new CupertinoLight().getUserAgentStylesheet()))
                .map(URL::toExternalForm)
                .ifPresent(getScene()::setUserAgentStylesheet);
        getScene().getStylesheets().add(OAuthApp.class
                .getResource("/one/jpro/platform/auth/example/css/login.css").toExternalForm());

        return Route.empty()
                .and(Route.get("/", (r) -> Response.node(new LoginPage(this, basicAuthProvider, credentials))))
                .when(request -> isUserAuthenticated(), Route.empty()
                        .and(Route.get("/user/signed-in", request -> Response.node(new SignedInPage(this)))))
                .filter(DevFilter.create())
                .filter(AuthFilter.create(basicAuthProvider, credentials, user -> {
                    setUser(user);
                    return Response.redirect("/user/signed-in");
                }, error -> Response.node(new ErrorPage(error))));
    }

    public final User getUser() {
        final var userJsonString = session.get("user");
        if (userJsonString != null) {
            final JSONObject userJson = new JSONObject(userJsonString);
            return new User(userJson);
        } else {
            return null;
        }
    }

    public final void setUser(User value) {
        if (value != null) {
            session.put("user", value.toJSON().toString());
        } else {
            session.remove("user");
        }
    }

    private boolean isUserAuthenticated() {
        return getUser() != null;
    }
}
