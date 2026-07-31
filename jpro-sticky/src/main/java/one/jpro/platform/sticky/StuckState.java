package one.jpro.platform.sticky;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.css.PseudoClass;
import javafx.scene.Node;

/**
 * Per-node holder for a {@link ScrollPosition#STICKY} node's <em>stuck</em> (currently pinned) state,
 * publishing it through two synchronized channels from a single write point so they can never drift:
 * <ul>
 *   <li>a {@link ReadOnlyBooleanProperty} (the Java channel) for listeners, bindings and logic; and</li>
 *   <li>the {@link #STUCK} JavaFX pseudo-class (the CSS channel), toggled on the node so a designer can
 *       restyle a stuck node in JavaFX CSS ({@code .header:stuck { ... }}) with no Java.</li>
 * </ul>
 * One holder lives for the node's life (stashed on {@code node.getProperties()} by {@link Scroll}), so
 * listener identity is stable across clear / re-apply. The active STICKY implementation feeds it through
 * {@link #set(boolean)} at each pin/unpin transition; {@link Scroll} resets it to {@code false} on
 * teardown (which removes the pseudo-class).
 *
 * @author Tobias Horak
 */
final class StuckState {

    /**
     * The {@code :stuck} JavaFX pseudo-class, toggled on a sticky node while it is pinned. It is a
     * JavaFX pseudo-class resolved by JavaFX's own CSS engine (server-side under JPro), not a DOM
     * pseudo-class, so it behaves identically on desktop and web and is used exactly like
     * {@code :hover} / {@code :focused}.
     */
    static final PseudoClass STUCK = PseudoClass.getPseudoClass("stuck");

    private final Node node;
    private final ReadOnlyBooleanWrapper stuck;

    StuckState(Node node) {
        this.node = node;
        this.stuck = new ReadOnlyBooleanWrapper(node, "stuck", false);
    }

    /** The read-only Java channel: {@code true} while the node is pinned. */
    ReadOnlyBooleanProperty property() {
        return stuck.getReadOnlyProperty();
    }

    /** The current stuck state. */
    boolean get() {
        return stuck.get();
    }

    /**
     * The single write point. Updates the property and toggles the pseudo-class together so the two
     * channels stay in lock-step. A no-op when the value is unchanged.
     *
     * @param value {@code true} if the node is now pinned
     */
    void set(boolean value) {
        if (stuck.get() == value) {
            return;
        }
        stuck.set(value);
        node.pseudoClassStateChanged(STUCK, value);
    }
}
