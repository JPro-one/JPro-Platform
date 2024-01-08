package one.jpro.platform.auth.core.api;

import one.jpro.platform.auth.core.basic.UserManager;
import one.jpro.platform.auth.core.basic.provider.BasicAuthenticationProvider;

import java.util.Map;

/**
 * Fluent Basic (username and password) Authentication interface.
 *
 * @author Besmir Beqiri
 */
public interface FluentBasicAuth {

    /**
     * Set the user manager.
     *
     * @param userManager the user manager
     * @return self
     */
    FluentBasicAuth userManager(UserManager userManager);

    /**
     * Set the roles.
     *
     * @param roles the roles
     * @return self
     */
    FluentBasicAuth roles(String... roles);

    /**
     * Set the attributes.
     *
     * @param attributes the attributes
     * @return self
     */
    FluentBasicAuth attributes(Map<String, Object> attributes);

    /**
     * Set the authorization path.
     *
     * @param authorizationPath the authorization path
     * @return self
     */
    FluentBasicAuth authorizationPath(String authorizationPath);

    /**
     * Create a simple authentication provider.
     *
     * @return a {@link BasicAuthenticationProvider} instance
     */
    BasicAuthenticationProvider create();
}
