package one.jpro.routing.popup.simplepopup;


import one.jpro.routing.popup.PopupAPI;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class SimplePopup extends StackPane {

    VBox popupBox = new VBox();

    public SimplePopup(String title, Node content, List<Button> buttons, boolean closable) {
        getStylesheets().add(getCSSFile());
        getStyleClass().add("simple-popup-background");
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

    public String getCSSFile() {
        return "/one/jpro/routing/popup/simplepopup/simplepopup.css";
    }

    public Node createTopArea(String title, boolean closable) {
        StackPane topArea = new StackPane();
        topArea.getStyleClass().add("simple-popup-top-area");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("simple-popup-title");
        topArea.getChildren().add(titleLabel);

        if(closable) {
            Button closeButton = new Button();
            closeButton.getStyleClass().add("simple-popup-close-button");
            // Set ikonli close icon
            FontIcon closeIcon = new FontIcon("eva-close");
            closeButton.setGraphic(closeIcon);
            StackPane.setAlignment(closeButton, Pos.CENTER_RIGHT);
            closeButton.setOnAction(e -> {
                PopupAPI.closePopup(this);
            });
            topArea.getChildren().add(closeButton);
        }

        return topArea;
    }

    public Node createContentArea(Node content) {
        StackPane contentArea = new StackPane();
        VBox.setVgrow(contentArea, Priority.ALWAYS);
        contentArea.getStyleClass().add("simple-popup-content-area");
        contentArea.getChildren().add(content);
        return contentArea;
    }

    public Node createButtonArea(List<Button> buttons) {
        HBox buttonArea = new HBox();
        buttonArea.getStyleClass().add("simple-popup-button-area");
        buttonArea.getChildren().addAll(buttons);

        return buttonArea;
    }
}
