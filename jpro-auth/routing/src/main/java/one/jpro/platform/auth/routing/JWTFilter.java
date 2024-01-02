package one.jpro.platform.auth.routing;

import one.jpro.platform.auth.core.authentication.User;
import one.jpro.platform.auth.core.jwt.JWTAuthenticationProvider;
import one.jpro.platform.auth.core.oauth2.OAuth2AuthenticationProvider;
import one.jpro.platform.auth.core.oauth2.OAuth2Credentials;
import one.jpro.platform.routing.Filter;
import one.jpro.platform.routing.Response;
import one.jpro.platform.routing.Route;
import org.json.JSONObject;
import simplefx.experimental.parts.FXFuture;

import java.util.Objects;
import java.util.function.Function;

/**
 * Defines {@link Route} filters using JWT authentication mechanism.
 *
 * @author Besmir Beqiri
 */
public interface JWTFilter {

    /**
     * Creates {@link Route} filter from a given {@link OAuth2AuthenticationProvider},
     * {@link OAuth2Credentials} and functions for handling successful and error cases.
     *
     * @param authProvider  the JWT authentication provider
     * @param credentials   a JSON object with the authentication information
     * @param authPath      the authentication path for the routing
     * @param tokenPath     the token path
     * @param userFunction  operation on the given user argument
     * @param errorFunction operation on the given error argument
     * @return a {@link Filter} object
     */
    static Filter create(JWTAuthenticationProvider authProvider,
                         JSONObject credentials,
                         String authPath,
                         String tokenPath,
                         Function<User, Response> userFunction,
                         Function<Throwable, Response> errorFunction) {
        Objects.requireNonNull(authProvider, "auth provider cannot be null");
        Objects.requireNonNull(credentials, "credentials cannot be null");
        Objects.requireNonNull(authPath, "authentication path cannot be null");
        Objects.requireNonNull(tokenPath, "token path cannot be null");
        Objects.requireNonNull(userFunction, "user function cannot be null");
        Objects.requireNonNull(errorFunction, "error function cannot be null");

        return (route) -> (request) -> {
            if (request.getPath().equals(authPath)) {
                return new Response(FXFuture.fromJava(authProvider.token(tokenPath, credentials)
                                .thenCompose(authProvider::authenticate))
                        .flatMap(user -> userFunction.apply(user).future())
                        .flatExceptionally(error -> errorFunction.apply(error).future()));
            } else {
                return route.apply(request);
            }
        };
    }
}
