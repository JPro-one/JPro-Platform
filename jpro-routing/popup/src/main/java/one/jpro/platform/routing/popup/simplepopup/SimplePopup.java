package one.jpro.platform.routing.popup.simplepopup;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import one.jpro.platform.routing.popup.PopupAPI;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

/**
 * Represents a simple popup window with customizable content and buttons.
 * This class extends {@link StackPane} to provide a flexible layout for displaying popups.
 *
 * @author Florian Kirmaier
 */
public class SimplePopup extends StackPane {

    /**
     * Constructs a {@code SimplePopup} with specified title, content, buttons and closability.
     *
     * @param title    the title of the popup
     * @param content  the main content of the popup as a {@link Node}
     * @param buttons  a list of {@link Button}s to be displayed at the bottom of the popup
     * @param closable a boolean indicating if the popup can be closed by the user
     */
    public SimplePopup(String title, Node content, List<Button> buttons, boolean closable) {
        getStylesheets().add(getClass()
                .getResource("/one/jpro/platform/routing/popup/simplepopup/simplepopup.css").toExternalForm());
        getStyleClass().add("simple-popup-background");
        VBox popupBox = new VBox();
        getChildren().add(popupBox);

        popupBox.setMaxWidth(Region.USE_PREF_SIZE);
        popupBox.setMaxHeight(Region.USE_PREF_SIZE);
        popupBox.getStyleClass().add("simple-popup-content");

        Node topArea = createTopArea(title, closable);
        popupBox.getChildren().add(topArea);
        popupBox.getChildren().add(createContentArea(content));

        Node buttonArea = createButtonArea(buttons);
        popupBox.getChildren().add(buttonArea);
    }

    /**
     * Creates the top area of the popup, including the title and optional close button.
     *
     * @param title    the title of the popup
     * @param closable indicates if a close button should be included
     * @return a {@link Node} representing the top area of the popup
     */
    public Node createTopArea(String title, boolean closable) {
        StackPane topArea = new StackPane();
        topArea.getStyleClass().add("simple-popup-top-area");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("simple-popup-title");
        topArea.getChildren().add(titleLabel);

        if (closable) {
            Button closeButton = new Button();
            closeButton.getStyleClass().add("simple-popup-close-button");
            // Set ikonli close icon
            FontIcon closeIcon = new FontIcon("eva-close");
            closeButton.setGraphic(closeIcon);
            StackPane.setAlignment(closeButton, Pos.CENTER_RIGHT);
            closeButton.setOnAction(e -> PopupAPI.closePopup(this));
            topArea.getChildren().add(closeButton);
        }

        return topArea;
    }

    /**
     * Creates the content area of the popup.
     *
     * @param content the main content of the popup as a {@link Node}
     * @return a {@link Node} representing the content area of the popup
     */
    public Node createContentArea(Node content) {
        StackPane contentArea = new StackPane();
        VBox.setVgrow(contentArea, Priority.ALWAYS);
        contentArea.getStyleClass().add("simple-popup-content-area");
        contentArea.getChildren().add(content);
        return contentArea;
    }

    /**
     * Creates the button area of the popup.
     *
     * @param buttons a list of {@link Button}s to be displayed at the bottom of the popup
     * @return a {@link Node} representing the button area of the popup
     */
    public Node createButtonArea(List<Button> buttons) {
        HBox buttonArea = new HBox();
        buttonArea.getStyleClass().add("simple-popup-button-area");
        buttonArea.getChildren().addAll(buttons);

        return buttonArea;
    }
}
