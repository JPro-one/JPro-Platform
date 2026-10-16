package one.jpro.platform.auth.core.api;

import javafx.stage.Stage;
import one.jpro.platform.auth.core.oauth2.provider.GoogleAuthenticationProvider;

import java.net.http.HttpClient;
import java.util.function.Consumer;

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
     * Set the redirect uri.
     *
     * @param redirectUri the redirect uri
     * @return self
     */
    FluentGoogleAuth redirectUri(String redirectUri);

    /**
     * Set a callback to configure the {@link HttpClient} used for all requests to the provider,
     * e.g. to set an {@code SSLContext}, proxy or connect timeout.
     *
     * @param configurator the callback applied to the {@link HttpClient.Builder}
     * @return self
     */
    FluentGoogleAuth configureHttpClient(Consumer<HttpClient.Builder> configurator);

    /**
     * Create a Google authentication provider.
     *
     * @param stage the stage
     * @return a {@link GoogleAuthenticationProvider} instance
     */
    GoogleAuthenticationProvider create(Stage stage);
}
