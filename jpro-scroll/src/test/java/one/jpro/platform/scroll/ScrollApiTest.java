package one.jpro.platform.scroll;

import com.jpro.webapi.WebAPI;
import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Server-side unit tests for the {@link Scroll} positioning API — the mode state machine and
 * its argument contract, exercised headlessly on the JavaFX thread with no browser.
 * <p>
 * The compositor pin itself (the web-side {@code <style>} / {@code animation-timeline}) is not
 * covered here: it only exists under a live JPro session and is verified manually in the browser,
 * matching every other module's browser behaviour.
 * <p>
 * Applying a non-static mode installs a {@link ScrollOverride}, whose {@code install()} calls
 * {@link WebAPI#getWebAPI(javafx.scene.Node, com.jpro.webapi.WebAPIConsumer)}. On a real desktop
 * JPro runtime that consumer never fires, so installation is inert; in a bare unit test the
 * runtime is not bootstrapped and the same call would throw. We therefore stub that static as a
 * no-op — precisely modelling the desktop contract (consumer never fires) — which lets these
 * tests pin down the observable server-side surface: applying a mode records it, modes are
 * mutually exclusive, clearing leaves no residue, and installation never reparents the node.
 *
 * @author Tobias Horak
 */
class ScrollApiTest {

    private MockedStatic<WebAPI> webApiMock;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
            latch.await();
        } catch (IllegalStateException e) {
            // Toolkit already initialized
        }
    }

    @BeforeEach
    void stubDesktopWebAPI() {
        // Desktop contract: the WebAPI consumer never fires, so install() is a no-op. A default
        // static mock leaves the void getWebAPI(Node, consumer) doing exactly nothing.
        webApiMock = Mockito.mockStatic(WebAPI.class);
    }

    @AfterEach
    void releaseWebAPIStub() {
        webApiMock.close();
    }

    // ---------------------------------------------------------------------
    // Read: default state
    // ---------------------------------------------------------------------

    @Test
    void unsetNodeIsStatic() {
        assertEquals(ScrollPosition.STATIC, Scroll.getScrollPosition(new Label()));
    }

    // ---------------------------------------------------------------------
    // Convenience delegators record the right mode
    // ---------------------------------------------------------------------

    @Test
    void setStickyRecordsSticky() {
        Label node = new Label();
        Scroll.setStickyPosition(node);
        assertEquals(ScrollPosition.STICKY, Scroll.getScrollPosition(node));
    }

    @Test
    void setFixedRecordsFixed() {
        Label node = new Label();
        Scroll.setFixedPosition(node);
        assertEquals(ScrollPosition.FIXED, Scroll.getScrollPosition(node));
    }

    @Test
    void stickyDelegatorWithSideAndOffsetStillRecordsSticky() {
        Label node = new Label();
        Scroll.setStickyPosition(node, Side.BOTTOM, 24);
        assertEquals(ScrollPosition.STICKY, Scroll.getScrollPosition(node));
    }

    // ---------------------------------------------------------------------
    // Canonical setter
    // ---------------------------------------------------------------------

    @Test
    void canonicalSetterRecordsPosition() {
        Label node = new Label();
        Scroll.setScrollPosition(node, ScrollPosition.FIXED);
        assertEquals(ScrollPosition.FIXED, Scroll.getScrollPosition(node));
    }

    @Test
    void canonicalSetterWithSideAndOffsetRecordsPosition() {
        Label node = new Label();
        Scroll.setScrollPosition(node, ScrollPosition.STICKY, Side.TOP, 10);
        assertEquals(ScrollPosition.STICKY, Scroll.getScrollPosition(node));
    }

    @Test
    void canonicalSetterWithStaticClears() {
        Label node = new Label();
        Scroll.setStickyPosition(node);
        Scroll.setScrollPosition(node, ScrollPosition.STATIC);
        assertEquals(ScrollPosition.STATIC, Scroll.getScrollPosition(node));
    }

    // ---------------------------------------------------------------------
    // Mutual exclusivity — the modes are one shared slot, last write wins
    // ---------------------------------------------------------------------

    @Test
    void switchingStickyToFixedReplacesMode() {
        Label node = new Label();
        Scroll.setStickyPosition(node);
        Scroll.setFixedPosition(node);
        assertEquals(ScrollPosition.FIXED, Scroll.getScrollPosition(node));
    }

    @Test
    void switchingFixedToStickyReplacesMode() {
        Label node = new Label();
        Scroll.setFixedPosition(node);
        Scroll.setStickyPosition(node);
        assertEquals(ScrollPosition.STICKY, Scroll.getScrollPosition(node));
    }

    @Test
    void repeatedSwitchesDoNotAccumulateState() {
        Label node = new Label();
        Scroll.setStickyPosition(node);
        int afterFirstApply = node.getProperties().size();
        for (int i = 0; i < 10; i++) {
            Scroll.setFixedPosition(node);
            Scroll.setStickyPosition(node);
        }
        // Switching modes tears down before re-installing, so the property footprint is bounded:
        // it must not grow with the number of switches.
        assertEquals(afterFirstApply, node.getProperties().size());
        assertEquals(ScrollPosition.STICKY, Scroll.getScrollPosition(node));
    }

    // ---------------------------------------------------------------------
    // Clearing leaves no residue
    // ---------------------------------------------------------------------

    @Test
    void clearReturnsToStatic() {
        Label node = new Label();
        Scroll.setStickyPosition(node);
        Scroll.clearScrollPosition(node);
        assertEquals(ScrollPosition.STATIC, Scroll.getScrollPosition(node));
    }

    @Test
    void clearLeavesNoResidualProperties() {
        Label node = new Label();
        assertTrue(node.getProperties().isEmpty(), "fresh node should carry no properties");
        Scroll.setFixedPosition(node, Side.TOP, 8);
        assertFalse(node.getProperties().isEmpty(), "applying a mode should stash state");
        Scroll.clearScrollPosition(node);
        assertTrue(node.getProperties().isEmpty(),
                "clearing should remove every property the module stashed (mode, side, offset, override)");
    }

    @Test
    void clearOnUnsetNodeIsANoOp() {
        Label node = new Label();
        Scroll.clearScrollPosition(node);
        assertEquals(ScrollPosition.STATIC, Scroll.getScrollPosition(node));
        assertTrue(node.getProperties().isEmpty());
    }

    // ---------------------------------------------------------------------
    // Desktop no-op: install() is inert off the web — the node keeps its flow slot
    // ---------------------------------------------------------------------

    @Test
    void applyingModeDoesNotReparentNodeOffTheWeb() {
        Label node = new Label("header");
        VBox parent = new VBox(new Label("above"), node, new Label("below"));
        assertSame(parent, node.getParent());
        int indexBefore = parent.getChildren().indexOf(node);

        Scroll.setStickyPosition(node);

        // With the WebAPI consumer not firing, the override never installs, so the node must stay
        // exactly where it was in the flow — not moved into an overlay, not replaced by a placeholder.
        assertSame(parent, node.getParent(), "node should not be reparented on desktop");
        assertEquals(indexBefore, parent.getChildren().indexOf(node));
        assertEquals(3, parent.getChildren().size());
    }

    // ---------------------------------------------------------------------
    // Argument contract
    // ---------------------------------------------------------------------

    @Test
    void nullNodeIsRejected() {
        assertThrows(NullPointerException.class, () -> Scroll.getScrollPosition(null));
        assertThrows(NullPointerException.class, () -> Scroll.setScrollPosition(null, ScrollPosition.STICKY));
        assertThrows(NullPointerException.class, () -> Scroll.setStickyPosition(null));
        assertThrows(NullPointerException.class, () -> Scroll.setFixedPosition(null));
        assertThrows(NullPointerException.class, () -> Scroll.clearScrollPosition(null));
    }

    @Test
    void nullPositionIsRejected() {
        assertThrows(NullPointerException.class,
                () -> Scroll.setScrollPosition(new Label(), null));
    }

    @Test
    void nullSideIsRejected() {
        assertThrows(NullPointerException.class,
                () -> Scroll.setScrollPosition(new Label(), ScrollPosition.STICKY, null, 0));
    }
}
