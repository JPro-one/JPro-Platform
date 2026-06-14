package one.jpro.platform.auth.routing;

import com.jpro.webapi.WebAPI;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import one.jpro.platform.auth.core.AuthAPI;
import one.jpro.platform.auth.core.authentication.User;
import one.jpro.platform.auth.core.basic.UserManager;
import one.jpro.platform.auth.core.oauth2.provider.OpenIDAuthenticationProvider;
import one.jpro.platform.auth.routing.buttons.GoogleLoginButton;
import one.jpro.platform.routing.Request;
import one.jpro.platform.routing.Response;
import one.jpro.platform.routing.Route;
import one.jpro.platform.routing.RouteApp;
import one.jpro.platform.routing.Transformer;
import one.jpro.platform.session.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * High-level entry point for adding authentication to a {@link RouteApp}.
 *
 * <p>Declare the login methods you want, then bind to the app. The result owns the user
 * session, builds a combined login UI, and produces the route filter — so adding Google,
 * generic OAuth2 or username/password login is a single call each, configured in one place
 * that is easy to swap for tests or a desktop local user (see {@code dummy(...)} /
 * {@code defaultUser(...)}).</p>
 *
 * <pre>{@code
 * RoutingAuth auth = RoutingAuth.config()
 *         .google(CLIENT_ID, CLIENT_SECRET)
 *         .usernamePassword(userManager)
 *         .loginRedirect("/home")
 *         .build(this);
 *
 * return Route.empty()
 *         .and(Route.get("/home", r -> Response.node(new HomePage())))
 *         .transform(auth.requireLogin())   // gate the route(s)
 *         .transform(auth.install());       // serve /login + handle callbacks
 * }</pre>
 *
 * For a desktop local user, automated tests, or local development, swap the configuration:
 * <pre>{@code
 * RoutingAuth auth = RoutingAuth.config()
 *         .dummy("tester", Set.of("USER"))   // one-click fake login
 *         .build(this);
 * // or, already signed in with no UI (desktop "local user" / headless tests):
 * RoutingAuth auth = RoutingAuth.config()
 *         .defaultUser("localuser", Set.of("USER"))
 *         .build(this);
 * }</pre>
 */
public final class RoutingAuth {

    private final UserSession userSession;
    private final AuthUIProvider combined;
    private final String loginUrl;
    private final Function<Request, Response> loginResponse;

    private RoutingAuth(UserSession userSession, AuthUIProvider combined,
                        String loginUrl, Function<Request, Response> loginResponse) {
        this.userSession = userSession;
        this.combined = combined;
        this.loginUrl = loginUrl;
        this.loginResponse = loginResponse;
    }

    /** Starts a new configuration. */
    public static Builder config() {
        return new Builder();
    }

    /** The login UI for all configured methods, stacked together. */
    public Node loginScreen() {
        return combined.createAuthenticationNode();
    }

    /**
     * The transform that wires auth into the app: it serves the login page at the configured
     * {@code loginUrl} and handles the login callbacks (e.g. OAuth2 redirects). Apply it to the
     * whole route (outermost). With this in place you don't need to declare a {@code /login}
     * route yourself — adding auth to an existing app is just this transform plus
     * {@link #requireLogin()} on the parts you want protected.
     */
    public Transformer install() {
        Transformer callbacks = combined.createTransformer();
        return route -> {
            Route inner = callbacks.apply(route);
            return request -> request.getPath().equals(loginUrl)
                    ? loginResponse.apply(request)
                    : inner.apply(request);
        };
    }

    /**
     * Route filter that protects whatever route it wraps: when the user is logged in the wrapped
     * route is used unchanged; otherwise every request is redirected to the login page (the wrapped
     * route — and any page it would build — is never invoked while logged out).
     *
     * <p>Apply it to the whole route to gate the entire app, or place a guarded sub-route
     * <em>after</em> your public routes (so they match first) to mix public and protected pages.
     * Apply {@link #install()} to the whole route as well, so login callbacks are always handled.</p>
     */
    public Transformer requireLogin() {
        return route -> request -> userSession.isLoggedIn()
                ? route.apply(request)
                : Response.redirect(loginUrl);
    }

    /** The user session backing this configuration. */
    public UserSession userSession() {
        return userSession;
    }

    /** The currently authenticated user, or {@code null}. */
    public User getUser() {
        return userSession.getUser();
    }

    /** Whether a user is currently authenticated. */
    public boolean isLoggedIn() {
        return userSession.isLoggedIn();
    }

    /** Logs the current user out. */
    public void logout() {
        userSession.logout();
    }

    /**
     * Fluent configuration for {@link RoutingAuth}. Each {@code add a method} call registers
     * one login option; {@link #build(RouteApp)} materializes them (it must be called once the
     * stage exists, i.e. from within {@code createRoute()}).
     */
    public static final class Builder {

