package one.jpro.platform.auth.routing;

import javafx.scene.Node;
import one.jpro.platform.routing.Transformer;

/**
 * Represents an authentication method, made up of two parts:
 * <ol>
 *   <li>A {@link Node} that displays the authentication UI (e.g. a login button or form).</li>
 *   <li>A {@link Transformer} that integrates the authentication method into the {@code RouteApp}.</li>
 * </ol>
 */
public interface AuthUIProvider {

    /**
     * Creates the JavaFX node that renders the authentication UI (e.g. a login button or form).
     *
     * @return the authentication UI node
     */
    Node createAuthenticationNode();

    /**
     * Creates the route filter that handles the authentication callback for this method.
     *
     * @return a {@link Transformer} to apply to the application route
     */
    Transformer createFilter();
}
