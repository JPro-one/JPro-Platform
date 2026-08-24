package one.jpro.platform.sticky;

import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;
import com.jpro.webapi.WebAPIConsumer;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
