package one.jpro.platform.auth.core.api;

import javafx.stage.Stage;
import one.jpro.platform.auth.core.oauth2.OAuth2Flow;
import one.jpro.platform.auth.core.oauth2.provider.KeycloakAuthenticationProvider;
import org.json.JSONObject;

/**
 * Fluent Keycloak Authentication API.
 *
 * @author Besmir Beqiri
 */
public class FluentKeycloakAuthAPI implements FluentKeycloakAuth {

    private OAuth2Flow flow = OAuth2Flow.AUTH_CODE;
    private String site;
    private String clientId;
    private String clientSecret;
    private String realm;
    private String redirectUri;

    @Override
    public FluentKeycloakAuth flow(OAuth2Flow flow) {
        this.flow = flow;
        return this;
    }

    @Override
    public FluentKeycloakAuth site(String site) {
        this.site = site;
        return this;
    }

    @Override
    public FluentKeycloakAuth clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public FluentKeycloakAuth clientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    @Override
    public FluentKeycloakAuth realm(String realm) {
        this.realm = realm;
        return this;
    }

    @Override
    public FluentKeycloakAuth redirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

    @Override
    public KeycloakAuthenticationProvider create(Stage stage) {
        final JSONObject config = new JSONObject();
        if (site != null) config.put("auth-server-url", site);
        if (clientId != null) config.put("resource", clientId);
        if (clientSecret != null) config.put("credentials", new JSONObject().put("secret", clientSecret));
        if (realm != null) config.put("realm", realm);

        final var keycloakAuthProvider = new KeycloakAuthenticationProvider(stage, flow, config);
        keycloakAuthProvider.getCredentials().setRedirectUri(redirectUri);
        return keycloakAuthProvider;
    }
}
