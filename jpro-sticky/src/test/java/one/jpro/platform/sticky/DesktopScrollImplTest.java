package one.jpro.platform.sticky;

import com.jpro.webapi.WebAPI;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless JavaFX tests of the desktop implementations selected by
 * {@link one.jpro.platform.sticky.impl.ScrollDispatcher} when {@link WebAPI#isBrowser()} is false:
 * {@link one.jpro.platform.sticky.impl.DesktopFixedImpl} (a scene-root overlay) and
 * {@link one.jpro.platform.sticky.impl.ScrollPaneStickyImpl} (a {@code translate} pin inside a
 * {@link ScrollPane}). They drive the public {@link Scroll} facade end-to-end and assert the observable
 * desktop behaviour: reparenting, the pin offset, containment release, and clean teardown.
 * <p>
 * {@code isBrowser()} is stubbed to {@code false} <em>inside</em> the FX-thread action, because a
 * {@link MockedStatic} is confined to the thread that opens it and installation runs on the FX thread.
 * Layout is forced synchronously so the {@code ScrollPane} yields real viewport bounds.
 *
 * @author Tobias Horak
 */
class DesktopScrollImplTest {

    private static final double EPS = 1e-6;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FxTestSupport.startToolkit();
    }

    /** Applies CSS and lays out the tree so ScrollPane skins produce real viewport bounds. */
    private static void layout(Parent root) {
        root.applyCss();
        root.layout();
    }

    // ---------------------------------------------------------------------
    // FIXED (desktop) -> DesktopFixedImpl: node moves into the scene overlay, restores on clear
    // ---------------------------------------------------------------------

    @Test
    void fixedReparentsNodeIntoOverlayAndRestoresOnClear() {
        FxTestSupport.onFx(() -> {
            try (MockedStatic<WebAPI> web = Mockito.mockStatic(WebAPI.class)) {
                web.when(WebAPI::isBrowser).thenReturn(false);

                Label above = new Label("above");
                Label node = new Label("fixed");
                Label below = new Label("below");
                VBox parent = new VBox(above, node, below);
                StackPane root = new StackPane(parent);
                Scene scene = new Scene(root, 400, 600);
                layout(root);
                assertSame(parent, node.getParent());
                int originalIndex = parent.getChildren().indexOf(node);

                Scroll.setFixedPosition(node, Side.TOP, 0);

                // The node is lifted out of the VBox into the shared (scene-root) overlay; a placeholder
                // holds its slot so the flow does not collapse.
                Group overlay = (Group) node.getParent();
                assertEquals("jpro-sticky-overlay", overlay.getId(), "fixed node should be mounted in the overlay");
                assertSame(root, overlay.getParent(), "with no host registered the overlay sits at the scene root");
                assertFalse(parent.getChildren().contains(node), "node should have left its flow parent");
                assertEquals(3, parent.getChildren().size(), "placeholder should keep the slot count");
                assertFalse(node.isManaged(), "an overlay-mounted fixed node is unmanaged");

                Scroll.clearScrollPosition(node);

                // Clearing puts the node back exactly where it was and empties the overlay.
                assertSame(parent, node.getParent(), "node should return to its flow parent");
                assertEquals(originalIndex, parent.getChildren().indexOf(node), "node should return to its slot");
                assertEquals(3, parent.getChildren().size());
                assertTrue(node.isManaged(), "restored node should be managed again");
                assertFalse(overlay.getChildren().contains(node), "overlay should no longer hold the node");
            }
        });
    }

    // ---------------------------------------------------------------------
    // STICKY (desktop, ScrollPane ancestor) -> ScrollPaneStickyImpl: translate pins to the pin line
    // ---------------------------------------------------------------------

    @Test
    void stickyPinsToViewportTopWhenScrolledPast() {
        FxTestSupport.onFx(() -> {
            try (MockedStatic<WebAPI> web = Mockito.mockStatic(WebAPI.class)) {
                web.when(WebAPI::isBrowser).thenReturn(false);

                Label header = new Label("HEADER");
                header.setPrefHeight(40);
                VBox content = tallContent(header);
                ScrollPane sp = new ScrollPane(content);
                sp.setPrefViewportHeight(300);
                sp.setPrefViewportWidth(320);
                StackPane root = new StackPane(sp);
                new Scene(root, 320, 300);
                layout(root);

                Scroll.setStickyPosition(header, Side.TOP, 0);
                assertEquals(0, header.getTranslateY(), EPS, "unscrolled: header sits in flow");

                sp.setVvalue(sp.getVmax()); // scroll to the bottom; the sticky listener re-pins
                layout(root);

                // Pinned to the top edge: the header rides down by exactly the scroll offset so it stays
                // at the viewport top, and while stuck it paints in front (negative viewOrder).
                double scrollOffset = content.getLayoutBounds().getHeight() - sp.getViewportBounds().getHeight();
                assertTrue(scrollOffset > 0, "test setup must actually scroll");
                assertEquals(scrollOffset, header.getTranslateY(), EPS, "header should pin to the viewport top");
                assertTrue(header.getViewOrder() < 0, "a stuck header should paint above its siblings");

                Scroll.clearScrollPosition(header);
                assertEquals(0, header.getTranslateY(), EPS, "clearing restores the flow position");
                assertEquals(0, header.getViewOrder(), EPS, "clearing restores the resting viewOrder");
            }
        });
    }

    @Test
    void stickyReleasesAtContainingBlockBottom() {
        FxTestSupport.onFx(() -> {
            try (MockedStatic<WebAPI> web = Mockito.mockStatic(WebAPI.class)) {
                web.when(WebAPI::isBrowser).thenReturn(false);

                Label header = new Label("HEADER");
                header.setPrefHeight(40);
                Region sectionBody = new Region();
                sectionBody.setPrefHeight(160);
                VBox section = new VBox(header, sectionBody); // 200px containing block at the very top
                VBox content = new VBox(section);
                for (int i = 0; i < 40; i++) {
                    Region filler = new Region();
                    filler.setPrefHeight(50);
                    content.getChildren().add(filler); // make the document tall enough to scroll well past
                }
                ScrollPane sp = new ScrollPane(content);
                sp.setPrefViewportHeight(300);
                sp.setPrefViewportWidth(320);
                StackPane root = new StackPane(sp);
                new Scene(root, 320, 300);
                layout(root);

                // Bind the pin to the 200px section: the header may only ride to the section's bottom.
                Scroll.setStickyPosition(header, Side.TOP, 0, section);

                sp.setVvalue(sp.getVmax());
                layout(root);

                double unboundedPin = content.getLayoutBounds().getHeight() - sp.getViewportBounds().getHeight();
                double releaseCap = section.getLayoutBounds().getHeight() - header.getLayoutBounds().getHeight();
                assertTrue(unboundedPin > releaseCap,
                        "test must scroll past the release point (" + unboundedPin + " vs " + releaseCap + ")");
                // Clamped to the containing block: the header stops at (section height - its own height)
                // instead of following the scroll all the way — the CSS sticky release.
                assertEquals(releaseCap, header.getTranslateY(), EPS,
                        "header should release at the section bottom, not follow the scroll");
            }
        });
    }

    // ---------------------------------------------------------------------
    // Content swap: the ScrollPane's content node is replaced (carrying the sticky node with it). The
    // pin must rebind to the live content, not keep computing against the detached old subtree.
    // ---------------------------------------------------------------------

    @Test
    void stickyRebindsWhenTheScrollPaneContentIsSwapped() {
        FxTestSupport.onFx(() -> {
            try (MockedStatic<WebAPI> web = Mockito.mockStatic(WebAPI.class)) {
                web.when(WebAPI::isBrowser).thenReturn(false);

                Label header = new Label("HEADER");
                header.setPrefHeight(40);
                VBox firstContent = tallContent(header); // header rides in the first content (2040px)
                ScrollPane sp = new ScrollPane(firstContent);
                sp.setPrefViewportHeight(300);
                sp.setPrefViewportWidth(320);
                StackPane root = new StackPane(sp);
                new Scene(root, 320, 300);
                layout(root);

                Scroll.setStickyPosition(header, Side.TOP, 0);

                // Swap the content: move the header into a taller content node and install it. A stale
                // binding would keep measuring the (now detached) first content and mis-pin.
                firstContent.getChildren().remove(header);
                VBox secondContent = new VBox(header);
                for (int i = 0; i < 60; i++) { // 40 + 60*50 = 3040px, taller than the first
                    Region row = new Region();
                    row.setPrefHeight(50);
                    secondContent.getChildren().add(row);
                }
                sp.setContent(secondContent);
                layout(root);

                sp.setVvalue(sp.getVmax());
                layout(root);

                // Pinned to the top edge of the live content: the header rides down by exactly the new
                // content's scroll offset, proving the pin rebound off the swapped-in subtree.
                double scrollOffset = secondContent.getLayoutBounds().getHeight() - sp.getViewportBounds().getHeight();
                assertTrue(scrollOffset > 0, "test setup must actually scroll");
                assertEquals(scrollOffset, header.getTranslateY(), EPS,
                        "after a content swap the sticky pin should track the new content");
            }
        });
    }

    // ---------------------------------------------------------------------
    // Placeholder-lifecycle teardown (P1-D): a pinned node is torn out of the overlay when its
    // placeholder (and so the route subtree) leaves the scene, but not on a transient reparent.
    // ---------------------------------------------------------------------

    @Test
    void fixedTearsDownWhenThePlaceholderLeavesTheScene() {
        final Label[] nodeRef = new Label[1];
        final VBox[] parentRef = new VBox[1];
        final Group[] overlayRef = new Group[1];

        FxTestSupport.onFx(() -> {
            try (MockedStatic<WebAPI> web = Mockito.mockStatic(WebAPI.class)) {
                web.when(WebAPI::isBrowser).thenReturn(false);

                Label node = new Label("fixed");
                VBox parent = new VBox(node);
                StackPane root = new StackPane(parent);
                new Scene(root, 400, 600);
                layout(root);

                Scroll.setFixedPosition(node, Side.TOP, 0);
                nodeRef[0] = node;
                parentRef[0] = parent;
                overlayRef[0] = (Group) node.getParent();
                assertTrue(overlayRef[0].getChildren().contains(node), "node should be mounted in the overlay");

                // Unmount the route: the parent (holding the placeholder) leaves the scene.
                root.getChildren().remove(parent);
            }
        });

        // The guarded teardown runs via Platform.runLater on the next pulse; this second onFx is
        // enqueued after it, so by the time it runs the teardown has happened.
        FxTestSupport.onFx(() -> {
            assertFalse(overlayRef[0].getChildren().contains(nodeRef[0]),
                    "placeholder scene->null must tear the node out of the overlay");
            assertSame(parentRef[0], nodeRef[0].getParent(),
                    "the node is restored to its (now-detached) flow parent");
        });
    }

    // ---------------------------------------------------------------------
    // Re-mount (route navigate away, then back): a positioned node whose flow subtree leaves the
    // scene and later returns must re-pin. Today the placeholder teardown is terminal — the
    // dispatcher keeps IMPL_KEY but its delegate is gone and nothing re-attaches on re-entry, so the
    // node reverts to plain flow while getScrollPosition() still reports FIXED. This asserts the fix.
    // ---------------------------------------------------------------------

    @Test
    void fixedReAttachesWhenTheRouteReturnsToTheScene() {
        final Label[] nodeRef = new Label[1];
        final VBox[] parentRef = new VBox[1];
        final StackPane[] rootRef = new StackPane[1];

        // Pin, then unmount the route (parent holding the placeholder leaves the scene).
        FxTestSupport.onFx(() -> {
            try (MockedStatic<WebAPI> web = Mockito.mockStatic(WebAPI.class)) {
                web.when(WebAPI::isBrowser).thenReturn(false);

                Label node = new Label("fixed");
                VBox parent = new VBox(node);
                StackPane root = new StackPane(parent);
                new Scene(root, 400, 600);
                layout(root);

                Scroll.setFixedPosition(node, Side.TOP, 0);
                assertEquals("jpro-sticky-overlay", ((Group) node.getParent()).getId(),
                        "node should start mounted in the overlay");

                nodeRef[0] = node;
                parentRef[0] = parent;
                rootRef[0] = root;

                root.getChildren().remove(parent); // navigate away
            }
        });

        // The guarded teardown has now run (this onFx is enqueued after that runLater). Navigate back:
        // re-add the same subtree to the scene.
        FxTestSupport.onFx(() -> {
            try (MockedStatic<WebAPI> web = Mockito.mockStatic(WebAPI.class)) {
                web.when(WebAPI::isBrowser).thenReturn(false);
                rootRef[0].getChildren().add(parentRef[0]); // navigate back
                layout(rootRef[0]);
            }
        });

        // A pulse later, the node must be pinned again: re-selected and re-installed into the overlay,
        // with getScrollPosition() still consistent. Today it is orphaned in plain flow -> this fails.
        FxTestSupport.onFx(() -> {
            assertEquals(ScrollPosition.FIXED, Scroll.getScrollPosition(nodeRef[0]),
                    "position mode should survive a route round-trip");
            assertTrue(nodeRef[0].getParent() instanceof Group
                            && "jpro-sticky-overlay".equals(((Group) nodeRef[0].getParent()).getId()),
                    "returning to the scene must re-pin the node into the overlay, not leave it in plain flow");
        });
    }

    @Test
    void fixedDoesNotTearDownOnASamePulseDetachReattach() {
        final Label[] nodeRef = new Label[1];
        final Group[] overlayRef = new Group[1];

        FxTestSupport.onFx(() -> {
            try (MockedStatic<WebAPI> web = Mockito.mockStatic(WebAPI.class)) {
                web.when(WebAPI::isBrowser).thenReturn(false);

                Label node = new Label("fixed");
                VBox parent = new VBox(node);
                StackPane root = new StackPane(parent);
                new Scene(root, 400, 600);
                layout(root);

                Scroll.setFixedPosition(node, Side.TOP, 0);
                nodeRef[0] = node;
                overlayRef[0] = (Group) node.getParent();

                // Same-pulse churn: detach then immediately reattach the route subtree. The placeholder
                // is back in a scene before the guarded teardown runs, so it must be a no-op.
                root.getChildren().remove(parent);
                root.getChildren().add(parent);
            }
        });

        FxTestSupport.onFx(() -> assertTrue(overlayRef[0].getChildren().contains(nodeRef[0]),
                "a transient same-pulse detach/reattach must not tear the node out of the overlay"));
    }

    /** A VBox whose first child is {@code header} (40px) followed by 40 x 50px rows: 2040px tall. */
    private static VBox tallContent(Label header) {
        VBox content = new VBox(header);
        for (int i = 0; i < 40; i++) {
            Region row = new Region();
            row.setPrefHeight(50);
            content.getChildren().add(row);
        }
        return content;
    }
}
