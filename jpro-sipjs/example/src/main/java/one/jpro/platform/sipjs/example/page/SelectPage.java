package one.jpro.platform.sipjs.example.page;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import one.jpro.platform.routing.LinkUtil;

public class SelectPage extends VBox {

    public SelectPage() {
        var button1 = new Button("Echo");
        var button2 = new Button("Alice and Bob");
        button1.getStyleClass().add("select-button");
        button2.getStyleClass().add("select-button");
        LinkUtil.setLink(button1, "/echo");
        LinkUtil.setLink(button2, "/aliceAndBob");

        var hbox = new HBox();
        hbox.getStyleClass().add("select-page-hbox");
        hbox.setAlignment(javafx.geometry.Pos.CENTER);
        hbox.getChildren().addAll(button1, button2);

        var title = new Label("Select a demo");
        title.getStyleClass().add("select-page-title");

        getChildren().add(title);
        getChildren().add(hbox);
        getStyleClass().add("jpro-sipjs-example-page");
    }

}
