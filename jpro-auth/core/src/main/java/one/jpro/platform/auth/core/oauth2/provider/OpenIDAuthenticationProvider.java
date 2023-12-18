package one.jpro.platform.auth.core.oauth2.provider;

import javafx.stage.Stage;
import one.jpro.platform.auth.core.authentication.User;
import one.jpro.platform.auth.core.oauth2.OAuth2AuthenticationProvider;
import one.jpro.platform.auth.core.oauth2.OAuth2Credentials;
import one.jpro.platform.auth.core.oauth2.OAuth2Options;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * OpenID's authentication provider.
 *
 * @author Besmir Beqiri
 */
public class OpenIDAuthenticationProvider extends OAuth2AuthenticationProvider {

    private final OAuth2Credentials credentials = new OAuth2Credentials();

    /**
     * Creates a OAuth2 authentication provider.
     *
     * @param stage   the JavaFX application stage
     * @param options the OAuth2 options
     */
    public OpenIDAuthenticationProvider(@Nullable Stage stage, @NotNull OAuth2Options options) {
        super(stage, options);

        // Configure credentials scopes
        if (options.getSupportedScopes() != null && !options.getSupportedScopes().isEmpty()) {
            credentials.setScopes(options.getSupportedScopes());
        } else {
            credentials.setScopes(List.of("openid"));
        }
    }

    @NotNull
    public OAuth2Credentials getCredentials() {
        return credentials;
    }

    /**
     * The client sends the end-user's browser to the authorization endpoint.
     * This endpoint is where the user signs in and grants access.
     * End-user interaction is required.
     *
     * @return a {@link CompletableFuture} that will complete with the authorization URL
     * once the HTTP server is ready to handle the callback, or with an exception
     * if an error occurs during the process.
     */
    @NotNull
    public CompletableFuture<String> authorizeUrl() {
        return super.authorizeUrl(credentials);
    }

    /**
     * Authenticate a user with the given credentials.
     *
     * @return a future that will complete with the authenticated user
     */
    @NotNull
    public CompletableFuture<User> authenticate() {
        return super.authenticate(credentials);
    }
}
