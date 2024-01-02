package one.jpro.platform.auth.routing;

import com.jpro.webapi.WebAPI;
import javafx.scene.Node;
import one.jpro.platform.auth.core.authentication.User;
import one.jpro.platform.auth.core.oauth2.OAuth2AuthenticationProvider;
import one.jpro.platform.auth.core.oauth2.OAuth2Credentials;
import one.jpro.platform.auth.core.oauth2.provider.OpenIDAuthenticationProvider;
import one.jpro.platform.routing.*;
import simplefx.experimental.parts.FXFuture;

import java.util.Objects;
import java.util.function.Function;

/**
 * Defines {@link Route} filters using OAuth2 authentication mechanism.
 *
 * @author Besmir Beqiri
 */
public interface OAuth2Filter {

    /**
     * Creates {@link Route} filter from a given {@link OAuth2AuthenticationProvider},
     * {@link OAuth2Credentials} and functions for handling successful and error cases.
     *
     * @param openidAuthProvider the OpenID authentication provider
     * @param userFunction       operation on the given user argument
     * @param errorFunction      operation on the given error argument
     * @return a {@link Filter} object
     */
    static Filter create(OpenIDAuthenticationProvider openidAuthProvider,
                         Function<User, Response> userFunction,
                         Function<Throwable, Response> errorFunction) {
        final var credentials = openidAuthProvider.getCredentials();
        return create(openidAuthProvider, credentials, userFunction, errorFunction);
    }

    /**
     * Creates {@link Route} filter from a given {@link OAuth2AuthenticationProvider},
     * {@link OAuth2Credentials} and functions for handling successful and error cases.
     *
     * @param authProvider  an OAuth2 authentication provider
     * @param credentials   an OAuth2 credentials
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
            if (request.getPath().equals(credentials.getRedirectUri())) {
                return new Response(FXFuture.fromJava(authProvider.authenticate(credentials))
                        .flatMap(r -> userFunction.apply(r).future())
                        .flatExceptionally(r -> errorFunction.apply(r).future()));
            } else {
                return route.apply(request);
            }
        };
    }

    /**
     * Initiates the authorization process for a given OAuth2 authentication provider,
     * updating the provided JavaFX Node with the authorization URL.
     *
     * @param node         the JavaFX node context for the authorization
     * @param authProvider the OAuth2 authentication provider
     * @param credentials  the OAuth2 credentials
     */
    static void authorize(Node node, OAuth2AuthenticationProvider authProvider, OAuth2Credentials credentials) {
        Objects.requireNonNull(node, "node can not be null");
        Objects.requireNonNull(authProvider, "auth provider can not be null");

        FXFuture.fromJava(authProvider.authorizeUrl(credentials))
                .map(url -> {
                    // gotoURL call is only needed when running as a desktop app
                    if (!WebAPI.isBrowser()) {
                        LinkUtil.getSessionManager(node).gotoURL(url);
                    }
                    return url;
                });
    }

    /**
     * Initiates the authorization process for a given OpenID authentication provider,
     * updating the provided JavaFX Node with the authorization URL.
     *
     * @param node               the JavaFX node context for the authorization
     * @param openidAuthProvider the OpenID authentication provider
     */
    static void authorize(Node node, OpenIDAuthenticationProvider openidAuthProvider) {
        Objects.requireNonNull(node, "node can not be null");
        Objects.requireNonNull(openidAuthProvider, "auth provider can not be null");

        FXFuture.fromJava(openidAuthProvider.authorizeUrl())
                .map(url -> {
                    // gotoURL call is only needed when running as a desktop app
                    if (!WebAPI.isBrowser()) {
                        LinkUtil.getSessionManager(node).gotoURL(url);
                    }
                    return url;
                });
    }
}
