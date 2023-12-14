package one.jpro.platform.auth.core.api;

import javafx.stage.Stage;
import one.jpro.platform.auth.core.oauth2.provider.GoogleAuthenticationProvider;

/**
 * Fluent Google Authentication interface.
 *
 * @author Besmir Beqiri
 */
public interface FluentGoogleAuth {

    /**
     * Set the client id.
     *
     * @param clientId the client id
     * @return self
     */
    FluentGoogleAuth clientId(String clientId);

    /**
     * Set the client secret.
     *
     * @param clientSecret the client secret
     * @return self
     */
    FluentGoogleAuth clientSecret(String clientSecret);

    /**
     * Create a Google authentication provider configured with the provided options.
     *
     * @param stage the stage
     * @return a {@link GoogleAuthenticationProvider} instance.
     */
    GoogleAuthenticationProvider create(Stage stage);
}
