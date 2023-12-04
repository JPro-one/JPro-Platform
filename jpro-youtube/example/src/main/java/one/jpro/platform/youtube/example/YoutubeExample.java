package one.jpro.platform.youtube.example;

import javafx.application.Application;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import one.jpro.platform.youtube.YoutubeNode;

public class YoutubeExample extends Application {

    @Override
    public void start(javafx.stage.Stage stage) throws Exception {
        var pin = new VBox();

        pin.getChildren().add(new StackPane(new YoutubeNode("dQw4w9WgXcQ")));
        pin.getChildren().add(new StackPane(new YoutubeNode("oqAsIcoN9MY")));
        stage.setScene(new javafx.scene.Scene(new VBox(pin)));

        stage.show();
    }
}
