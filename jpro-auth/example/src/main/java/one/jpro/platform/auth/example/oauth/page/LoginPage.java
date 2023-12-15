package one.jpro.platform.auth.example.oauth.page;

import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import one.jpro.platform.auth.example.oauth.OAuthApp;

import static one.jpro.platform.routing.LinkUtil.gotoPage;

/**
 * Main login page.
 *
 * @author Besmir Beqiri
 */
public class LoginPage extends Page {

    public static final String GOOGLE_PROVIDER_PATH = "/provider/google";
    public static final String MICROSOFT_PROVIDER_PATH = "/provider/microsoft";
    public static final String KEYCLOAK_PROVIDER_PATH = "/provider/keycloak";

    public LoginPage(OAuthApp loginApp) {
        final var headerLabel = new Label("Authentication Module");
        headerLabel.getStyleClass().add("header-label");

        final var selectLabel = new Label("Select an authentication provider:");
        selectLabel.getStyleClass().add("header2-label");

        final var googleProviderButton = loginApp.createAuthProviderButton("Google");
        googleProviderButton.setOnAction(event -> gotoPage(googleProviderButton, GOOGLE_PROVIDER_PATH));

        final var microsoftProviderButton = loginApp.createAuthProviderButton("Microsoft");
        microsoftProviderButton.setOnAction(event -> gotoPage(microsoftProviderButton, MICROSOFT_PROVIDER_PATH));

        final var keycloakProviderButton = loginApp.createAuthProviderButton("Keycloak");
        keycloakProviderButton.setOnAction(event -> gotoPage(keycloakProviderButton, KEYCLOAK_PROVIDER_PATH));

        final var tilePane = new TilePane(googleProviderButton, microsoftProviderButton, keycloakProviderButton);
        tilePane.getStyleClass().add("tile-pane");
        VBox.setVgrow(tilePane, Priority.ALWAYS);

        final var pane = new VBox(headerLabel, selectLabel, tilePane);
        pane.getStyleClass().add("login-pane");

        getChildren().add(pane);
    }
}
