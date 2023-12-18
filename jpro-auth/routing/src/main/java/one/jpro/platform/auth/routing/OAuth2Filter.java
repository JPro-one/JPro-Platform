package one.jpro.platform.auth.routing;

import one.jpro.platform.auth.core.authentication.User;
import one.jpro.platform.auth.core.oauth2.OAuth2AuthenticationProvider;
import one.jpro.platform.auth.core.oauth2.OAuth2Credentials;
import one.jpro.platform.auth.core.oauth2.provider.OpenIDAuthenticationProvider;
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
     * @param authProvider  the OpenID authentication provider
     * @param userFunction  operation on the given user argument
     * @param errorFunction operation on the given error argument
     * @return a {@link Filter} object
     */
    static Filter create(OpenIDAuthenticationProvider authProvider,
                         Function<User, Response> userFunction,
                         Function<Throwable, Response> errorFunction) {
        final var credentials = authProvider.getCredentials();
        return create(authProvider, credentials, userFunction, errorFunction);
    }

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
                         Function<User, Response> userFunction,
                         Function<Throwable, Response> errorFunction) {
        Objects.requireNonNull(authProvider, "auth provider can not be null");
        Objects.requireNonNull(credentials, "credentials can not be null");
        Objects.requireNonNull(userFunction, "user function can not be null");
        Objects.requireNonNull(errorFunction, "error function cannot be null");

        return (route) -> (request) -> {
            if (request.path().equals(credentials.getRedirectUri())) {
                return new Response(FXFuture.fromJava(authProvider.authenticate(credentials))
                        .flatMap(r -> userFunction.apply(r).future())
                        .flatExceptionally(r -> errorFunction.apply(r).future()));
            } else {
                return route.apply(request);
            }
        };
    }
}
