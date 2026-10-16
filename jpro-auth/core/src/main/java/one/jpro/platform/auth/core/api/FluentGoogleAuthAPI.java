package one.jpro.platform.auth.core.api;

import javafx.stage.Stage;
import one.jpro.platform.auth.core.oauth2.provider.GoogleAuthenticationProvider;

import java.net.http.HttpClient;
import java.util.function.Consumer;

/**
 * Fluent Google Authentication API.
 *
 * @author Besmir Beqiri
 */
public class FluentGoogleAuthAPI implements FluentGoogleAuth {

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private Consumer<HttpClient.Builder> httpClientConfigurator;

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
    public FluentGoogleAuth configureHttpClient(Consumer<HttpClient.Builder> configurator) {
        this.httpClientConfigurator = configurator;
        return this;
    }

    @Override
    public GoogleAuthenticationProvider create(Stage stage) {
        final var googleAuthProvider = new GoogleAuthenticationProvider(stage, clientId, clientSecret);
        googleAuthProvider.getOptions().setHttpClientConfigurator(httpClientConfigurator);
        googleAuthProvider.getCredentials().setRedirectUri(redirectUri);
        return googleAuthProvider;
    }
}
