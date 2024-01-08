package one.jpro.platform.auth.core.basic;

import one.jpro.platform.auth.core.authentication.CredentialValidationException;
import one.jpro.platform.auth.core.authentication.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Provide user management capabilities. It includes methods for creating,
 * updating, deleting users, changing passwords, and checking user existence.
 *
 * @author Besmir Beqiri
 */
public interface UserManager {

    /**
     * Creates a new user with the specified credentials.
     * The method allows specifying roles and additional attributes for the user.
     *
     * @param credentials the credentials for the new user, typically containing a username and password
     * @param roles       an optional set of roles to assign to the user
     * @param attributes  an optional map of additional attributes to associate with the user
     * @return a {@code CompletableFuture} that, upon completion, returns the created {@code User} object.
     * @throws CredentialValidationException if the provided credentials do not meet the required validation criteria.
     */

    CompletableFuture<User> createUser(@NotNull UsernamePasswordCredentials credentials,
                                       @Nullable Set<String> roles,
                                       @Nullable Map<String, Object> attributes) throws CredentialValidationException;

    /**
     * Updates an existing user identified by the username.
     * This method allows updating the user's roles and additional attributes.
     *
     * @param username   the username of the user to be updated
     * @param roles      an optional new set of roles for the user
     * @param attributes an optional map of new or updated attributes for the user
     * @return a {@code CompletableFuture} that, when completed, returns the updated {@code User} object.
     * @throws UserNotFoundException         if no user exists with the given username
     * @throws CredentialValidationException if any provided credentials are invalid
     */
    CompletableFuture<User> updateUser(@NotNull String username,
                                       @Nullable Set<String> roles,
                                       @Nullable Map<String, Object> attributes) throws UserNotFoundException;

    /**
     * Deletes a user with the specified username.
     *
     * @param username the username of the user to be deleted.
     * @return a {@code CompletableFuture} that, when completed, returns the deleted {@code User}
     */
    CompletableFuture<User> deleteUser(@Nullable String username);

    /**
     * Changes the password of an existing user.
     *
     * @param username    the username of the user whose password is to be changed
     * @param newPassword the new password for the user
     * @return a {@code CompletableFuture} that, when completed, returns the user with the updated password
     * @throws UserNotFoundException         if no user exists with the given username
     * @throws CredentialValidationException if the new password does not meet the required validation criteria
     */
    CompletableFuture<User> changePassword(String username, String newPassword)
            throws UserNotFoundException, CredentialValidationException;

    /**
     * Checks if a user exists with the specified username.
     *
     * @param username The username to check for existence.
     * @return {@code true} if the user exists, {@code false} otherwise.
     */
    boolean userExists(String username);

    /**
     * Locates the user based on the username.
     *
     * @param username the username identifying the user whose data is required
     * @return a fully populated user record (never <code>null</code>)
     * @throws UserNotFoundException if the user could not be found
     */
    CompletableFuture<User> loadUserByUsername(String username) throws UserNotFoundException;
}
