package one.jpro.platform.auth.core.api;

import javafx.stage.Stage;
import one.jpro.platform.auth.core.oauth2.provider.MicrosoftAuthenticationProvider;

/**
 * Fluent Microsoft Authentication interface.
 *
 * @author Besmir Beqiri
 */
public interface FluentMicrosoftAuth {

    /**
     * Set the client id.
     *
     * @param clientId the client id
     * @return self
     */
    FluentMicrosoftAuth clientId(String clientId);

    /**
     * Set the client secret.
     *
     * @param clientSecret the client secret
     * @return self
     */
    FluentMicrosoftAuth clientSecret(String clientSecret);

    /**
     * Set the tenant.
     *
     * @param tenant the tenant
     * @return self
     */
    FluentMicrosoftAuth tenant(String tenant);

    /**
     * Set the redirect uri.
     *
     * @param redirectUri the redirect uri
     * @return self
     */
    FluentMicrosoftAuth redirectUri(String redirectUri);

    /**
     * Create a Microsoft authentication provider.
     *
     * @param stage the stage
     * @return a {@link MicrosoftAuthenticationProvider} instance
     */
    MicrosoftAuthenticationProvider create(Stage stage);
}
