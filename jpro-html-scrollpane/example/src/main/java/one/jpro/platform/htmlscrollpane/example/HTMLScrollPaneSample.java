package one.jpro.platform.htmlscrollpane.example;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import one.jpro.platform.htmlscrollpane.HTMLScrollPaneSkin;

/**
 * The HTMLScrollPaneSample class is an application that creates a JavaFX scrollable pane
 * with HTML-like formatting for the content.
 * <p>
 * To use this class, create an instance of the class and call the start() method.
 *
 * @author Florian Kirmaier
 * @author Besmir Beqiri
 */
public class HTMLScrollPaneSample extends Application {

        @Override
        public void start(Stage primaryStage) {
            Scene scene = new Scene(createRoot());
            primaryStage.setScene(scene);
            primaryStage.show();
        }

        /**
         * Creates the root element for the application.
         * <p>
         * This method creates a ScrollPane with a VBox containing 100 labels. Each label displays a numbered sentence.
         * The font size of each label increases with the index. The scroll pane is then returned as the root element.
         *
         * @return The root node of the application.
         */
        public Parent createRoot() {
            final var content = new VBox();
            for (int i = 1; i <= 100; i++) {
                // add a label
                final var label = new Label(String.format("%2d) The quick brown fox jumps over the lazy dog.", i));
                label.setFont(Font.font("Arial", 12 + i));
                content.getChildren().add(label);
            }
            final var scrollPane = new ScrollPane();
            scrollPane.setContent(content);
            scrollPane.setSkin(new HTMLScrollPaneSkin(scrollPane));

            return scrollPane;
        }
}
