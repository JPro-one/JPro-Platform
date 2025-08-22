package one.jpro.platform.auth.routing;

import javafx.scene.Node;
import one.jpro.platform.routing.Filter;

/**
 * This class represents an Authentication Method, represented by:
 * 1. A Node that can be used to display the authentication UI.
 * 2. A Filter that can be used to integrate the authentication method
 *   into theo RouteApp.
 */
public interface AuthUIProvider {
    Node createAuthenticationNode();
    Filter createFilter();
}
