package one.jpro.platform.auth.routing;

import one.jpro.platform.routing.Transformer;
import one.jpro.platform.routing.Response;
import org.jetbrains.annotations.NotNull;

public class AuthRestrictionTransformer {
    /**
     * This makes the whole UI only accessible to authenticated users.
     */
    public static Transformer create(AuthUIProvider routingAuthenticationProvider,
                                                 @NotNull UserSession userSession) {
        var filter = routingAuthenticationProvider.createTransformer();
        Transformer result1 = (route) -> (request) -> {
            if (userSession.getUser() != null) {
                return route.apply(request);
            } else {
                return Response.node(routingAuthenticationProvider.createAuthenticationNode());
            }
        };
        return filter.compose(result1);
    }
}
