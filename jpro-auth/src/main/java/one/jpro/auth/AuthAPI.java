package one.jpro.auth;

import one.jpro.auth.api.*;

/**
 * Access and configure supported authentication providers via simplified and fluent style API.
 *
 * @author Besmir Beqiri
 */
public interface AuthAPI {

    /**
     * Configure and create a Google authentication provider.
     *
     * @return fluent style api.
     */
    static FluentGoogleAuth googleAuth() {
        return new FluentGoogleAuthAPI();
    }

    /**
     * Configure and create a Keycloak authentication provider.
     *
     * @return fluent style api.
     */
    static FluentKeycloakAuth keycloakAuth() {
        return new FluentKeycloakAuthAPI();
    }

    /**
     * Configure and create a Microsoft authentication provider.
     *
     * @return fluent style api.
     */
    static FluentMicrosoftAuth microsoftAuth() {
        return new FluentMicrosoftAuthAPI();
    }
}
