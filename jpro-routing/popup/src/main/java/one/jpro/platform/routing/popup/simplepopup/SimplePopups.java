package one.jpro.platform.routing.popup.simplepopup;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import one.jpro.platform.routing.popup.PopupAPI;

import java.util.List;

/**
 * Utility class for creating simple popups in a JPro application.
 *
 * @author Florian Kirmaier
 */
public class SimplePopups {

    /**
     * Creates an informational popup with a specified title and text content.
     * The popup includes a close button that, when clicked, will close the popup.
     *
     * @param title    the title of the popup
     * @param infoText the text content of the popup
     * @return a {@link SimplePopup} object configured with the title, text content, and a close button
     */
    public static SimplePopup infoPopup(String title, String infoText) {
        Button closeButton = createCloseButton();
        return new SimplePopup(title, createText(infoText), List.of(closeButton), true);
    }

    /**
     * Creates a text node for displaying in the popup.
     * The text is styled with a CSS class for consistent appearance.
     *
     * @param text the text to be displayed
     * @return a {@link Node} containing the text, styled appropriately
     */
    public static Node createText(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("simple-popup-text");
        return label;
    }

    /**
     * Creates a button for closing the popup.
     * The button is configured with an action to close the popup when clicked.
     *
     * @return a {@code Button} configured to close the popup when clicked
     */
    public static Button createCloseButton() {
        Button closeButton = new Button("Close");
        closeButton.setOnAction(event -> PopupAPI.closePopup(closeButton));
        return closeButton;
    }
}
