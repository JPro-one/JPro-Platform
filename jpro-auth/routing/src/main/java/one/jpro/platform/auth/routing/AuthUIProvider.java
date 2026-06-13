package one.jpro.platform.auth.routing;

import javafx.scene.Node;
import one.jpro.platform.routing.Transformer;

/**
 * This class represents an Authentication Method, represented by:
 * 1. A Node that can be used to display the authentication UI.
 * 2. A Transformer that can be used to integrate the authentication method
 *   into theo RouteApp.
 */
public interface AuthUIProvider {
    Node createAuthenticationNode();
    Transformer createFilter();
}
