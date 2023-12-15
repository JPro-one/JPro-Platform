package one.jpro.platform.auth.example.oauth.page;

import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import one.jpro.platform.auth.example.oauth.OAuthApp;
import one.jpro.platform.auth.core.oauth2.OAuth2AuthenticationProvider;
import one.jpro.platform.mdfx.MarkdownView;

/**
 * Authorization provider discovery page.
 *
 * @author Besmir Beqiri
 */
public class AuthProviderDiscoveryPage extends Page {

    public AuthProviderDiscoveryPage(OAuthApp loginApp,
                                     OAuth2AuthenticationProvider authProvider) {
        final var headerLabel = new Label("OpenID Connect Discovery: " + loginApp.getAuthProviderName(authProvider));
        headerLabel.getStyleClass().add("header-label");

        MarkdownView providerDiscoveryView = new MarkdownView();
        providerDiscoveryView.mdStringProperty().bind(Bindings.createStringBinding(() -> {
            final var authOptions = loginApp.getAuthOptions();
            return authOptions == null ? "" : loginApp.jsonToMarkdown(authOptions.toJSON());
        }, loginApp.authOptionsProperty()));

        final var pane = new VBox(headerLabel, providerDiscoveryView);
        pane.getStyleClass().add("openid-provider-discovery-pane");

        getChildren().add(pane);
    }
}
