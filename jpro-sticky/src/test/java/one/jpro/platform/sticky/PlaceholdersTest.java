package one.jpro.platform.sticky;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Placeholders} — the layout fidelity a pinned node's flow slot keeps once the
 * node is reparented into the {@link StickyOverlay}. They assert the standard-pane constraint keys and
 * the horizontal footprint transfer to the placeholder, and that the vertical footprint is left alone
 * (so the FIXED path can still collapse the slot to zero height).
 *
 * @author Tobias Horak
 */
class PlaceholdersTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FxTestSupport.startToolkit();
    }

    @Test
    void mirrorCopiesHBoxConstraintsAndReservesWidth() {
        FxTestSupport.onFx(() -> {
            Region node = new Region();
            node.resize(240, 60);
            HBox.setHgrow(node, Priority.ALWAYS);
            HBox.setMargin(node, new Insets(4, 8, 4, 8));

            Region placeholder = new Region();
            placeholder.setMaxWidth(Double.MAX_VALUE);
            Placeholders.mirror(node, placeholder);

            assertEquals(Priority.ALWAYS, HBox.getHgrow(placeholder), "hgrow must transfer to the slot");
            assertEquals(new Insets(4, 8, 4, 8), HBox.getMargin(placeholder), "margin must transfer");
            assertEquals(240, placeholder.getPrefWidth(), 0.001, "the slot must reserve the node width");
            assertEquals(Double.MAX_VALUE, placeholder.getMaxWidth(), 0.001, "maxWidth stays fill");
        });
    }

    @Test
    void mirrorCopiesGridPaneAndVBoxConstraints() {
        FxTestSupport.onFx(() -> {
            Region node = new Region();
            node.resize(100, 40);
            GridPane.setColumnSpan(node, 3);
            GridPane.setRowIndex(node, 2);
            VBox.setVgrow(node, Priority.SOMETIMES);

            Region placeholder = new Region();
            Placeholders.mirror(node, placeholder);

            assertEquals(Integer.valueOf(3), GridPane.getColumnSpan(placeholder));
            assertEquals(Integer.valueOf(2), GridPane.getRowIndex(placeholder));
            assertEquals(Priority.SOMETIMES, VBox.getVgrow(placeholder));
        });
    }

    @Test
    void mirrorCarriesAnExplicitMinWidthAsAFloor() {
        FxTestSupport.onFx(() -> {
            Region node = new Region();
            node.resize(300, 50);
            node.setMinWidth(120);

            Region placeholder = new Region();
            Placeholders.mirror(node, placeholder);

            assertEquals(120, placeholder.getMinWidth(), 0.001, "an explicit node min-width is a real floor");
        });
    }

    @Test
    void mirrorDoesNotConstrainHeightSoFixedCanCollapse() {
        FxTestSupport.onFx(() -> {
            Region node = new Region();
            node.resize(240, 60);

            Region placeholder = new Region();
            Placeholders.mirror(node, placeholder);

            // Height is the caller's concern (sync sets prefHeight; FIXED collapses to 0). Mirror must
            // leave both height dimensions at their computed defaults so it can't block that collapse.
            assertEquals(Region.USE_COMPUTED_SIZE, placeholder.getPrefHeight(), 0.001);
            assertEquals(Region.USE_COMPUTED_SIZE, placeholder.getMinHeight(), 0.001);
        });
    }

    @Test
    void mirrorIgnoresNonConstraintProperties() {
        FxTestSupport.onFx(() -> {
            Label node = new Label();
            node.resize(80, 20);
            node.getProperties().put("app-custom-key", "value");
            node.getProperties().put(new Object(), "opaque");

            Region placeholder = new Region();
            Placeholders.mirror(node, placeholder);

            assertFalse(placeholder.getProperties().containsKey("app-custom-key"),
                    "only standard layout-pane constraint keys may transfer");
            assertTrue(placeholder.getProperties().keySet().stream()
                            .noneMatch(k -> k instanceof String && !((String) k).contains("-")),
                    "no stray non-prefixed keys should have been copied");
        });
    }
}
