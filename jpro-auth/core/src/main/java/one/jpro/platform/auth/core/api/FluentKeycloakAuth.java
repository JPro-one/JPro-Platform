package one.jpro.platform.auth.core.api;

import javafx.stage.Stage;
import one.jpro.platform.auth.core.oauth2.OAuth2Flow;
import one.jpro.platform.auth.core.oauth2.provider.KeycloakAuthenticationProvider;

/**
 * Fluent Keycloak Authentication interface.
 *
 * @author Besmir Beqiri
 */
public interface FluentKeycloakAuth {

    /**
     * Set the OAuth2 flow.
     *
     * @param flow the flow
     * @return self
     */
    FluentKeycloakAuth flow(OAuth2Flow flow);

    /**
     * Set the site.
     *
     * @param site the site
     * @return self
     */
    FluentKeycloakAuth site(String site);

    /**
     * Set the client id.
     *
     * @param clientId the client id
     * @return self
     */
    FluentKeycloakAuth clientId(String clientId);

    /**
     * Set the client secret.
     *
     * @param clientSecret the client secret
     * @return self
     */
    FluentKeycloakAuth clientSecret(String clientSecret);

    /**
     * Set the realm.
     *
     * @param realm the realm
     * @return self
     */
    FluentKeycloakAuth realm(String realm);

    /**
     * Set the redirect uri.
     *
     * @param redirectUri the redirect uri
     * @return self
     */
    FluentKeycloakAuth redirectUri(String redirectUri);

    /**
     * Create a Keycloak authentication provider.
     *
     * @param stage the stage
     * @return a {@link KeycloakAuthenticationProvider} instance
     */
    KeycloakAuthenticationProvider create(Stage stage);
}
