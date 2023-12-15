package one.jpro.platform.auth.routing;

import one.jpro.platform.auth.core.authentication.User;
import one.jpro.platform.auth.core.oauth2.OAuth2AuthenticationProvider;
import one.jpro.platform.auth.core.oauth2.OAuth2Credentials;
import one.jpro.platform.routing.Filter;
import one.jpro.platform.routing.Response;
import one.jpro.platform.routing.Route;
import simplefx.experimental.parts.FXFuture;

import java.util.Objects;
import java.util.function.Function;

/**
 * Defines a {@link Route} filter using OAuth2 authentication mechanism.
 *
 * @author Besmir Beqiri
 */
public interface OAuth2Filter {

    /**
     * Creates {@link Route} filter from a given {@link OAuth2AuthenticationProvider},
     * {@link OAuth2Credentials} and an operation a given user if the authentication
     * is successful.
     *
     * @param authProvider  the OAuth2 authentication provider
     * @param credentials   the OAuth2 credentials
     * @param userFunction  operation on the given user argument
     * @param errorFunction operation on the given error argument
     * @return a {@link Filter} object
     */
    static Filter create(OAuth2AuthenticationProvider authProvider,
                                OAuth2Credentials credentials,
                                Function<User, FXFuture<Response>> userFunction,
                                Function<Throwable, FXFuture<Response>> errorFunction) {
        Objects.requireNonNull(authProvider, "auth provider can not be null");
        Objects.requireNonNull(credentials, "credentials can not be null");
        Objects.requireNonNull(userFunction, "user function can not be null");
        Objects.requireNonNull(errorFunction, "error function cannot be null");

        return (route) -> (request) -> {
            if (request.path().equals(credentials.getRedirectUri())) {
                return FXFuture.fromJava(authProvider.authenticate(credentials))
                        .flatMap(userFunction::apply)
                        .flatExceptionally(errorFunction::apply);
            } else {
                return route.apply(request);
            }
        };
    }
}
