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
import javafx.scene.control.ScrollPane;
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
 *   <li>a {@link javafx.scene.control.ScrollPane} section whose sub-header pins to the viewport top
 *       and then releases. This is the sticky path that runs on the <em>desktop</em> (and in the browser
 *       when the scroll is a server-side FX ScrollPane), so desktop and web share one code path;</li>
 *   <li>{@link ScrollPosition#FIXED fixed} elements at every anchor kind: a full-width bottom bar
 *       (stretch), a bottom-right FAB (corner), a top-centered toast (center), and a translucent
 *       full-viewport frame (stretch on both axes).</li>
 * </ul>
 * The pinned header, the section sub-header, and the FAB are <strong>buttons with click counters</strong>,
 * and there is a normal in-flow button below the header. They verify picking, the invisible half of
 * the mechanism: clicks must land on a reparented, server-pinned element while it is pinned, and must
 * pass <em>through</em> the mouse-transparent overlay to the content underneath.
 * <p>
 * <strong>Observability.</strong> The sticky page header and the ScrollPane sub-header carry the
 * {@code sticky-demo-header} style class, and {@code sticky-sample.css} gives them a drop-shadow via
 * the auto-toggled {@link Scroll#STUCK_PSEUDO_CLASS :stuck} pseudo-class while they are pinned (the
 * CSS channel, styled with no Java beyond the sticky call). The page header additionally logs its
 * {@link Scroll#stuckProperty} transitions (the Java channel). Both flip together from one source.
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
        // The observability CSS channel: sticky-sample.css restyles a pinned header via the :stuck
        // pseudo-class alone (see the .sticky-demo-header rule). No Java beyond the sticky call.
        scene.getStylesheets().add(ScrollSample.class.getResource("sticky-sample.css").toExternalForm());
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
        // Observability: the style class + sticky-sample.css give it a drop-shadow via the :stuck
        // pseudo-class while it is pinned (the CSS channel), and stuckProperty() feeds the Java channel.
        header.getStyleClass().add("sticky-demo-header");
        Scroll.setStickyPosition(header);
        Scroll.stuckProperty(header).addListener((obs, was, is) ->
                System.out.println("[jpro-sticky] page header stuck=" + is));
        root.getChildren().add(header);

        // A normal in-flow button just below the header: clicking it confirms clicks pass through the
        // mouse-transparent overlay to ordinary content, and that pinned elements do not swallow them.
        final var flowButton = countButton("Flow button (not pinned)");
        VBox.setMargin(flowButton, new Insets(12, 16, 4, 16));
        root.getChildren().add(flowButton);

        // A JavaFX ScrollPane section, wrapped in a titled card so it reads as one contained panel. This
        // is the one demo that exercises STICKY on the DESKTOP: desktop content only scrolls through a
        // ScrollPane, so that is where sticky lives (ScrollPaneStickyImpl). The same code runs in the browser too,
        // because there the scroll is a server-driven FX ScrollPane. One code path, both platforms. Placed
        // high so it is visible in the desktop window.
        final var scrollSection = scrollPaneSection();
        VBox.setMargin(scrollSection, new Insets(12, 16, 8, 16));
        root.getChildren().add(scrollSection);

        root.getChildren().addAll(filler(1, 25));

        // Bounded section: the sub-header pins while the section scrolls through, then releases at the
        // section's bottom (its containing block). No explicit container needed; the parent bounds it.
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

        // Fixed bottom bar, floated 5px off the left, right, and bottom edges: a bar is just a stretch
        // anchor (pin one edge, stretch the perpendicular axis), here with a 5px inset on all three.
        // AtlantaFX has no .button.warning accent, so the amber comes from the theme colour.
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
     * A {@link ScrollPane} section whose sub-header pins to the viewport top while its sub-section
     * scrolls through, then <em>releases</em> at the sub-section's end (the containing block). This is
     * the {@link one.jpro.platform.sticky.ScrollPosition#STICKY STICKY} path that runs on the desktop:
     * on desktop, content scrolls only through a ScrollPane, and in the browser the very same code runs
     * because the scroll is a server-side FX ScrollPane. The sub-header is a click-counting button, so
     * it also proves picking lands while pinned.
     *
     * @return a titled card wrapping a ScrollPane with a bounded sticky sub-header over tall content
     */
    private static VBox scrollPaneSection() {
        final var content = new VBox();

        // A short lead-in so the sub-header starts unpinned and visibly pins as you scroll into it.
        content.getChildren().addAll(filler(1, 4));

        // The bounded sub-section: its sub-header pins at the viewport top, then releases at the bottom.
        final var sub = new VBox();
        final var subHeader = barButton("ScrollPane sticky header (pins, then releases)", Styles.ACCENT);
        subHeader.setMinHeight(40);
        // Same :stuck restyle as the page header, but on the ScrollPaneStickyImpl path, so the CSS channel is
        // verifiable on the desktop too (the ScrollPane is the desktop scroll surface).
        subHeader.getStyleClass().add("sticky-demo-header");
        sub.getChildren().add(subHeader);
        sub.getChildren().addAll(filler(5, 24));
        content.getChildren().add(sub);

        // Trailing content so the sub-section can scroll fully past and the release is visible.
        content.getChildren().addAll(filler(25, 32));

        final var scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(240);
        scrollPane.setMinHeight(240);
        // Let the card border be the only frame: drop the ScrollPane's own border and background.
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        // Pin the sub-header to the viewport top, bounded by its parent sub-section (the default block).
        Scroll.setStickyPosition(subHeader);

        // Wrap in a titled, bordered card so it is obvious where the ScrollPane begins and ends (its rows
        // would otherwise blend into the page content below). Theme colours only, no new dependencies.
        final var title = new Label("JavaFX ScrollPane: Sticky works on desktop in addition to web.");
        title.getStyleClass().add(Styles.TEXT_MUTED);
        title.setMaxWidth(Double.MAX_VALUE);
        title.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 7 7 0 0; "
                + "-fx-padding: 8 12; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 1 0;");

        final var card = new VBox(title, scrollPane);
        card.setStyle("-fx-border-color: -color-border-default; -fx-border-width: 1; "
                + "-fx-border-radius: 8; -fx-background-radius: 8;");
        return card;
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
