package one.jpro.platform.sipjs.example.page;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import one.jpro.platform.routing.LinkUtil;

public class SelectPage extends VBox {

    public SelectPage() {
        var button1 = new Button("Echo");
        var button2 = new Button("Auto");
        var button3 = new Button("Alice and Bob");
        var button4 = new Button("Alice");
        var button5 = new Button("Bob");
        button1.getStyleClass().add("select-button");
        button2.getStyleClass().add("select-button");
        button3.getStyleClass().add("select-button");
        button4.getStyleClass().add("select-button");
        button5.getStyleClass().add("select-button");
        LinkUtil.setLink(button1, "/echo");
        LinkUtil.setLink(button2, "/auto");
        LinkUtil.setLink(button3, "/aliceAndBob");
        LinkUtil.setLink(button4, "/alice");
        LinkUtil.setLink(button5, "/bob");

        var hbox = new HBox();
        hbox.getStyleClass().add("select-page-hbox");
        hbox.setAlignment(javafx.geometry.Pos.CENTER);
        hbox.getChildren().addAll(button1, button2, button3, button4, button5);

        var title = new Label("Select a demo");
        title.getStyleClass().add("select-page-title");

        getChildren().add(title);
        getChildren().add(hbox);
        getStyleClass().add("jpro-sipjs-example-page");
    }

}
