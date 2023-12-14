package one.jpro.platform.auth.core.api;

import javafx.stage.Stage;
import one.jpro.platform.auth.core.oauth2.provider.GoogleAuthenticationProvider;

/**
 * Fluent Google Authentication API.
 *
 * @author Besmir Beqiri
 */
public class FluentGoogleAuthAPI implements FluentGoogleAuth {

    private String clientId;
    private String clientSecret;

    @Override
    public FluentGoogleAuth clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    @Override
    public FluentGoogleAuth clientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    @Override
    public GoogleAuthenticationProvider create(Stage stage) {
        return new GoogleAuthenticationProvider(stage, clientId, clientSecret);
    }
}