        private final List<BiFunction<RouteApp, UserSession, AuthUIProvider>> methods = new ArrayList<>();
        private User autoLoginUser;
        private String sessionName = "app";
        private String loginUrl = "/login";
        private Function<Request, Response> loginResponse;
        private String loginRedirect = "/";
        private Consumer<User> onLogin = user -> {};
        private Function<Throwable, Response> onError =
                error -> Response.node(new Label("Login failed: " + error.getMessage()));

        private Builder() {}

        /** Namespaces the on-disk session storage (default {@code "app"}). */
        public Builder sessionName(String sessionName) {
            this.sessionName = sessionName;
            return this;
        }

        /** The URL of the login page (default {@code "/login"}); {@link RoutingAuth#requireLogin()}
         * redirects there and {@link RoutingAuth#install()} serves it. */
        public Builder loginUrl(String path) {
            this.loginUrl = path;
            return this;
        }

        /** Customizes what is shown at the login URL (default: the combined login screen). */
        public Builder loginResponse(Function<Request, Response> loginResponse) {
            this.loginResponse = loginResponse;
            return this;
        }

        /** Where to navigate after a successful login (default {@code "/"}). */
        public Builder loginRedirect(String path) {
            this.loginRedirect = path;
            return this;
        }

        /** Hook invoked with the user on every successful login (e.g. logging, app state). */
        public Builder onLogin(Consumer<User> onLogin) {
            this.onLogin = onLogin;
            return this;
        }

        /** How to render a failed login (default: a simple message node). */
        public Builder onError(Function<Throwable, Response> onError) {
            this.onError = onError;
            return this;
        }

        /** Adds "Sign in with Google" using the given OAuth2 client credentials. */
        public Builder google(String clientId, String clientSecret) {
            return google(clientId, clientSecret, "/auth/google");
        }

        /** Adds "Sign in with Google" with an explicit redirect URI. */
        public Builder google(String clientId, String clientSecret, String redirectUri) {
            methods.add((app, session) -> {
                var provider = AuthAPI.googleAuth()
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .redirectUri(redirectUri)
                        .create(app.getStage());
                return AuthUIProviders.createOAuth2(provider, session, GoogleLoginButton::new, onLoginRedirect(), onError);
            });
            return this;
        }

        /** Adds a generic OAuth2/OpenID login for an already-configured provider. */
        public Builder oauth2(OpenIDAuthenticationProvider provider) {
            methods.add((app, session) -> AuthUIProviders.createOAuth2(provider, session,
                    () -> new Button("Login"), onLoginRedirect(), onError));
            return this;
        }

        /** Adds username/password login backed by the given user manager. */
        public Builder usernamePassword(UserManager userManager, String... roles) {
            methods.add((app, session) -> {
                var provider = AuthAPI.basicAuth().userManager(userManager).roles(roles).create();
                return AuthUIProviders.createBasicProvider(provider, session, onLogin, loginRedirect);
            });
            return this;
        }

        /** Adds a one-click fake login as the given user — for local testing. */
        public Builder dummy(String name, Set<String> roles) {
            methods.add((app, session) -> AuthUIProviders.dummy(new User(name, roles), session, loginRedirect, onLogin));
            return this;
        }

        /**
         * Logs in as the given user immediately and without any UI. Useful for a desktop
         * "local user" (e.g. always signed in as {@code localuser} on desktop, real login on
         * the web) or for headless automated tests.
         */
        public Builder defaultUser(User user) {
            this.autoLoginUser = user;
            return this;
        }

        /** Convenience for {@link #defaultUser(User)} with a name and roles. */
        public Builder defaultUser(String name, Set<String> roles) {
            return defaultUser(new User(name, roles));
        }

        /** Materializes the configuration against the running application. */
        public RoutingAuth build(RouteApp app) {
            ObservableMap<String, String> session = WebAPI.isBrowser()
                    ? new SessionManager(sessionName).getSession(app.getWebAPI())
                    : new SessionManager(sessionName).getSession("user-session");
            UserSession userSession = new UserSession(session);

            if (autoLoginUser != null && !userSession.isLoggedIn()) {
                userSession.setUser(autoLoginUser);
                onLogin.accept(autoLoginUser);
            }

            List<AuthUIProvider> providers = new ArrayList<>();
            for (var method : methods) {
                providers.add(method.apply(app, userSession));
            }
            AuthUIProvider combined = AuthUIProviders.combine(providers.toArray(AuthUIProvider[]::new));
            Function<Request, Response> loginResp = loginResponse != null ? loginResponse
                    : request -> Response.node(combined.createAuthenticationNode());
            return new RoutingAuth(userSession, combined, loginUrl, loginResp);
        }

        // OAuth2 success: run the onLogin hook, then redirect to loginRedirect.
        private Function<User, Response> onLoginRedirect() {
            return user -> {
                onLogin.accept(user);
                return Response.redirect(loginRedirect);
            };
        }
    }
}
