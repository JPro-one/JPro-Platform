package one.jpro.routing.popup.simplepopup;

import one.jpro.routing.popup.PopupAPI;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.util.Arrays;

/**
 * This class contains various static methods to create SimplePopup.
 */
public class SimplePopups {

    public static SimplePopup infoPopup(String title, String infoText) {

        Button closeButton = createCloseButton();

        SimplePopup popup = new SimplePopup(title, createText(infoText), Arrays.asList(closeButton), true);

        return popup;
    }

    public static Node createText(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("simple-popup-text");
        return label;
    }

    public static Button createCloseButton() {
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> {
            PopupAPI.closePopup(closeButton);
        });
        return closeButton;
    }
}
