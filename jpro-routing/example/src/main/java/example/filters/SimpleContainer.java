package example.filters;

import one.jpro.routing.filter.container.Container;
import one.jpro.routing.Request;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class SimpleContainer extends VBox implements Container {

    HBox top;

    public SimpleContainer() {
        getStyleClass().add("simple-container");
        top = new HBox();

        Label topText = new Label();
        // Bind the text of the request to the topText
        requestProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue!=null) {
                topText.textProperty().set(newValue.toString());
            } else {
                topText.setText("");
            }
        });
        top.getChildren().add(topText);

        updateChildren();
        contentProperty().addListener((observable, oldValue, newValue) -> updateChildren());

    }

    private void updateChildren() {
        getChildren().clear();
        getChildren().add(top);
        if(contentProperty().get()!=null) {
            VBox.setVgrow(contentProperty().get(), Priority.ALWAYS);
            getChildren().add(contentProperty().get());
        }
    }

    ObjectProperty<Node> _contentProperty = new SimpleObjectProperty<>();

    @Override
    public ObjectProperty<Node> contentProperty() {
        return _contentProperty;
    }

    ObjectProperty<Request> _requestProperty = new SimpleObjectProperty<>();

    @Override
    public ObjectProperty<Request> requestProperty() {
        return _requestProperty;
    }
}
