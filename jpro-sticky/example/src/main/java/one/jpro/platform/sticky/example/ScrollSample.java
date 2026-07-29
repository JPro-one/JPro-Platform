package one.jpro.platform.sticky.example;

import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.Styles;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import one.jpro.platform.sticky.Scroll;
import one.jpro.platform.sticky.ScrollAnchor;
import one.jpro.platform.sticky.ScrollPosition;

/**
 * The ScrollSample demonstrates scroll-aware positioning across the full {@link Scroll} surface:
 * <ul>
 *   <li>a {@link ScrollPosition#STICKY sticky} page header that stays pinned throughout the scroll
 *       (bounded by the page-spanning root, so effectively document-long);</li>
 *   <li>a bounded section whose sticky sub-header pins while the section is in view and then
 *       <em>releases</em> at the section's end (CSS-native containment);</li>
 *   <li>{@link ScrollPosition#FIXED fixed} elements at every anchor kind: a full-width bottom bar
 *       (stretch), a bottom-right FAB (corner), a top-centered toast (center), and a translucent
 *       full-viewport frame (stretch on both axes).</li>
 * </ul>
 * The pinned header, the section sub-header, and the FAB are <strong>buttons with click counters</strong>,
 * and there is a normal in-flow button below the header. They verify picking, the invisible half of
 * the mechanism: clicks must land on a reparented, server-pinned element while it is pinned, and must
 * pass <em>through</em> the mouse-transparent overlay to the content underneath.
 * <p>
 * Styling uses the AtlantaFX {@link CupertinoLight} theme (matching the other platform examples), so
 * the demo reads as one system without any hand-rolled colours.
 *
 * @author Tobias Horak
 */
public class ScrollSample extends Application {

