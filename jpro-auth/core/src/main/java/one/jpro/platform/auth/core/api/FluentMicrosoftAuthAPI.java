package one.jpro.platform.auth.core.api;

import javafx.stage.Stage;
import one.jpro.platform.auth.core.oauth2.provider.MicrosoftAuthenticationProvider;

/**
 * Fluent Microsoft Authentication API.
 *
 * @author Besmir Beqiri
 */
public class FluentMicrosoftAuthAPI implements FluentMicrosoftAuth {

    private String clientId;
    private String clientSecret;
    private String tenant;
    private String redirectUri;

    @Override
    public FluentMicrosoftAuth clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    @Override
    public FluentMicrosoftAuth clientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    @Override
    public FluentMicrosoftAuth tenant(String tenant) {
        this.tenant = tenant;
        return this;
    }

    @Override
    public FluentMicrosoftAuth redirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

    @Override
    public MicrosoftAuthenticationProvider create(Stage stage) {
        final var microsoftAuthProvider = new MicrosoftAuthenticationProvider(stage, clientId, clientSecret, tenant);
        microsoftAuthProvider.getCredentials().setRedirectUri(redirectUri);
        return microsoftAuthProvider;
    }
}
