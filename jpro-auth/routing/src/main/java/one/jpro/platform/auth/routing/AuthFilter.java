package one.jpro.platform.auth.routing;

import one.jpro.platform.auth.core.authentication.User;
import one.jpro.platform.auth.core.basic.provider.BasicAuthenticationProvider;
import one.jpro.platform.auth.core.basic.UsernamePasswordCredentials;
import one.jpro.platform.routing.Filter;
import one.jpro.platform.routing.Response;
import one.jpro.platform.routing.Route;
import org.jetbrains.annotations.NotNull;
import simplefx.experimental.parts.FXFuture;

import java.util.Objects;
import java.util.function.Function;

/**
 * Defines {@link Route} filters using basic (username and password) authentication mechanism.
 *
 * @author Besmir Beqiri
 */
public interface AuthFilter {

    /**
     * Creates {@link Route} filter from a given {@link BasicAuthenticationProvider},
     * {@link UsernamePasswordCredentials} and functions for handling successful and error cases.
     *
     * @param authProvider  basic (username and password) authentication provider
     * @param credentials   basic (username and password) credentials
     * @param userFunction  operation on the given user argument
     * @param errorFunction operation on the given error argument
     * @return a {@link Filter} object
     */
    static Filter create(@NotNull BasicAuthenticationProvider authProvider,
                         @NotNull UsernamePasswordCredentials credentials,
                         @NotNull Function<User, Response> userFunction,
                         @NotNull Function<Throwable, Response> errorFunction) {
        Objects.requireNonNull(authProvider, "Authentication provider cannot be null");

        return (route) -> (request) -> {
            if (request.getPath().equals(authProvider.getAuthorizationPath())) {
                return new Response(FXFuture.fromJava(authProvider.authenticate(credentials))
                        .flatMap(user -> userFunction.apply(user).future())
                        .flatExceptionally(error -> errorFunction.apply(error).future()));
            } else {
                return route.apply(request);
            }
        };
    }
}