    @Override
    public void start(Stage primaryStage) {
        Scene scene = new Scene(createRoot(), 400, 600);
        scene.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Builds the demo scene: the sticky page header, a bounded sticky section, and the four fixed
     * anchor demos, over a tall column of content so the page scrolls natively.
     *
     * @return the root node of the application
     */
    public Parent createRoot() {
        final var root = new VBox();
        ((Region) root).setPrefSize(400, 600);

        // Sticky page header (a button, to prove clicks land on the pinned element): its parent is the
        // page-spanning root, so it stays pinned document-long.
        final var header = barButton("Sticky page header", Styles.ACCENT);
        header.setMinHeight(48);
        Scroll.setStickyPosition(header);
        root.getChildren().add(header);

        // A normal in-flow button just below the header: clicking it confirms clicks pass through the
        // mouse-transparent overlay to ordinary content, and that pinned elements do not swallow them.
        final var flowButton = countButton("Flow button (not pinned)");
        VBox.setMargin(flowButton, new Insets(12, 16, 4, 16));
        root.getChildren().add(flowButton);

        root.getChildren().addAll(filler(1, 25));

        // Bounded section: the sub-header pins while the section scrolls through, then releases at the
        // section's bottom (its containing block). No explicit container needed — the parent bounds it.
        final var section = new VBox();
        final var sectionHeader = barButton("Section sub-header (pins, then releases)", Styles.SUCCESS);
        sectionHeader.setMinHeight(40);
        // Pin 48px down so it stacks below the 48px-tall page header, not on top of it.
        Scroll.setStickyPosition(sectionHeader, Side.TOP, 48);
        section.getChildren().add(sectionHeader);
        section.getChildren().addAll(filler(26, 65));
        root.getChildren().add(section);

        // Enough trailing content that the section fully scrolls past the viewport, so the section
        // sub-header visibly releases (a short page would run out of scroll while it is still pinned).
        root.getChildren().addAll(filler(66, 160));

        // Fixed bottom bar, floated 5px off the left, right, and bottom edges: stretch horizontal (with
        // insets) + pin above the bottom. Uses the canonical anchor since setFixedBar only insets the
        // pinned edge. AtlantaFX has no .button.warning accent, so the amber comes from the theme colour.
        final var bottomBar = barButton("Fixed bottom bar", null);
        bottomBar.setStyle("-fx-background-radius: 8; -fx-background-color: -color-warning-emphasis; "
                + "-fx-text-fill: -color-fg-emphasis;");
        bottomBar.setMinHeight(40);
        Scroll.setFixedPosition(bottomBar, ScrollAnchor.of().left(5).right(5).bottom(5));
        root.getChildren().add(bottomBar);

        // Fixed bottom-right FAB (a button, corner-anchored), floated above the bottom bar. Rounded via
        // an explicit radius rather than Styles.BUTTON_CIRCLE, which is icon-only and hides the counter.
        final var fab = new Button("0");
        fab.getStyleClass().add(Styles.ACCENT);
        fab.setMinSize(56, 56);
        fab.setStyle("-fx-background-radius: 28; -fx-font-size: 18;");
        final int[] fabClicks = {0};
        fab.setOnAction(e -> fab.setText(String.valueOf(++fabClicks[0])));
        Scroll.setFixedPosition(fab, ScrollAnchor.of().bottom(64).right(24));
        root.getChildren().add(fab);

        // Fixed top-centered toast, dropped below the page header. Themed colours (fg on bg) keep it
        // legible in either light or dark AtlantaFX theme.
        final var toast = new Label("Fixed toast (top center)");
        toast.setStyle("-fx-background-color: -color-fg-default; -fx-text-fill: -color-bg-default; "
                + "-fx-padding: 8 16; -fx-background-radius: 16; -fx-font-size: 13;");
        Scroll.setFixedPosition(toast, Pos.TOP_CENTER, 64);
        root.getChildren().add(toast);

        // Fixed full-viewport frame (stretch both axes). Mouse-transparent + translucent so it frames
        // the viewport without blocking the other demos (and so clicks reach the content beneath it).
        // The label sits at viewport centre so it stays legible instead of landing on the sticky bars.
        final var overlay = new Label("full-viewport overlay (stretch both)");
        overlay.setAlignment(Pos.CENTER);
        overlay.setStyle("-fx-border-color: -color-danger-emphasis; -fx-border-width: 3; "
                + "-fx-text-fill: -color-danger-emphasis; -fx-padding: 8; -fx-font-size: 12;");
        overlay.setMouseTransparent(true);
        Scroll.setFixedFullscreen(overlay);
        root.getChildren().add(overlay);

        VBox.setVgrow(root, Priority.ALWAYS);
        return root;
    }

    /**
     * A full-width bar {@link Button} whose label counts its clicks. The optional AtlantaFX severity
     * style class fills it (pass {@code null} to fill it via an inline theme colour instead).
     */
    private static Button barButton(String text, String severityStyleClass) {
        final var button = new Button(text + "  (clicks: 0)");
        if (severityStyleClass != null) {
            button.getStyleClass().add(severityStyleClass);
        }
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        // Square the corners so the bar reads as full-bleed and content slides cleanly under it.
        button.setStyle("-fx-background-radius: 0;");
        final int[] clicks = {0};
        button.setOnAction(e -> button.setText(text + "  (clicks: " + (++clicks[0]) + ")"));
        return button;
    }

    /** A natural-width outlined {@link Button} whose label counts the clicks it receives. */
    private static Button countButton(String text) {
        final var button = new Button(text + "  (clicks: 0)");
        button.getStyleClass().add(Styles.BUTTON_OUTLINED);
        final int[] clicks = {0};
        button.setOnAction(e -> button.setText(text + "  (clicks: " + (++clicks[0]) + ")"));
        return button;
    }

    /** A few pangrams so the rows read as varied data instead of one repeated sentence. */
    private static final String[] DESCRIPTIONS = {
            "The quick brown fox jumps over the lazy dog.",
            "Sphinx of black quartz, judge my vow.",
            "Pack my box with five dozen liquor jugs.",
            "How vexingly quick daft zebras jump.",
    };

    /**
     * A run of table-style content rows [from, to] so the page has realistic tabular content to
     * scroll under the sticky bars, rather than a wall of cramped text.
     */
    private static VBox filler(int from, int to) {
        final var box = new VBox();
        for (int i = from; i <= to; i++) {
            box.getChildren().add(row(i));
        }
        return box;
    }

    /** One striped table row: a muted index cell, a description that grows, and a status pill. */
    private static HBox row(int index) {
        final var idx = new Label(String.format("%3d", index));
        idx.setMinWidth(40);
        idx.getStyleClass().add(Styles.TEXT_MUTED);

        final var desc = new Label(DESCRIPTIONS[index % DESCRIPTIONS.length]);
        desc.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(desc, Priority.ALWAYS);

        final var row = new HBox(12, idx, desc, statusPill(index));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(9, 16, 9, 16));
        // Zebra striping plus a hairline separator, both from theme colours.
        final var stripe = (index % 2 == 0) ? " -fx-background-color: -color-bg-subtle;" : "";
        row.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 0 0 1 0;" + stripe);
        return row;
    }

    /** A small themed status pill whose label rotates so the rows are not all identical. */
    private static Label statusPill(int index) {
        final boolean active = (index % 3 == 0);
        final var pill = new Label(active ? "active" : "idle");
        pill.setStyle("-fx-text-fill: " + (active ? "-color-success-fg" : "-color-fg-muted") + "; "
                + "-fx-background-color: " + (active ? "-color-success-subtle" : "-color-bg-inset") + "; "
                + "-fx-padding: 2 10; -fx-background-radius: 10; -fx-font-size: 11;");
        return pill;
    }
}
