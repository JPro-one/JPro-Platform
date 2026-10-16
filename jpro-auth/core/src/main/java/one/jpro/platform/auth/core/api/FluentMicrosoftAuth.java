package one.jpro.platform.auth.core.api;

import javafx.stage.Stage;
import one.jpro.platform.auth.core.oauth2.provider.MicrosoftAuthenticationProvider;

import java.net.http.HttpClient;
import java.util.function.Consumer;

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
     * Set a callback to configure the {@link HttpClient} used for all requests to the provider,
     * e.g. to set an {@code SSLContext}, proxy or connect timeout.
     *
     * @param configurator the callback applied to the {@link HttpClient.Builder}
     * @return self
     */
    FluentMicrosoftAuth configureHttpClient(Consumer<HttpClient.Builder> configurator);

    /**
     * Create a Microsoft authentication provider.
     *
     * @param stage the stage
     * @return a {@link MicrosoftAuthenticationProvider} instance
     */
    MicrosoftAuthenticationProvider create(Stage stage);
}
