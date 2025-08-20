package one.jpro.platform.auth.example.login.page;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import one.jpro.platform.auth.core.oauth2.provider.GoogleAuthenticationProvider;
import one.jpro.platform.auth.routing.AuthBasicOAuth2Filter;
import one.jpro.platform.auth.routing.AuthUIProvider;

import java.util.Optional;

/**
 * Login page.
 *
 * @author Besmir Beqiri
 */
public class LoginPage extends Page {

    public LoginPage(GoogleAuthenticationProvider authProvider, AuthUIProvider uiProvider) {
        getStyleClass().add("simple-page");

        final var googleProviderButton = uiProvider.createAuthenticationNode();

        getChildren().add(googleProviderButton);
    }

    /**
     * Create a button for the given provider.
     *
     * @param text the provider name
     * @return a button node
     */
    public Button createAuthProviderButton(String text) {
        final var iconView = new ImageView();
        iconView.setFitWidth(56);
        iconView.setFitHeight(56);
        Optional.ofNullable(getClass().getResourceAsStream("/images/" + text + "_Logo.png"))
                .map(inputStream -> new Image(inputStream, 0, 0, true, true))
                .ifPresent(iconView::setImage);

        final var loginButton = new Button("Login with\n" + text, iconView);
        loginButton.getStyleClass().addAll("login-button");
        return loginButton;
    }
}
