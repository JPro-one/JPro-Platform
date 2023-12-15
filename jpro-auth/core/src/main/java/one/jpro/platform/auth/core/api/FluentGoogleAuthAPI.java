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
    private String redirectUri;

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
    public FluentGoogleAuth redirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

    @Override
    public GoogleAuthenticationProvider create(Stage stage) {
        final var googleAuthProvider = new GoogleAuthenticationProvider(stage, clientId, clientSecret);
        googleAuthProvider.getCredentials().setRedirectUri(redirectUri);
        return googleAuthProvider;
    }
}
