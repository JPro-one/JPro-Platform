package one.jpro.platform.sticky;

import com.jpro.webapi.WebAPI;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.geometry.Side;
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

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless JavaFX tests of the sticky <em>observability</em> channels ({@code STICKY_OBSERVABILITY_PLAN.md}):
 * the {@link Scroll#stuckProperty} Java channel and the {@link Scroll#STUCK_PSEUDO_CLASS} CSS channel,
 * driven through {@link FXStickyImpl} (the desktop / FX-ScrollPane path). Both channels flip from a
 * single write point ({@link StuckState}), so these assert they move together as a {@link ScrollPane}
 * scrolls past the pin line and back, that the property instance is stable across clear / re-apply, and
 * that a non-sticky (STATIC / FIXED) node reads {@code false}.
 * <p>
 * The web {@link ScrollOverride} path derives {@code stuck} from the same rule (server pin vs flow top)
 * but cannot run headless (it needs a live {@link WebAPI}); it is covered by the shared rule here plus a
 * manual browser check on {@code ScrollSample}. {@code isBrowser()} is stubbed {@code false} inside the
 * FX-thread action for the same reason as {@link DesktopScrollImplTest} (a {@link MockedStatic} is
 * thread-confined).
 *
 * @author Tobias Horak
 */
class StuckObservabilityTest {

    private static final double EPS = 1e-6;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FxTestSupport.startToolkit();
    }

    private static void layout(Parent root) {
        root.applyCss();
        root.layout();
    }

    /** A ScrollPane over {@code header} (40px) followed by 40 x 50px rows (2040px tall content). */
    private static ScrollPane scroller(Label header) {
        header.setPrefHeight(40);
        VBox content = new VBox(header);
        for (int i = 0; i < 40; i++) {
            Region row = new Region();
            row.setPrefHeight(50);
            content.getChildren().add(row);
        }
        ScrollPane sp = new ScrollPane(content);
        sp.setPrefViewportHeight(300);
        sp.setPrefViewportWidth(320);
        return sp;
    }

    // ---------------------------------------------------------------------
    // Both channels flip together as the pin line is crossed, and reset on scroll-back
    // ---------------------------------------------------------------------

    @Test
    void stuckPropertyAndPseudoClassTrackThePin() {
        FxTestSupport.onFx(() -> {
            try (MockedStatic<WebAPI> web = Mockito.mockStatic(WebAPI.class)) {
                web.when(WebAPI::isBrowser).thenReturn(false);

                Label header = new Label("HEADER");
                ScrollPane sp = scroller(header);
                StackPane root = new StackPane(sp);
                new Scene(root, 320, 300);
                layout(root);

                // Observe before applying: the property is created lazily and the sticky call reuses it.
                ReadOnlyBooleanProperty stuck = Scroll.stuckProperty(header);
                AtomicInteger flips = new AtomicInteger();
                stuck.addListener((obs, was, is) -> flips.incrementAndGet());

                Scroll.setStickyPosition(header, Side.TOP, 0);
                layout(root);
                assertFalse(stuck.get(), "unscrolled: not stuck");
                assertFalse(Scroll.isStuck(header));
                assertFalse(header.getPseudoClassStates().contains(Scroll.STUCK_PSEUDO_CLASS),
                        ":stuck absent while in flow");
                assertEquals(0, flips.get(), "no transition yet");

                sp.setVvalue(sp.getVmax()); // scroll past the pin line
                layout(root);
                assertTrue(stuck.get(), "pinned: property true");
                assertTrue(Scroll.isStuck(header));
                assertTrue(header.getPseudoClassStates().contains(Scroll.STUCK_PSEUDO_CLASS),
                        ":stuck present while pinned");
                assertEquals(1, flips.get(), "one transition to stuck");

                sp.setVvalue(sp.getVmin()); // scroll back above the pin line
                layout(root);
                assertFalse(stuck.get(), "unpinned again: property false");
                assertFalse(header.getPseudoClassStates().contains(Scroll.STUCK_PSEUDO_CLASS),
                        ":stuck removed when it releases");
                assertEquals(2, flips.get(), "one transition back to unstuck");
            }
        });
    }

    // ---------------------------------------------------------------------
    // A bounded sticky node stays stuck through the containment release (it is still displaced)
    // ---------------------------------------------------------------------

    @Test
    void stuckStaysTrueAtContainingBlockRelease() {
        FxTestSupport.onFx(() -> {
            try (MockedStatic<WebAPI> web = Mockito.mockStatic(WebAPI.class)) {
                web.when(WebAPI::isBrowser).thenReturn(false);

                Label header = new Label("HEADER");
                header.setPrefHeight(40);
                Region body = new Region();
                body.setPrefHeight(160);
                VBox section = new VBox(header, body); // 200px containing block at the top
                VBox content = new VBox(section);
                for (int i = 0; i < 40; i++) {
                    Region filler = new Region();
                    filler.setPrefHeight(50);
                    content.getChildren().add(filler);
                }
                ScrollPane sp = new ScrollPane(content);
                sp.setPrefViewportHeight(300);
                sp.setPrefViewportWidth(320);
                StackPane root = new StackPane(sp);
                new Scene(root, 320, 300);
                layout(root);

                Scroll.setStickyPosition(header, Side.TOP, 0, section);

                sp.setVvalue(sp.getVmax());
                layout(root);

                // Released at the section bottom but still displaced from flow (translateY != 0), which
                // is the shared 'stuck' rule (appear != natural) — so :stuck stays on through release.
                assertTrue(header.getTranslateY() > 0, "header is clamped away from its flow position");
                assertTrue(Scroll.isStuck(header), "still displaced => still stuck at release");
                assertTrue(header.getPseudoClassStates().contains(Scroll.STUCK_PSEUDO_CLASS));
            }
        });
    }

    // ---------------------------------------------------------------------
    // Teardown clears both channels; the property instance is stable across clear / re-apply
    // ---------------------------------------------------------------------

    @Test
    void teardownClearsBothChannelsAndPropertyIsStable() {
        FxTestSupport.onFx(() -> {
            try (MockedStatic<WebAPI> web = Mockito.mockStatic(WebAPI.class)) {
                web.when(WebAPI::isBrowser).thenReturn(false);

                Label header = new Label("HEADER");
                ScrollPane sp = scroller(header);
                StackPane root = new StackPane(sp);
                new Scene(root, 320, 300);
                layout(root);

                ReadOnlyBooleanProperty first = Scroll.stuckProperty(header);
                AtomicInteger flips = new AtomicInteger();
                first.addListener((obs, was, is) -> flips.incrementAndGet());

                Scroll.setStickyPosition(header, Side.TOP, 0);
                sp.setVvalue(sp.getVmax());
                layout(root);
                assertTrue(first.get(), "pinned before clear");
                assertTrue(header.getPseudoClassStates().contains(Scroll.STUCK_PSEUDO_CLASS));

                Scroll.clearScrollPosition(header);
                assertFalse(first.get(), "clearing drops stuck to false");
                assertFalse(header.getPseudoClassStates().contains(Scroll.STUCK_PSEUDO_CLASS),
                        "clearing removes :stuck");

                // Same property instance is handed back, and the listener attached once still fires.
                ReadOnlyBooleanProperty again = Scroll.stuckProperty(header);
                assertSame(first, again, "stuckProperty is stable across clear / re-apply");

                int before = flips.get();
                Scroll.setStickyPosition(header, Side.TOP, 0);
                sp.setVvalue(sp.getVmax());
                layout(root);
                assertTrue(again.get(), "re-applied sticky pins again");
                assertTrue(flips.get() > before, "the once-attached listener still sees transitions");
            }
        });
    }

    // ---------------------------------------------------------------------
    // Non-sticky nodes never read stuck: FIXED (always pinned => no info) and STATIC
    // ---------------------------------------------------------------------

    @Test
    void fixedAndStaticNodesReadFalse() {
        FxTestSupport.onFx(() -> {
            try (MockedStatic<WebAPI> web = Mockito.mockStatic(WebAPI.class)) {
                web.when(WebAPI::isBrowser).thenReturn(false);

                Label fixed = new Label("fixed");
                Label plain = new Label("plain");
                VBox parent = new VBox(fixed, plain);
                StackPane root = new StackPane(parent);
                new Scene(root, 400, 600);
                layout(root);

                Scroll.setFixedBar(fixed, Side.TOP);
                layout(root);
                assertFalse(Scroll.isStuck(fixed), "a fixed node is always pinned, so it carries no stuck state");
                assertFalse(fixed.getPseudoClassStates().contains(Scroll.STUCK_PSEUDO_CLASS),
                        ":stuck is STICKY-only");

                // An untouched node reports false and still yields a usable (false) property.
                assertFalse(Scroll.isStuck(plain));
                assertFalse(Scroll.stuckProperty(plain).get());
            }
        });
    }

    // ---------------------------------------------------------------------
    // The exposed pseudo-class constant is the canonical "stuck" JavaFX pseudo-class
    // ---------------------------------------------------------------------

    @Test
    void exposedPseudoClassIsTheStuckPseudoClass() {
        assertSame(javafx.css.PseudoClass.getPseudoClass("stuck"), Scroll.STUCK_PSEUDO_CLASS);
    }
}
