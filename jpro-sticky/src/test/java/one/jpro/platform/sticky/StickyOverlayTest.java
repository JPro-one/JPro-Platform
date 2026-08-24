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
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
    // overlayForNode — resolves to the nearest registered host (else the scene
    // root), creates once, caches, and only hosts on a Pane/Group
    // ---------------------------------------------------------------------

    @Test
    void overlayForNodeFallsBackToSceneRootWhenNoHostRegistered() {
        FxTestSupport.onFx(() -> {
            StackPane root = new StackPane();
            Region node = new Region();
            root.getChildren().add(node);
            Scene scene = new Scene(root, 100, 100);

            Group overlay = StickyOverlay.overlayForNode(node);
            assertNotNull(overlay);
            assertEquals("jpro-sticky-overlay", overlay.getId());
            assertTrue(root.getChildren().contains(overlay), "overlay must be a child of the scene root");
            assertFalse(overlay.isManaged(), "overlay must be unmanaged so it shares document coords");
            assertTrue(overlay.getViewOrder() < 0,
                    "overlay must paint above host content (survives a routing content swap)");

            // Second call returns the very same instance (cached on the host) — not a duplicate.
            assertSame(overlay, StickyOverlay.overlayForNode(node));
            assertEquals(1, root.getChildren().stream().filter(n -> n == overlay).count());
        });
    }

    @Test
    void overlayForNodeMountsIntoTheNearestRegisteredHost() {
        FxTestSupport.onFx(() -> {
            StackPane root = new StackPane();
            StackPane outer = new StackPane();
            StackPane inner = new StackPane();
            Region node = new Region();
            inner.getChildren().add(node);
            outer.getChildren().add(inner);
            root.getChildren().add(outer);
            Scene scene = new Scene(root, 100, 100);

            // Both ancestors are hosts; the nearest one on the parent chain wins.
            Scroll.registerOverlayHost(outer);
            Scroll.registerOverlayHost(inner);

            Group overlay = StickyOverlay.overlayForNode(node);
            assertTrue(inner.getChildren().contains(overlay), "overlay must mount into the nearest host");
            assertFalse(outer.getChildren().contains(overlay));
            assertFalse(root.getChildren().contains(overlay));
        });
    }

    @Test
    void overlayForNodeReturnsNullWhenHostCannotHost() {
        FxTestSupport.onFx(() -> {
            // A Label is a Parent but neither a Pane nor a Group, so as scene root it cannot host the
            // overlay; passing the root itself as the node exercises the no-host, non-hostable path.
            Label root = new Label("root");
            Scene scene = new Scene(root, 100, 100);
            assertNull(StickyOverlay.overlayForNode(root));
        });
    }

    @Test
    void overlayForNodeReturnsNullOutsideAScene() {
        FxTestSupport.onFx(() -> assertNull(StickyOverlay.overlayForNode(new Region())));
    }

    @Test
    void isOverlayRecognisesOverlayGroupsOnly() {
        FxTestSupport.onFx(() -> {
            StackPane root = new StackPane();
            Region node = new Region();
            root.getChildren().add(node);
            new Scene(root, 100, 100);

            Group overlay = StickyOverlay.overlayForNode(node);
            assertTrue(StickyOverlay.isOverlay(overlay), "a sticky overlay Group is an overlay");
            assertFalse(StickyOverlay.isOverlay(root), "a Pane host is not an overlay");
            assertFalse(StickyOverlay.isOverlay(new Group()), "an unrelated Group is not an overlay");
            assertFalse(StickyOverlay.isOverlay(null), "null is not an overlay");
        });
    }

    // ---------------------------------------------------------------------
    // remove — an emptied host overlay is detached and its cache stamp cleared
    // ---------------------------------------------------------------------

    @Test
    void removeDetachesAnEmptiedHostOverlayAndClearsItsCache() {
        FxTestSupport.onFx(() -> {
            StackPane root = new StackPane();
            StackPane host = new StackPane();
            Region node = new Region();
            host.getChildren().add(node);
            root.getChildren().add(host);
            Scene scene = new Scene(root, 100, 100);
            Scroll.registerOverlayHost(host);

            Group overlay = StickyOverlay.overlayForNode(node);
            StickyOverlay.insertSorted(overlay, node, 1);
            assertTrue(host.getChildren().contains(overlay));

            StickyOverlay.remove(overlay, node);
            assertFalse(host.getChildren().contains(overlay), "an emptied overlay must detach from its host");

            // Cache stamp cleared: a fresh resolve builds a new overlay rather than reusing the stale one.
            // (insertSorted moved the node into the overlay; put it back under the host, as teardown does.)
            host.getChildren().add(node);
            Group rebuilt = StickyOverlay.overlayForNode(node);
            assertNotNull(rebuilt);
            assertNotSame(overlay, rebuilt, "the emptied overlay's cache must have been cleared");
        });
    }

    // ---------------------------------------------------------------------
    // nextStackOrder — add-order within a tier, FIXED tiered above STICKY
    // ---------------------------------------------------------------------

    @Test
    void nextStackOrderIsMonotonicWithinATier() {
        long a = StickyOverlay.nextStackOrder(ScrollPosition.STICKY);
        long b = StickyOverlay.nextStackOrder(ScrollPosition.STICKY);
        long c = StickyOverlay.nextStackOrder(ScrollPosition.STICKY);
        assertTrue(a < b && b < c, "within a tier the stack order must strictly increase: " + a + ", " + b + ", " + c);
    }

    @Test
    void fixedTiersAboveStickyRegardlessOfApplyOrder() {
        // Apply a FIXED first, then a STICKY: despite FIXED being earlier in add-order, its higher type
        // tier keeps its key greater, so it sorts after (paints in front of) the later sticky node.
        long fixedAppliedFirst = StickyOverlay.nextStackOrder(ScrollPosition.FIXED);
        long stickyAppliedLater = StickyOverlay.nextStackOrder(ScrollPosition.STICKY);
        assertTrue(stickyAppliedLater < fixedAppliedFirst,
                "STICKY must sort below FIXED even when applied later");
    }

    @Test
    void insertSortedPlacesFixedAboveStickyByTier() {
        FxTestSupport.onFx(() -> {
            Group overlay = new Group();
            Label sticky = new Label("sticky");
            Label fixed = new Label("fixed");

            // Apply the fixed node FIRST (lower add-sequence) and the sticky node later.
            long fixedKey = StickyOverlay.nextStackOrder(ScrollPosition.FIXED);
            long stickyKey = StickyOverlay.nextStackOrder(ScrollPosition.STICKY);
            StickyOverlay.insertSorted(overlay, fixed, fixedKey);
            StickyOverlay.insertSorted(overlay, sticky, stickyKey);

            // Sorted ascending: sticky (lower key) first, fixed last — so fixed paints on top.
            assertEquals(List.of(sticky, fixed), overlay.getChildren());
        });
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
