package one.jpro.platform.auth.example.basic.page;

import one.jpro.platform.auth.core.basic.LoginPane;
import one.jpro.platform.auth.core.basic.UsernamePasswordCredentials;
import one.jpro.platform.auth.core.basic.provider.BasicAuthenticationProvider;
import one.jpro.platform.auth.routing.AuthBasicFilter;
import one.jpro.platform.auth.routing.AuthUIProvider;

/**
 * Basic login page with username and password.
 *
 * @author Besmir Beqiri
 */
public class LoginPage extends Page {

    public LoginPage(BasicAuthenticationProvider authProvider,
                     AuthUIProvider authUIProvider,
                     UsernamePasswordCredentials credentials) {
        getChildren().add(authUIProvider.createAuthenticationNode());
    }
}
