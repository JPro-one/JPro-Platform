package one.jpro.platform.auth.core.api;

import one.jpro.platform.auth.core.basic.UserManager;
import one.jpro.platform.auth.core.basic.provider.BasicAuthenticationProvider;

import java.util.Map;
import java.util.Set;

/**
 * Fluent Basic (username and password) Authentication API.
 *
 * @author Besmir Beqiri
 */
public class FluentBasicAuthAPI implements FluentBasicAuth {

    private UserManager userManager;
    private Set<String> roles;
    private Map<String, Object> attributes;
    private String authorizationPath = BasicAuthenticationProvider.DEFAULT_AUTHORIZATION_PATH;

    @Override
    public FluentBasicAuth userManager(UserManager userManager) {
        this.userManager = userManager;
        return this;
    }

    @Override
    public FluentBasicAuth roles(String... roles) {
        this.roles = Set.of(roles);
        return this;
    }

    @Override
    public FluentBasicAuth attributes(Map<String, Object> attributes) {
        this.attributes = attributes;
        return this;
    }

    @Override
    public FluentBasicAuth authorizationPath(String authorizationPath) {
        this.authorizationPath = authorizationPath;
        return this;
    }

    @Override
    public BasicAuthenticationProvider create() {
        final var basicAuthProvider = new BasicAuthenticationProvider(userManager, roles, attributes);
        basicAuthProvider.setAuthorizationPath(authorizationPath);
        return basicAuthProvider;
    }
}
