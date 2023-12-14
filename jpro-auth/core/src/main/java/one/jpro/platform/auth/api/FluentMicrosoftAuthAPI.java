package one.jpro.platform.auth.api;

import javafx.stage.Stage;
import one.jpro.platform.auth.oauth2.provider.MicrosoftAuthenticationProvider;

/**
 * Fluent Microsoft Authentication API.
 *
 * @author Besmir Beqiri
 */
public class FluentMicrosoftAuthAPI implements FluentMicrosoftAuth {

    private String clientId;
    private String clientSecret;
    private String tenant;

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
    public MicrosoftAuthenticationProvider create(Stage stage) {
        return new MicrosoftAuthenticationProvider(stage, clientId, clientSecret, tenant);
    }
}
