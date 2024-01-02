package one.jpro.platform.auth.core.basic;

import one.jpro.platform.auth.core.authentication.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * The {@code BasicAuthenticationProvider} class implements the {@code AuthenticationProvider} interface
 * to provide basic authentication using username and password credentials.
 *
 * @author Besmir Beqiri
 */
public class BasicAuthenticationProvider implements AuthenticationProvider<UsernamePasswordCredentials> {

    private static final Logger logger = LoggerFactory.getLogger(BasicAuthenticationProvider.class);

    public static final String DEFAULT_AUTHORIZATION_PATH = "/auth/basic";

    @NotNull
    private String authorizationPath = DEFAULT_AUTHORIZATION_PATH;
    @Nullable
    private Set<String> roles;
    @Nullable
    private Map<String, Object> attributes;

    /**
     * Constructs a new {@code BasicAuthenticationProvider} with specified roles and attributes.
     *
     * @param roles      the set of roles to be associated with the authenticated user, may be {@code null}.
     * @param attributes the map of attributes to be associated with the authenticated user, may be {@code null}.
     */
    public BasicAuthenticationProvider(@Nullable final Set<String> roles,
                                       @Nullable final Map<String, Object> attributes) {
        this.roles = roles;
        this.attributes = attributes;
    }

    /**
     * Authenticates the user based on the provided {@code UsernamePasswordCredentials}.
     *
     * @param credentials the credentials containing the username and password
     * @return a {@code CompletableFuture} that, when completed, provides the authenticated {@code User}.
     * @throws CredentialValidationException if the credentials are not valid
     */
    @NotNull
    @Override
    public CompletableFuture<User> authenticate(@NotNull final UsernamePasswordCredentials credentials)
            throws CredentialValidationException {
        try {
            credentials.validate(null);
        } catch (CredentialValidationException ex) {
            logger.error("Username and password credentials not valid", ex);
            return CompletableFuture.failedFuture(ex);
        }

        JSONObject userJSON = new JSONObject();
        userJSON.put(User.KEY_NAME, credentials.getUsername());
        userJSON.put(User.KEY_ROLES, new JSONArray(roles));

        JSONObject authJSON = new JSONObject();
        authJSON.put("password", credentials.getPassword());
        userJSON.put(User.KEY_ATTRIBUTES, new JSONObject(attributes).put("auth", authJSON));

        final User user = Authentication.create(userJSON);
        return CompletableFuture.completedFuture(user);
    }

    /**
     * Gets the authorization path URI for basic authentication.
     * This is the URI path that the users will be redirected to if they need to be authenticated.
     *
     * @return the authorization path string
     */
    @NotNull
    public String getAuthorizationPath() {
        return authorizationPath;
    }

    /**
     * Sets the authorization path for basic authentication.
     * This is the URI path that the users will be redirected to if they need to be authenticated.
     *
     * @param authorizationPath the authorization path string
     */
    public void setAuthorizationPath(@NotNull String authorizationPath) {
        this.authorizationPath = authorizationPath;
    }

    /**
     * Gets the set of roles associated with this authentication provider.
     *
     * @return The set of roles, may be {@code null}.
     */
    @Nullable
    public Set<String> getRoles() {
        return roles;
    }

    /**
     * Sets the roles to be associated with this authentication provider.
     *
     * @param roles The set of roles, may be {@code null}.
     */
    public void setRoles(@Nullable Set<String> roles) {
        this.roles = roles;
    }

    /**
     * Gets the attributes associated with this authentication provider.
     *
     * @return The attributes, may be {@code null}.
     */
    @Nullable
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * Sets the attributes to be associated with this authentication provider.
     *
     * @param attributes The map of attributes, may be {@code null}.
     */
    public void setAttributes(@Nullable Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}
