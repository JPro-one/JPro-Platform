package one.jpro.platform.auth.example.simple;

import atlantafx.base.theme.CupertinoLight;
import com.jpro.webapi.WebAPI;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableMap;
import one.jpro.platform.auth.core.AuthAPI;
import one.jpro.platform.auth.core.authentication.User;
import one.jpro.platform.auth.core.oauth2.OAuth2Credentials;
import one.jpro.platform.auth.example.simple.page.ErrorPage;
import one.jpro.platform.auth.example.simple.page.LoginPage;
import one.jpro.platform.auth.example.simple.page.SignedInPage;
import one.jpro.platform.auth.routing.AuthFilters;
import one.jpro.platform.routing.Redirect;
import one.jpro.platform.routing.Route;
import one.jpro.platform.routing.RouteApp;
import one.jpro.platform.routing.dev.DevFilter;
import one.jpro.platform.sessions.SessionManager;
import org.json.JSONObject;
import simplefx.experimental.parts.FXFuture;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import static one.jpro.platform.routing.RouteUtils.getNode;
import static one.jpro.platform.routing.RouteUtils.viewFromNode;

/**
 * The {@link SimpleApp} class extends {@link RouteApp} to create a JavaFX application
 * with integrated Google OAuth authentication. It manages user sessions and error handling
 * using JavaFX properties. This class sets up routes for the application including
 * the login page, successful signed in page, and error handling page.
 *
 * <p>It uses environment variables to fetch Google Client ID and Secret for OAuth configuration.
 * The class provides a structured way to handle user authentication and maintain session state.
 *
 * <p>Routes are defined to handle different parts of the application, such as user authentication,
 * session management, and error display. This includes handling OAuth2 authentication with Google,
 * managing user sessions, and providing appropriate UI responses for different authentication states.
 *
 * <p>It also defines properties for the current user and any errors that occur during the
 * authentication process, allowing for easy integration with JavaFX UI components and data binding.
 *
 * <p>Note: This class requires additional context about {@code RouteApp}, {@code AuthAPI},
 * {@code OAuth2Credentials}, and other related classes/methods for its full functionality.
 *
 * @author Besmir Beqiri
 */
public class SimpleApp extends RouteApp {

    static final String GOOGLE_CLIENT_ID = System.getenv("GOOGLE_TEST_CLIENT_ID");
    static final String GOOGLE_CLIENT_SECRET = System.getenv("GOOGLE_TEST_CLIENT_SECRET");

    private final SessionManager sessionManager = new SessionManager("simple-app");

    @Override
    public Route createRoute() {
        Optional.ofNullable(CupertinoLight.class.getResource(new CupertinoLight().getUserAgentStylesheet()))
                .map(URL::toExternalForm)
                .ifPresent(getScene()::setUserAgentStylesheet);
        getScene().getStylesheets().add(one.jpro.platform.auth.example.showcase.LoginApp.class
                .getResource("/one/jpro/platform/auth/example/css/login.css").toExternalForm());

        final var googleAuthProvider = AuthAPI.googleAuth()
                .clientId(GOOGLE_CLIENT_ID)
                .clientSecret(GOOGLE_CLIENT_SECRET)
                .create(getStage());

        final var googleCredentials = new OAuth2Credentials()
                .setScopes(List.of("openid", "email"))
                .setRedirectUri("/auth/google");

        return Route.empty()
                .and(getNode("/", (r) -> new LoginPage(this, googleAuthProvider, googleCredentials)))
                .and(getNode("/user/signed-in", (r) -> new SignedInPage(this, googleAuthProvider)))
                .filter(DevFilter.create())
                .filter(AuthFilters.oauth2(googleAuthProvider, googleCredentials, user -> {
                    setUser(user);
                    return FXFuture.unit(new Redirect("/user/signed-in"));
                }, error -> FXFuture.unit(viewFromNode(new ErrorPage(error)))));
    }

    public  ObservableMap<String, String> getSession() {
        return (WebAPI.isBrowser()) ? sessionManager.getSession(getWebAPI())
                : sessionManager.getSession("user-session");
    }

    // User property
    private ObjectProperty<User> userProperty;

    public final User getUser() {
        User user = null;
        final var userJsonString = getSession().get("user");
        if (userJsonString != null) {
            System.out.println("userJsonString = " + userJsonString);
            final JSONObject userJson = new JSONObject(userJsonString);
            user = new User(userJson);
        }

        return userProperty == null ? user : userProperty.get();
    }

    public final void setUser(User value) {
        if (value != null) {
            getSession().put("user", value.toJSON().toString());
        } else {
            getSession().remove("user");
        }
        userProperty().set(value);
    }

    /**
     * The user property contains the currently logged-in user.
     *
     * @return the user property
     */
    public final ObjectProperty<User> userProperty() {
        if (userProperty == null) {
            userProperty = new SimpleObjectProperty<>(this, "user");
        }
        return userProperty;
    }
}
