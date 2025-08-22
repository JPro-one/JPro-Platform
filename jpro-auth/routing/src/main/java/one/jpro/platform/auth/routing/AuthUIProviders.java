package one.jpro.platform.auth.routing;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import one.jpro.platform.auth.core.basic.LoginPane;
import one.jpro.platform.auth.core.basic.UsernamePasswordCredentials;
import one.jpro.platform.auth.core.basic.provider.BasicAuthenticationProvider;
import one.jpro.platform.auth.core.oauth2.provider.OpenIDAuthenticationProvider;
import one.jpro.platform.auth.routing.buttons.GoogleLoginButton;
import one.jpro.platform.routing.Filter;
import one.jpro.platform.routing.LinkUtil;
import one.jpro.platform.routing.Response;
import org.jetbrains.annotations.NotNull;
import javafx.scene.layout.Pane;

import java.util.Arrays;
import java.util.function.Supplier;

public class AuthUIProviders {

    /**
     * Creates an AuthUIProvider for OAuth2/OpenID authentication.
     *
     * @param openidAuthProvider the OpenID authentication provider
     * @param userSession        the user session to be used
     * @return an AuthUIProvider that provides a button for OAuth2 authentication
     */
    public static AuthUIProvider createOAuth2(@NotNull OpenIDAuthenticationProvider openidAuthProvider,
                                                      @NotNull UserSession userSession) {
        return createOAuth2(openidAuthProvider, userSession, () -> new Button("Login"));
    }

    /**
     * Creates an AuthUIProvider for OAuth2/OpenID authentication with a custom button.
     *
     * @param openidAuthProvider the OpenID authentication provider
     * @param userSession        the user session to be used
     * @param createButton       a supplier that provides the button to be used for authentication
     * @return an AuthUIProvider that provides a button for OAuth2 authentication
     */
    public static AuthUIProvider createOAuth2(@NotNull OpenIDAuthenticationProvider openidAuthProvider,
                                                      @NotNull UserSession userSession,
                                                      @NotNull Supplier<Button> createButton) {
        return new AuthUIProvider() {
            @Override
            public Node createAuthenticationNode() {
                var button = createButton.get();
                button.setOnAction(event -> AuthBasicOAuth2Filter.authorize(button, openidAuthProvider));
                return button;
            }

            @Override
            public Filter createFilter() {
                return AuthBasicOAuth2Filter.create(openidAuthProvider, userSession,
                        user -> Response.redirect("/"), // That's ok, let the app handle it.
                        err -> {throw new RuntimeException(err);});
            }
        };
    }

    /**
     * Creates an AuthUIProvider for Google OAuth2/OpenID authentication.
     * This AuthUIProvider uses a specific button for Google login.
     */
    public static AuthUIProvider createGoogle(@NotNull OpenIDAuthenticationProvider openidAuthProvider,
                                              @NotNull UserSession userSession) {
        return createOAuth2(openidAuthProvider, userSession, () -> new GoogleLoginButton());
    }

    /**
     * Creates an AuthUIProvider for basic authentication using a username and password.
     */
    public static AuthUIProvider createBasicProvider(@NotNull BasicAuthenticationProvider authProvider,
                                                                    @NotNull UserSession userSession) {
        return new AuthUIProvider() {
            @Override
            public Node createAuthenticationNode() {
                return new VBox() {{
                    // Use LoginPane?
                    getChildren().add(new Label("Username:"));
                    var username = new TextField();
                    getChildren().add(username);
                    getChildren().add(new Label("Password:"));
                    var password = new PasswordField();
                    getChildren().add(password);
                    var button = new Button("Login");
                    getChildren().add(button);
                    var infoLabel = new Label();
                    getChildren().add(infoLabel);
                    Runnable loginAction = () -> {
                        authProvider.authenticate(new UsernamePasswordCredentials(username.getText(), password.getText())).thenAccept(user -> {
                            userSession.setUser(user);
                            LinkUtil.getSessionManager(button).gotoURL("/");
                        }).exceptionally(error -> {
                            error.printStackTrace();
                            infoLabel.setText(error.getCause().getMessage());
                            return null;
                        });
                    };
                    password.setOnAction(event -> {
                        loginAction.run();
                    });
                    button.setOnAction(event -> {
                        loginAction.run();
                    });
                }};
            }

            @Override
            public Filter createFilter() {
                return Filter.empty();
            }
        };
    }

    /**
     * Combines multiple AuthUIProviders into a single AuthUIProvider.
     */
    public static AuthUIProvider combine(AuthUIProvider... providers) {
        return combine(() -> new VBox(), providers);
    }

    /**
     *
     */
    public static AuthUIProvider combine(Supplier<Pane> paneSupplier, AuthUIProvider... providers) {
        return new AuthUIProvider() {
            @Override
            public Node createAuthenticationNode() {
                var pane = paneSupplier.get();
                for (var provider : providers) {
                    pane.getChildren().add(provider.createAuthenticationNode());
                }
                return pane;
            }

            @Override
            public Filter createFilter() {
                return Arrays.stream(providers)
                        .map(AuthUIProvider::createFilter)
                        .reduce(Filter::compose).orElse(Filter.empty());
            }
        };
    }
}
