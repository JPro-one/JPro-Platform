package one.jpro.platform.auth.core.api;

import one.jpro.platform.auth.core.basic.provider.BasicAuthenticationProvider;

import java.util.Map;
import java.util.Set;

/**
 * Fluent Basic (username and password) Authentication API.
 *
 * @author Besmir Beqiri
 */
public class FluentBasicAuthAPI implements FluentBasicAuth {

    private Set<String> roles;
    private Map<String, Object> attributes;
    private String authorizationPath = BasicAuthenticationProvider.DEFAULT_AUTHORIZATION_PATH;

    @Override
    public FluentBasicAuth roles(Set<String> roles) {
        this.roles = roles;
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
        final var basicAuthProvider = new BasicAuthenticationProvider(roles, attributes);
        basicAuthProvider.setAuthorizationPath(authorizationPath);
        return basicAuthProvider;
    }
}
