package one.jpro.platform.auth.core.basic;

import one.jpro.platform.auth.core.authentication.CredentialValidationException;
import one.jpro.platform.auth.core.authentication.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Non-persistent implementation of {@code UserDetailsManager} which is backed
 * by an in-memory map.
 * <p>
 * Mainly intended for testing and demonstration purposes, where a persistent
 * system isn't required.
 *
 * @author Besmir Beqiri
 */
public class InMemoryUserManager implements UserManager {

    private final Map<String, User> users;

    public InMemoryUserManager() {
        users = new HashMap<>();
    }

    public InMemoryUserManager(@NotNull final Collection<User> users) {
        this();
        for (User user : users) {
            this.users.put(user.getName(), user);
        }
    }

    public InMemoryUserManager(User... users) {
        this();
        for (User user : users) {
            this.users.put(user.getName(), user);
        }
    }

    @Override
    public CompletableFuture<User> createUser(@NotNull UsernamePasswordCredentials credentials,
                                              @Nullable Set<String> roles,
                                              @Nullable Map<String, Object> attributes)
            throws CredentialValidationException {
        // validate credentials
        Objects.requireNonNull(credentials, "Credentials cannot be null");
        credentials.validate(null);

        // check if user exists
        if (userExists(credentials.getUsername())) {
            throw new IllegalArgumentException("User already exists: " + credentials.getUsername());
        }

        return CompletableFuture.supplyAsync(() -> {
            final JSONObject userJSON = new JSONObject();
            userJSON.put(User.KEY_NAME, credentials.getUsername());
            userJSON.put(User.KEY_ROLES, roles);
            final JSONObject credentialsJSON = credentials.toJSON();
            userJSON.put(User.KEY_ATTRIBUTES, new JSONObject(attributes).put("credentials", credentialsJSON));
            return new User(userJSON);
        }).thenApply(user -> {
            users.put(user.getName(), user);
            return user;
        });
    }

    @Override
    public CompletableFuture<User> updateUser(@NotNull String username,
                                              @Nullable Set<String> roles,
                                              @Nullable Map<String, Object> attributes)
            throws UserNotFoundException, CredentialValidationException {
        // check if user exists
        if (!userExists(username)) {
            throw new UserNotFoundException("User does not exist: " + username);
        }

        return CompletableFuture.supplyAsync(() -> {
            final User user = users.get(username);
            final JSONObject userJSON = user.toJSON();
            final JSONObject attributesJSON = new JSONObject(attributes);
            if (user.hasAttribute("credentials")) {
                final JSONObject credentialsJSON = userJSON.getJSONObject(User.KEY_ATTRIBUTES)
                        .getJSONObject("credentials");
                attributesJSON.put("credentials", credentialsJSON);
            }
            // recreate user with updated roles and attributes
            return new User(username, roles, attributesJSON.toMap());
        }).thenApply(user -> {
            users.put(user.getName(), user);
            return user;
        });
    }

    @Override
    public CompletableFuture<User> deleteUser(@Nullable String username) {
        return CompletableFuture.completedFuture(users.remove(username));
    }

    @Override
    public CompletableFuture<User> changePassword(@NotNull String username,
                                                  @NotNull String newPassword)
            throws UserNotFoundException, CredentialValidationException {
        // check if user exists
        if (!userExists(username)) {
            throw new UserNotFoundException("User does not exist: " + username);
        }
        // validate credentials (this will also validate the new password)
        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, newPassword);
        credentials.validate(null);

        return CompletableFuture.supplyAsync(() -> {
            final User user = users.get(credentials.getUsername());
            final JSONObject userJSON = user.toJSON();
            final JSONObject credentialsJSON = credentials.toJSON();
            final JSONObject attributesJSON = userJSON.getJSONObject(User.KEY_ATTRIBUTES);
            // update credentials
            attributesJSON.put("credentials", credentialsJSON);
            // if user has auth attributes, remove them to force re-authentication
            if (attributesJSON.has("auth")) {
                attributesJSON.remove("auth");
            }
            return new User(userJSON);
        }).thenApply(user -> {
            users.put(user.getName(), user);
            return user;
        });
    }

    @Override
    public boolean userExists(@Nullable String username) {
        return users.containsKey(username);
    }

    @Override
    public CompletableFuture<User> loadUserByUsername(@NotNull String username) throws UserNotFoundException {
        final User user = users.get(username);
        if (user == null) {
            return CompletableFuture.failedFuture(new UserNotFoundException("User does not exist: " + username));
        }
        return CompletableFuture.completedFuture(users.get(username));
    }
}
