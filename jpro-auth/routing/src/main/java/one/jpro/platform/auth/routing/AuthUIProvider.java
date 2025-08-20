package one.jpro.platform.auth.routing;

import javafx.scene.Node;
import one.jpro.platform.routing.Filter;

public interface AuthUIProvider {
    Node createAuthenticationNode();
    Filter createFilter();
}
