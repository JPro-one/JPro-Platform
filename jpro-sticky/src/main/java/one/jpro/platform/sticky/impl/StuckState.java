package one.jpro.platform.sticky.impl;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.Node;
import one.jpro.platform.sticky.Scroll;
import one.jpro.platform.sticky.ScrollPosition;

/**
 * Per-node holder for a {@link ScrollPosition#STICKY} node's <em>stuck</em> (currently pinned) state,
 * publishing it through two synchronized channels from a single write point so they can never drift:
 * <ul>
 *   <li>a {@link ReadOnlyBooleanProperty} (the Java channel) for listeners, bindings and logic; and</li>
 *   <li>the {@link Scroll#STUCK_PSEUDO_CLASS} JavaFX pseudo-class (the CSS channel), toggled on the node so a designer can
 *       restyle a stuck node in JavaFX CSS ({@code .header:stuck { ... }}) with no Java.</li>
 * </ul>
 * One holder lives for the node's life (stashed on {@code node.getProperties()} by {@link Scroll}), so
 * listener identity is stable across clear / re-apply. The active STICKY implementation feeds it through
 * {@link #set(boolean)} at each pin/unpin transition; {@link Scroll} resets it to {@code false} on
 * teardown (which removes the pseudo-class).
 *
 * @author Tobias Horak
 */
public final class StuckState {

    private final Node node;
    private final ReadOnlyBooleanWrapper stuck;

    public StuckState(Node node) {
        this.node = node;
        this.stuck = new ReadOnlyBooleanWrapper(node, "stuck", false);
    }

    /** The read-only Java channel: {@code true} while the node is pinned. */
    public ReadOnlyBooleanProperty property() {
        return stuck.getReadOnlyProperty();
    }

    /** The current stuck state. */
    public boolean get() {
        return stuck.get();
    }

    /**
     * The single write point. Updates the property and toggles the pseudo-class together so the two
     * channels stay in lock-step. A no-op when the value is unchanged.
     *
     * @param value {@code true} if the node is now pinned
     */
    public void set(boolean value) {
        if (stuck.get() == value) {
            return;
        }
        stuck.set(value);
        node.pseudoClassStateChanged(Scroll.STUCK_PSEUDO_CLASS, value);
    }
}
