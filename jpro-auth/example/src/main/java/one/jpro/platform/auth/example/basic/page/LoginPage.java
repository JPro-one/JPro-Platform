package one.jpro.platform.auth.example.basic.page;

import one.jpro.platform.auth.core.basic.LoginPane;
import one.jpro.platform.auth.core.basic.UsernamePasswordCredentials;
import one.jpro.platform.auth.core.basic.provider.BasicAuthenticationProvider;
import one.jpro.platform.auth.routing.AuthFilter;

/**
 * Basic login page with username and password.
 *
 * @author Besmir Beqiri
 */
public class LoginPage extends Page {

    public LoginPage(BasicAuthenticationProvider authProvider,
                     UsernamePasswordCredentials credentials) {
        final var loginPane = new LoginPane(credentials);
        loginPane.getStyleClass().add("basic");
        loginPane.getSubmitButton().setOnAction(event -> AuthFilter.authorize(loginPane, authProvider));

        getChildren().add(loginPane);
    }
}
