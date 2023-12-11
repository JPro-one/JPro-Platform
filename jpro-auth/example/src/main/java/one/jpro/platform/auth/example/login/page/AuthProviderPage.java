package one.jpro.platform.auth.example.login.page;

import com.jpro.webapi.WebAPI;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import one.jpro.platform.auth.example.login.LoginApp;
import one.jpro.platform.auth.oauth2.OAuth2AuthenticationProvider;
import one.jpro.platform.auth.oauth2.OAuth2Credentials;
import simplefx.experimental.parts.FXFuture;

import java.util.Optional;

import static one.jpro.platform.auth.example.login.BaseLoginApp.AUTH_ERROR_PATH;
import static one.jpro.platform.routing.LinkUtil.gotoPage;

/**
 * Authorization provider page.
 *
 * @author Besmir Beqiri
 */
public class AuthProviderPage extends Page {

    public AuthProviderPage(LoginApp loginApp,
                            OAuth2AuthenticationProvider authProvider,
                            OAuth2Credentials authCredentials) {
        final var headerLabel = new Label("Authentication Provider: " + loginApp.getAuthProviderName(authProvider));
        headerLabel.getStyleClass().add("header-label");

        final var pane = new VBox(headerLabel);
        pane.getStyleClass().add("auth-provider-pane");

        final var authOptions = authProvider.getOptions();

        final var siteLabel = new Label("Site:");
        final var siteField = new TextField(authOptions.getSite());
        siteField.setEditable(false);
        pane.getChildren().addAll(siteLabel, siteField);

        Optional.ofNullable(authOptions.getTenant()).ifPresent(tenant -> {
            final var tenantLabel = new Label("Tenant:");
            final var tenantField = new TextField(tenant);
            tenantField.setEditable(false);
            pane.getChildren().addAll(tenantLabel, tenantField);
        });

        final var clientIdLabel = new Label("Client ID:");
        final var clientIdField = new TextField(authOptions.getClientId());
        clientIdField.setEditable(false);
        pane.getChildren().addAll(clientIdLabel, clientIdField);

        Optional.ofNullable(authOptions.getClientSecret()).ifPresent(clientSecret -> {
            final var clientSecretLabel = new Label("Client Secret:");
            final var clientSecretField = new TextField(clientSecret);
            clientSecretField.setEditable(false);
            pane.getChildren().addAll(clientSecretLabel, clientSecretField);
        });

        final var scopesLabel = new Label("Scopes:");
        final var scopesField = new TextField(String.join(", ", authCredentials.getScopes()));
        scopesField.setEditable(false);
        pane.getChildren().addAll(scopesLabel, scopesField);

        final var redirectUriLabel = new Label("Redirect URI:");
        final var redirectUriField = new TextField(authCredentials.getRedirectUri());
        redirectUriField.setEditable(false);
        pane.getChildren().addAll(redirectUriLabel, redirectUriField);

        Optional.ofNullable(authCredentials.getNonce()).ifPresent(nonce -> {
            final var nonceLabel = new Label("Nonce:");
            final var nonceField = new TextField(nonce);
            nonceField.setEditable(false);
            pane.getChildren().addAll(nonceLabel, nonceField);
        });

        final var signInBox = loginApp.createButtonWithDescription(
                "Sign in with the selected authentication provider.", "Sign In",
                event -> authProvider.authorizeUrl(authCredentials)
                        .thenAccept(url -> {
                            // gotoURL call is only needed when running as a desktop app
                            if (!WebAPI.isBrowser()) {
                                loginApp.getSessionManager().gotoURL(url);
                            }
                        }));

        final var discoveryBox = loginApp.createButtonWithDescription(
                "The OpenID Connect Discovery provides a client with configuration details.",
                "Discovery", event -> FXFuture.fromJava(authProvider.discover())
                        .map(provider -> {
                            final var options = provider.getOptions();
                            loginApp.setAuthOptions(options);
                            gotoPage(headerLabel, "/provider/discovery/"
                                    + loginApp.getAuthProviderName(authProvider).toLowerCase());
                            return provider;
                        })
                        .exceptionally(throwable -> {
                            loginApp.setError(throwable);
                            gotoPage(headerLabel, AUTH_ERROR_PATH);
                            return null;
                        }));
        pane.getChildren().addAll(signInBox, discoveryBox);

        getChildren().add(pane);
    }
}
