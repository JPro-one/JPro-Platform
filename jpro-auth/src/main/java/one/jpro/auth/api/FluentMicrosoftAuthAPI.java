package one.jpro.auth.api;

import javafx.stage.Stage;
import one.jpro.auth.http.HttpServer;
import one.jpro.auth.oath2.provider.MicrosoftAuthenticationProvider;

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
        return new MicrosoftAuthenticationProvider(HttpServer.create(stage), clientId, clientSecret, tenant);
    }
}
