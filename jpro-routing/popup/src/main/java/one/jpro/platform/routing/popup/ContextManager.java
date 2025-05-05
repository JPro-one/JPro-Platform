package one.jpro.platform.routing.popup;

import javafx.css.Styleable;
import javafx.scene.Node;

import java.util.Map;
import java.util.Optional;

public class ContextManager<A> {

    // unique per ContextManager instance
    private final Object CONTEXT_KEY = new Object();

    /**
     * Associate a context object of type A with this Node.
     */
    public void setContext(Node node, A context) {
        node.getProperties().put(CONTEXT_KEY, context);
    }

    /**
     * Look up the nearest context of type A on this node or its
     * CSS “styleable” ancestors.  Throws if none is found.
     */
    public A getContext(Node node) {
        return findContext(node)
                .orElseThrow(() -> new IllegalStateException(
                        "Couldn't find context for node: " + node
                ));
    }

    /**
     * Recursively search this styleable and its styleableParent chain
     * for a stored context under our CONTEXT_KEY.
     */
    @SuppressWarnings("unchecked")
    private Optional<A> findContext(Styleable styleable) {
        // If this styleable is a Node, check its properties map first
        if (styleable instanceof Node) {
            Node n = (Node) styleable;
            Map<Object, Object> props = n.getProperties();
            if (props.containsKey(CONTEXT_KEY)) {
                return Optional.of((A) props.get(CONTEXT_KEY));
            }
        }

        // Otherwise, recurse up the CSS “styleable” parent chain
        Styleable parent = styleable.getStyleableParent();
        if (parent != null) {
            return findContext(parent);
        }

        return Optional.empty();
    }
}
