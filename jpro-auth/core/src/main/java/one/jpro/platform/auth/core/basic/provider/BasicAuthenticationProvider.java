package one.jpro.platform.auth.core.basic.provider;

import one.jpro.platform.auth.core.authentication.AuthenticationException;
import one.jpro.platform.auth.core.authentication.AuthenticationProvider;
import one.jpro.platform.auth.core.authentication.CredentialValidationException;
import one.jpro.platform.auth.core.authentication.User;
import one.jpro.platform.auth.core.basic.UserManager;
import one.jpro.platform.auth.core.basic.UserNotFoundException;
import one.jpro.platform.auth.core.basic.UsernamePasswordCredentials;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static one.jpro.platform.auth.core.utils.AuthUtils.BCRYPT_PASSWORD_ENCODER;

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
    private UserManager userManager;
    @NotNull
    private String authorizationPath = DEFAULT_AUTHORIZATION_PATH;
    @Nullable
    private Set<String> roles;
    @Nullable
    private Map<String, Object> attributes;

    /**
     * Constructs a new {@code BasicAuthenticationProvider} with specified roles and attributes.
     *
     * @param userManager the user manager to be used for authentication
     * @param roles       the set of roles to be associated with the authenticated user, may be {@code null}
     * @param attributes  the map of attributes to be associated with the authenticated user, may be {@code null}
     */
    public BasicAuthenticationProvider(@NotNull final UserManager userManager,
                                       @Nullable final Set<String> roles,
                                       @Nullable final Map<String, Object> attributes) {
        this.userManager = userManager;
        this.roles = roles;
        this.attributes = attributes;
    }

    /**
     * Authenticates the user based on the provided {@code UsernamePasswordCredentials}.
     *
     * @param credentials the credentials containing the username and password
     * @return a {@code CompletableFuture} that, when completed, provides the authenticated {@code User}
     * @throws CredentialValidationException if the credentials are not valid
     */
    @Override
    public CompletableFuture<User> authenticate(@NotNull final UsernamePasswordCredentials credentials)
            throws AuthenticationException, CredentialValidationException {
        try {
            credentials.validate(null);
        } catch (CredentialValidationException ex) {
            logger.error("Username and password credentials not valid", ex);
            return CompletableFuture.failedFuture(ex);
        }

        return getUserManager().loadUserByUsername(credentials.getUsername())
                .thenCompose(user -> {
                    final JSONObject attributesJSON = user.toJSON().getJSONObject(User.KEY_ATTRIBUTES);
                    if (attributesJSON.has("credentials")) {
                        final JSONObject credentialsJSON = attributesJSON.getJSONObject("credentials");
                        final String username = credentialsJSON.getString("username");
                        final String encodedPassword = credentialsJSON.getString("password");

                        if (username.equals(credentials.getUsername())
                                && BCRYPT_PASSWORD_ENCODER.matches(credentials.getPassword(), encodedPassword)) {
                            final JSONObject authJSON = new JSONObject();
                            authJSON.put("type", "basic");
                            authJSON.put("username", username);
                            authJSON.put("password", encodedPassword);

                            final JSONObject userJSON = user.toJSON();
                            userJSON.put(User.KEY_ROLES, roles);
                            userJSON.put(User.KEY_ATTRIBUTES, new JSONObject(attributes).put("auth", authJSON));
                            return CompletableFuture.completedFuture(new User(userJSON));
                        } else {
                            return CompletableFuture.failedFuture(
                                    new AuthenticationException("Invalid username or password"));
                        }
                    } else {
                        return CompletableFuture.failedFuture(
                                new AuthenticationException("User has no credentials"));
                    }
                })
                .exceptionallyCompose(throwable -> {
                    final Throwable rootCause = throwable.getCause();
                    if (rootCause instanceof UserNotFoundException) {
                        return CompletableFuture.failedFuture(
                                new AuthenticationException("Invalid username", rootCause));
                    } else {
                        return CompletableFuture.failedFuture(rootCause);
                    }
                });
    }

    /**
     * Gets the user manager associated with this authentication provider.
     *
     * @return the user manager
     */
    @NotNull
    public UserManager getUserManager() {
        return userManager;
    }

    /**
     * Sets the user manager to be associated with this authentication provider.
     *
     * @param userManager the user manager
     */
    public void setUserManager(@NotNull final UserManager userManager) {
        this.userManager = userManager;
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
     * @return the set of roles, may be {@code null}
     */
    @Nullable
    public Set<String> getRoles() {
        return roles;
    }

    /**
     * Sets the roles to be associated with this authentication provider.
     *
     * @param roles the set of roles, may be {@code null}
     */
    public void setRoles(@Nullable Set<String> roles) {
        this.roles = roles;
    }

    /**
     * Gets the attributes associated with this authentication provider.
     *
     * @return the attributes, may be {@code null}
     */
    @Nullable
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * Sets the attributes to be associated with this authentication provider.
     *
     * @param attributes the map of attributes, may be {@code null}
     */
    public void setAttributes(@Nullable Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}
