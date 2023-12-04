package one.jpro.platform.webrtc.example.videoroom.page;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import one.jpro.platform.routing.LinkUtil;

public class OverviewPage extends VBox {

    public OverviewPage() {
        getStyleClass().add("page");
        getStyleClass().add("overview-page");
        var overview = new Label("Overview");
        overview.getStyleClass().add("title");
        getChildren().add(overview);

        int randomId = (int) (Math.random() * 1000);
        var button = new Button("Create Room");
        LinkUtil.setLink(button, "/room/" + randomId);
        getChildren().add(button);
    }
}
