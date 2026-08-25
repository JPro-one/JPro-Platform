package one.jpro.platform.sticky;

import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;
import com.jpro.webapi.WebAPIConsumer;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

/**
 * Headless JavaFX test of the web compositor path
 * ({@link one.jpro.platform.sticky.impl.WebScrollImpl}) selected by
 * {@link one.jpro.platform.sticky.impl.ScrollDispatcher} when {@link WebAPI#isBrowser()} is true.
 * <p>
 * The compositor motion itself is driven by injected CSS in the browser and cannot run headless, so
 * this does not assert pixel positions (that is verified manually on {@code ScrollSample}). What it
 * does exercise is the shared reparenting <em>lifecycle</em>: the node is lifted into the overlay,
 * and, after its flow subtree leaves the scene and later returns (a route navigate-away / back), it
 * is re-pinned rather than orphaned in plain flow. {@link WebAPI} is stubbed just enough to let
 * install / sync / teardown run: {@code getWebAPI} delivers a mock synchronously and the mock answers
 * the viewport, element, and {@code executeScript} calls the compositor path makes.
 *
 * @author Tobias Horak
 */
class WebScrollImplTest {

    private static final Rectangle2D VIEWPORT = new Rectangle2D(0, 0, 400, 600);

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FxTestSupport.startToolkit();
    }

    private static void layout(Parent root) {
        root.applyCss();
        root.layout();
    }

    /** A browser-mode {@link WebAPI} stub wired to deliver itself and answer the compositor's calls. */
    private static WebAPI stubBrowser(MockedStatic<WebAPI> web) {
        web.when(WebAPI::isBrowser).thenReturn(true);
        final WebAPI api = Mockito.mock(WebAPI.class);
        final ReadOnlyObjectProperty<Rectangle2D> viewport = new SimpleObjectProperty<>(VIEWPORT);
        Mockito.when(api.browserViewport()).thenReturn(viewport);
        Mockito.when(api.getBrowserViewport()).thenReturn(VIEWPORT);
        final JSVariable element = Mockito.mock(JSVariable.class);
        Mockito.when(element.getName()).thenReturn("window.__jproStickyTestElement");
        Mockito.when(api.getElement(any(Node.class))).thenReturn(element);
        // getWebAPI(node, consumer) is static void: deliver the stub synchronously so attach() runs inline.
        web.when(() -> WebAPI.getWebAPI(any(Node.class), any(WebAPIConsumer.class)))
                .thenAnswer(invocation -> {
                    invocation.getArgument(1, WebAPIConsumer.class).consume(api);
                    return null;
                });
        return api;
    }

    private static boolean inOverlay(Node node) {
        return node.getParent() instanceof Group
                && "jpro-sticky-overlay".equals(((Group) node.getParent()).getId());
    }

    // ---------------------------------------------------------------------
    // Re-mount (route navigate away, then back): the web-pinned node must re-pin on re-entry, not be
    // left orphaned in plain flow with getScrollPosition() still reporting FIXED.
    // ---------------------------------------------------------------------

    @Test
    void webFixedReAttachesWhenTheRouteReturnsToTheScene() {
        final Label[] nodeRef = new Label[1];
        final VBox[] parentRef = new VBox[1];
        final StackPane[] rootRef = new StackPane[1];

        // Pin in browser mode, then unmount the route (parent holding the placeholder leaves the scene).
        FxTestSupport.onFx(() -> {
            try (MockedStatic<WebAPI> web = Mockito.mockStatic(WebAPI.class)) {
                stubBrowser(web);

                Label node = new Label("fixed");
                VBox parent = new VBox(node);
                StackPane root = new StackPane(parent);
                new Scene(root, 400, 600);
                layout(root);

                Scroll.setFixedPosition(node, Side.TOP, 0);
                layout(root);
                assertTrue(inOverlay(node), "web fixed node should start mounted in the overlay");

                nodeRef[0] = node;
                parentRef[0] = parent;
                rootRef[0] = root;

                root.getChildren().remove(parent); // navigate away
            }
        });

        // The guarded teardown (route unmount -> dispatcher detach) has now run. Navigate back.
        FxTestSupport.onFx(() -> {
            try (MockedStatic<WebAPI> web = Mockito.mockStatic(WebAPI.class)) {
                stubBrowser(web);
                rootRef[0].getChildren().add(parentRef[0]); // navigate back; scene re-entry re-installs inline
                layout(rootRef[0]);
            }
        });

        // The node must be pinned again: re-selected and re-installed into the overlay.
        FxTestSupport.onFx(() -> {
            assertEquals(ScrollPosition.FIXED, Scroll.getScrollPosition(nodeRef[0]),
                    "position mode should survive a route round-trip");
            assertTrue(inOverlay(nodeRef[0]),
                    "returning to the scene must re-pin the web node into the overlay, not leave it in plain flow");
        });
    }

    // ---------------------------------------------------------------------
    // Stuck-state lifecycle: a web sticky node that is pinned (stuck) when its route leaves the scene
    // must not keep reporting stuck. The reparenting teardown runs off the dispatcher's detach path,
    // which does not go through Scroll.setScrollPosition's central reset, so the stuck channel has to be
    // cleared there too or stuckProperty / :stuck stay latched on an off-screen node.
    // ---------------------------------------------------------------------

    @Test
    void webStickyStuckStateResetsWhenTheRouteLeavesTheScene() {
        final Label[] nodeRef = new Label[1];

        FxTestSupport.onFx(() -> {
            try (MockedStatic<WebAPI> web = Mockito.mockStatic(WebAPI.class)) {
                web.when(WebAPI::isBrowser).thenReturn(true);
                final WebAPI api = Mockito.mock(WebAPI.class);
                // A mutable viewport so we can simulate a native scroll; getBrowserViewport() reads it live.
                final SimpleObjectProperty<Rectangle2D> viewport = new SimpleObjectProperty<>(VIEWPORT);
                Mockito.when(api.browserViewport()).thenReturn(viewport);
                Mockito.when(api.getBrowserViewport()).thenAnswer(inv -> viewport.get());
                final JSVariable element = Mockito.mock(JSVariable.class);
                Mockito.when(element.getName()).thenReturn("window.__jproStickyTestElement");
                Mockito.when(api.getElement(any(Node.class))).thenReturn(element);
                web.when(() -> WebAPI.getWebAPI(any(Node.class), any(WebAPIConsumer.class)))
                        .thenAnswer(inv -> {
                            inv.getArgument(1, WebAPIConsumer.class).consume(api);
                            return null;
                        });

                Label header = new Label("HEADER");
                header.setPrefHeight(40);
                VBox content = new VBox(header); // tall containing block so the pin does not release early
                for (int i = 0; i < 40; i++) {
                    Region row = new Region();
                    row.setPrefHeight(50);
                    content.getChildren().add(row);
                }
                StackPane root = new StackPane(content);
                root.setAlignment(Pos.TOP_LEFT); // keep the flow top at scene y = 0, so scrolling drives the pin
                new Scene(root, 400, 600);
                layout(root);

                // STICKY, browser, no ScrollPane ancestor -> WebScrollImpl (the reparenting path with a stuck sink).
                Scroll.setStickyPosition(header, Side.TOP, 0);
                layout(root);

                // Simulate a native scroll down: the viewport top passes the header's flow top, so the server
                // pin lifts it off its natural position -> stuck.
                viewport.set(new Rectangle2D(0, 200, 400, 600));
                layout(root);
                assertTrue(Scroll.isStuck(header), "scrolled past its flow top, the sticky header should read stuck");

                nodeRef[0] = header;
                root.getChildren().remove(content); // navigate away: the flow slot (placeholder) leaves the scene
            }
        });

        // After the guarded detach runs, the node is no longer pinned; its stuck channel must reflect that.
        FxTestSupport.onFx(() ->
                assertFalse(Scroll.isStuck(nodeRef[0]),
                        "a node whose route left the scene is not pinned; its stuck state must reset to false"));
    }
}
