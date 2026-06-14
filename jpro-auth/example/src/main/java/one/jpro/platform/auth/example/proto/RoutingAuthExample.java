package one.jpro.platform.auth.example.proto;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import one.jpro.platform.auth.core.basic.InMemoryUserManager;
import one.jpro.platform.auth.core.basic.UserManager;
import one.jpro.platform.auth.core.basic.UsernamePasswordCredentials;
import one.jpro.platform.auth.routing.RoutingAuth;
import one.jpro.platform.routing.LinkUtil;
import one.jpro.platform.routing.Response;
import one.jpro.platform.routing.Route;
import one.jpro.platform.routing.RouteApp;

import java.util.Map;
import java.util.Set;

/**
 * Runnable demo of the {@link RoutingAuth} prototype — public + protected pages, with a
 * redirect to a real /login page. No network required.
 *
 * <p>Run with: {@code ./gradlew jpro-auth:example:run -Psample=routing-auth}</p>
 * The username/password form accepts {@code admin} / {@code password}.
 */
public class RoutingAuthExample extends RouteApp {

    private final UserManager userManager = new InMemoryUserManager();
    private RoutingAuth auth;

    public RoutingAuthExample() {
        userManager.createUser(new UsernamePasswordCredentials("admin", "password"),
                Set.of("ADMIN"), Map.of("enabled", Boolean.TRUE)).join();
    }

    @Override
    public Route createRoute() {
        auth = RoutingAuth.config()
                .dummy("tester", Set.of("USER"))
                .usernamePassword(userManager, "ADMIN")
                .loginUrl("/login")         // where requireLogin() sends logged-out users
                .loginResponse(r -> Response.node(customLoginPage()))  // custom login page
                .loginRedirect("/secret")   // where to land after a successful login
                .onLogin(user -> System.out.println("Logged in: " + user.getName()))
                .build(this);

        // A protected sub-route: only this is gated; everything else stays public.
        Route protectedRoutes = Route.empty()
                .and(Route.get("/secret", request -> Response.node(secretPage(auth))))
                .transform(auth.requireLogin());

        return Route.empty()
                .and(Route.get("/", request -> Response.node(publicHome())))
                .and(protectedRoutes)
                .transform(auth.filter());  // serves /login + handles callbacks (no /login route needed)
    }

    private Node publicHome() {
        var title = new Label("Public home (no login needed)");
        var toSecret = new Button("Go to /secret (protected)");
        toSecret.setOnAction(e -> LinkUtil.gotoPage(toSecret, "/secret"));
        return page(title, toSecret);
    }

    // Custom login page that reuses the provider login buttons via auth.loginScreen().
    private Node customLoginPage() {
        return page(new Label("Please sign in"), auth.loginScreen());
    }

    private Node secretPage(RoutingAuth auth) {
        var welcome = new Label("Secret page — welcome, " + auth.getUser().getName()
                + " " + auth.getUser().getRoles());
        var logout = new Button("Logout");
        logout.setOnAction(e -> {
            auth.logout();
            LinkUtil.gotoPage(logout, "/");
        });
        return page(welcome, logout);
    }

    private Node page(Node... children) {
        var box = new VBox(12, children);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        return box;
    }
}
