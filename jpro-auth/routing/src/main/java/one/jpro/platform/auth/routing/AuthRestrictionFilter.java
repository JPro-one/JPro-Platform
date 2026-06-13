package one.jpro.platform.auth.routing;

import one.jpro.platform.routing.Transformer;
import one.jpro.platform.routing.Response;
import org.jetbrains.annotations.NotNull;

public class AuthRestrictionFilter {
    /**
     * This makes the whole UI only accessible to authenticated users.
     */
    public static Transformer create(AuthUIProvider routingAuthenticationProvider,
                                                 @NotNull UserSession userSession) {
        var filter = routingAuthenticationProvider.createFilter();
        Transformer result1 = (route) -> (request) -> {
            System.out.println("Got user: " + userSession.getUser());
            if (userSession.getUser() != null) {
                return route.apply(request);
            } else {
                return Response.node(routingAuthenticationProvider.createAuthenticationNode());
            }
        };
        return filter.compose(result1);
    }
}
