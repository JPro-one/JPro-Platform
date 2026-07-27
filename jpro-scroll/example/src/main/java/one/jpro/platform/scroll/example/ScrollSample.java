package one.jpro.platform.scroll.example;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import one.jpro.platform.scroll.Scroll;
import one.jpro.platform.scroll.ScrollPosition;

/**
 * The ScrollSample demonstrates scroll-aware positioning: a sticky header that stays
 * pinned to the top of the viewport while a tall column of content scrolls underneath it.
 * <p>
 * To use this class, create an instance and call the {@code start()} method.
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
     * Creates the root element for the application.
     * <p>
     * This method builds a header pinned with {@link ScrollPosition#STICKY} above a column
     * of 100 labels, so the header remains visible as the content scrolls.
     *
     * @return the root node of the application
     */
    public Parent createRoot() {
        final var header = new Label("Sticky header");
        header.setMinHeight(48);
        header.setMaxWidth(Double.MAX_VALUE);
        // A solid background so scrolled content slides underneath, not through, the pinned header.
        header.setStyle("-fx-background-color: #2b6cb0; -fx-text-fill: white; -fx-font-size: 18; -fx-padding: 12;");
        Scroll.setStickyPosition(header);

        final var content = new VBox();
        for (int i = 1; i <= 100; i++) {
            content.getChildren().add(new Label(String.format("%2d) The quick brown fox jumps over the lazy dog.", i)));
        }

        final var root = new VBox(header, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        ((Region) root).setPrefSize(400, 600);
        return root;
    }
}
