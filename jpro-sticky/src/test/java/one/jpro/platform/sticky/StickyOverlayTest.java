package one.jpro.platform.sticky;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link StickyOverlay} — the per-scene host that both the web and desktop paths mount
 * pinned nodes into, and the source-order stacking model shared between them (STICKY_DESIGN.md §8).
 * These run on the JavaFX thread but need no layout: they exercise the overlay's structure and the
 * {@code insertSorted}/{@code remove} bookkeeping directly.
 *
 * @author Tobias Horak
 */
class StickyOverlayTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FxTestSupport.startToolkit();
    }

    // ---------------------------------------------------------------------
    // forScene — creates once, caches, only hosts on a Pane/Group root
    // ---------------------------------------------------------------------

    @Test
    void forSceneCreatesOverlayUnderPaneRootAndCachesIt() {
        FxTestSupport.onFx(() -> {
            StackPane root = new StackPane();
            Scene scene = new Scene(root, 100, 100);

            Group overlay = StickyOverlay.forScene(scene);
            assertNotNull(overlay);
            assertEquals("jpro-sticky-overlay", overlay.getId());
            assertTrue(root.getChildren().contains(overlay), "overlay must be a child of the scene root");
            assertFalse(overlay.isManaged(), "overlay must be unmanaged so it shares document coords");

            // Second call returns the very same instance (cached on the scene) — not a duplicate.
            assertSame(overlay, StickyOverlay.forScene(scene));
            assertEquals(1, root.getChildren().stream().filter(n -> n == overlay).count());
        });
    }

    @Test
    void forSceneReturnsNullWhenRootCannotHost() {
        FxTestSupport.onFx(() -> {
            // A Label is a Parent but neither a Pane nor a Group, so it cannot host the overlay.
            Scene scene = new Scene(new Label("root"), 100, 100);
            assertNull(StickyOverlay.forScene(scene));
        });
    }

    // ---------------------------------------------------------------------
    // nextStackOrder — strictly increasing
    // ---------------------------------------------------------------------

    @Test
    void nextStackOrderIsMonotonic() {
        long a = StickyOverlay.nextStackOrder();
        long b = StickyOverlay.nextStackOrder();
        long c = StickyOverlay.nextStackOrder();
        assertTrue(a < b && b < c, "stack order must strictly increase: " + a + ", " + b + ", " + c);
    }

    // ---------------------------------------------------------------------
    // insertSorted — children ordered by stack order regardless of insertion order
    // ---------------------------------------------------------------------

    @Test
    void insertSortedOrdersBySourceNotInsertionOrder() {
        FxTestSupport.onFx(() -> {
            Group overlay = new Group();
            Label a = new Label("a");
            Label b = new Label("b");
            Label c = new Label("c");

            // Insert out of order (b, then a, then c) but with source-order stack values a<b<c.
            StickyOverlay.insertSorted(overlay, b, 5);
            StickyOverlay.insertSorted(overlay, a, 2);
            StickyOverlay.insertSorted(overlay, c, 8);

            // Children must be ordered a, b, c — later source order paints on top (last in list).
            assertEquals(List.of(a, b, c), overlay.getChildren());
        });
    }

    @Test
    void insertSortedAppendsEqualOrHigherAfterExisting() {
        FxTestSupport.onFx(() -> {
            Group overlay = new Group();
            Label first = new Label("first");
            Label second = new Label("second");
            StickyOverlay.insertSorted(overlay, first, 1);
            StickyOverlay.insertSorted(overlay, second, 3);
            assertEquals(List.of(first, second), overlay.getChildren());
        });
    }

    // ---------------------------------------------------------------------
    // remove — drops the node and its stashed order; null overlay is a no-op
    // ---------------------------------------------------------------------

    @Test
    void removeDropsNodeFromOverlay() {
        FxTestSupport.onFx(() -> {
            Group overlay = new Group();
            Region node = new Region();
            StickyOverlay.insertSorted(overlay, node, 1);
            assertTrue(overlay.getChildren().contains(node));

            StickyOverlay.remove(overlay, node);
            assertFalse(overlay.getChildren().contains(node));
        });
    }

    @Test
    void removeWithNullOverlayIsANoOp() {
        FxTestSupport.onFx(() -> {
            Region node = new Region();
            // A node that was never mounted (null overlay) must not blow up on removal.
            StickyOverlay.remove(null, node);
        });
    }
}
