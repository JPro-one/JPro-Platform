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
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Utility class with authorization filters used in the routing process.
 *
 * @author Besmir Beqiri
 */
public final class AuthFilters {

    /**
     * Creates {@link Route} filter from a given {@link OAuth2AuthenticationProvider},
     * {@link OAuth2Credentials} and an operation a given user if the authentication
     * is successful.
     *
     * @param authProvider  the JWT authentication provider
     * @param credentials   a JSON object with the authentication information
     * @param authPath      the authentication path for the routing
     * @param tokenPath     the token path
     * @param userConsumer  operation on the given user argument
     * @param errorConsumer operation on the given error argument
     * @return a {@link Filter} object
     */
    static Filter jwt(JWTAuthenticationProvider authProvider,
                      JSONObject credentials,
                      String authPath,
                      String tokenPath,
                      Consumer<? super User> userConsumer,
                      Consumer<? super Throwable> errorConsumer) {
        Objects.requireNonNull(authProvider, "auth provider cannot be null");
        Objects.requireNonNull(credentials, "credentials cannot be null");
        Objects.requireNonNull(authPath, "authentication path cannot be null");
        Objects.requireNonNull(tokenPath, "token path cannot be null");
        Objects.requireNonNull(userConsumer, "user consumer cannot be null");
        Objects.requireNonNull(errorConsumer, "error consumer cannot be null");

        return (route) -> (request) -> {
            if (request.path().equals(authPath)) {
                return FXFuture.fromJava(authProvider.token(tokenPath, credentials)
                                .thenCompose(authProvider::authenticate))
                        .flatMap(user -> {
                            userConsumer.accept(user);
                            return route.apply(request);
                        })
                        .flatExceptionally(ex -> {
                            errorConsumer.accept(ex);
                            return route.apply(request);
                        });
            } else {
                return route.apply(request);
            }
        };
    }

    /**
     * Creates {@link Route} filter from a given {@link OAuth2AuthenticationProvider},
     * {@link OAuth2Credentials} and an operation a given user if the authentication
     * is successful.
     *
     * @param authProvider  the OAuth2 authentication provider
     * @param credentials   the OAuth2 credentials
     * @param userConsumer  consumer operation on the given user argument
     * @param errorConsumer consumer operation on the given error argument
     * @return a {@link Filter} object
     */
    public static Filter oauth2(OAuth2AuthenticationProvider authProvider,
                                OAuth2Credentials credentials,
                                Consumer<? super User> userConsumer,
                                Consumer<? super Throwable> errorConsumer) {
        Objects.requireNonNull(authProvider, "auth provider can not be null");
        Objects.requireNonNull(credentials, "credentials can not be null");
        Objects.requireNonNull(userConsumer, "user consumer can not be null");
        Objects.requireNonNull(errorConsumer, "error consumer cannot be null");

        return (route) -> (request) -> {
            if (request.path().equals(credentials.getRedirectUri())) {
                return FXFuture.fromJava(authProvider.authenticate(credentials))
                        .flatMap(user -> {
                            userConsumer.accept(user);
                            return route.apply(request);
                        })
                        .flatExceptionally(ex -> {
                            errorConsumer.accept(ex);
                            return route.apply(request);
                        });
            } else {
                return route.apply(request);
            }
        };
    }

    /**
     * Creates {@link Route} filter from a given {@link OAuth2AuthenticationProvider},
     * {@link OAuth2Credentials} and an operation a given user if the authentication
     * is successful.
     *
     * @param authProvider  the OAuth2 authentication provider
     * @param credentials   the OAuth2 credentials
     * @param userFunction  function operation on the given user argument
     * @param errorFunction function operation on the given error argument
     * @return a {@link Filter} object
     */
    public static Filter oauth2(OAuth2AuthenticationProvider authProvider,
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
                        .flatMap(user -> {
                            userFunction.apply(user);
                            return route.apply(request);
                        })
                        .flatExceptionally(ex -> {
                            errorFunction.apply(ex);
                            return route.apply(request);
                        });
            } else {
                return route.apply(request);
            }
        };
    }

    private AuthFilters() {
        // Hide the default constructor.
    }
}
