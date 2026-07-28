package one.jpro.platform.sticky.example;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
 * and there is a normal in-flow button below the header. They verify picking — the invisible half of
 * the mechanism: clicks must land on a reparented, server-pinned element while it is pinned, and must
 * pass <em>through</em> the mouse-transparent overlay to the content underneath.
 *
 * @author Tobias Horak
 */
public class ScrollSample extends Application {

    @Override
    public void start(Stage primaryStage) {
        Scene scene = new Scene(createRoot(), 400, 600);
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
        final var header = barButton("Sticky page header", "#2b6cb0");
        header.setMinHeight(48);
        Scroll.setStickyPosition(header);
        root.getChildren().add(header);

        // A normal in-flow button just below the header: clicking it confirms clicks pass through the
        // mouse-transparent overlay to ordinary content, and that pinned elements do not swallow them.
        final var flowButton = countButton("Flow button (not pinned)");
        root.getChildren().add(flowButton);

        root.getChildren().addAll(filler(1, 25));

        // Bounded section: the sub-header pins while the section scrolls through, then releases at the
        // section's bottom (its containing block). No explicit container needed — the parent bounds it.
        final var section = new VBox();
        final var sectionHeader = barButton("Section sub-header (pins, then releases)", "#2f855a");
        sectionHeader.setMinHeight(40);
        // Pin 48px down so it stacks below the 48px-tall page header, not on top of it.
        Scroll.setStickyPosition(sectionHeader, javafx.geometry.Side.TOP, 48);
        section.getChildren().add(sectionHeader);
        section.getChildren().addAll(filler(26, 65));
        root.getChildren().add(section);

        // Enough trailing content that the section fully scrolls past the viewport, so the section
        // sub-header visibly releases (a short page would run out of scroll while it is still pinned).
        root.getChildren().addAll(filler(66, 160));

        // Fixed full-width bottom bar (a button; stretch horizontal + pin bottom).
        final var bottomBar = barButton("Fixed bottom bar", "#c05621");
        bottomBar.setMinHeight(40);
        Scroll.setFixedBar(bottomBar, javafx.geometry.Side.BOTTOM);
        root.getChildren().add(bottomBar);

        // Fixed bottom-right FAB (a button, corner-anchored), floated above the bottom bar.
        final var fab = new Button("0");
        fab.setStyle("-fx-background-color: #6b46c1; -fx-text-fill: white; -fx-font-size: 20; "
                + "-fx-min-width: 56; -fx-min-height: 56; -fx-background-radius: 28;");
        final int[] fabClicks = {0};
        fab.setOnAction(e -> fab.setText(String.valueOf(++fabClicks[0])));
        Scroll.setFixedPosition(fab, ScrollAnchor.of().bottom(64).right(24));
        root.getChildren().add(fab);

        // Fixed top-centered toast, dropped below the page header.
        final var toast = chip("Fixed toast — top center");
        toast.setStyle("-fx-background-color: #1a202c; -fx-text-fill: white; -fx-font-size: 13; "
                + "-fx-padding: 8 16; -fx-background-radius: 16;");
        Scroll.setFixedPosition(toast, Pos.TOP_CENTER, 64);
        root.getChildren().add(toast);

        // Fixed full-viewport frame (stretch both axes). Mouse-transparent + translucent so it frames
        // the viewport without blocking the other demos (and so clicks reach the content beneath it).
        final var overlay = new Label("full-viewport overlay (stretch both)");
        overlay.setAlignment(Pos.TOP_CENTER);
        overlay.setStyle("-fx-border-color: rgba(229,62,62,0.6); -fx-border-width: 3; "
                + "-fx-text-fill: rgba(229,62,62,0.7); -fx-padding: 8; -fx-font-size: 12;");
        overlay.setMouseTransparent(true);
        Scroll.setFixedFullscreen(overlay);
        root.getChildren().add(overlay);

        VBox.setVgrow(root, Priority.ALWAYS);
        return root;
    }

    /** A full-width coloured bar {@link Button} whose label counts the clicks it receives. */
    private static Button barButton(String text, String colour) {
        final var button = new Button(text + "  —  clicks: 0");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setStyle("-fx-background-color: " + colour + "; -fx-text-fill: white; -fx-font-size: 16; "
                + "-fx-padding: 10; -fx-background-radius: 0;");
        final int[] clicks = {0};
        button.setOnAction(e -> button.setText(text + "  —  clicks: " + (++clicks[0])));
        return button;
    }

    /** A natural-width {@link Button} whose label counts the clicks it receives. */
    private static Button countButton(String text) {
        final var button = new Button(text + "  —  clicks: 0");
        final int[] clicks = {0};
        button.setOnAction(e -> button.setText(text + "  —  clicks: " + (++clicks[0])));
        return button;
    }

    /** A natural-width chip label (used for the toast). */
    private static Label chip(String text) {
        return new Label(text);
    }

    /** A run of numbered content lines [from, to]. */
    private static VBox filler(int from, int to) {
        final var box = new VBox();
        for (int i = from; i <= to; i++) {
            box.getChildren().add(new Label(String.format("%3d) The quick brown fox jumps over the lazy dog.", i)));
        }
        return box;
    }
}
