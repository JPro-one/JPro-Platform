package one.jpro.auth.api;

import javafx.stage.Stage;
import one.jpro.auth.oath2.provider.MicrosoftAuthenticationProvider;

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
     * Create a Microsoft authentication provider configured with the provided options.
     *
     * @param stage the stage
     * @return a {@link MicrosoftAuthenticationProvider} instance.
     */
    MicrosoftAuthenticationProvider create(Stage stage);
}
