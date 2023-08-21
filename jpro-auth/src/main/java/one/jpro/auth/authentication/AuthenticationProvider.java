package one.jpro.auth.authentication;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Defines an authentication provider.
 *
 * @author Besmir Beqiri
 */
@FunctionalInterface
public interface AuthenticationProvider<T extends Credentials> {

    /**
     * Authenticate a user with the given credentials.
     *
     * @param credentials a {@link Credentials} object containing the
     *                    information for authenticating the user.
     * @return the result future
     */
    @NotNull CompletableFuture<User> authenticate(@NotNull T credentials);
}
