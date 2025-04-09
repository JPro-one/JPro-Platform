package one.jpro.platform.sipjs.example.page;

import com.jpro.webapi.WebAPI;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import one.jpro.platform.sipjs.example.component.User;
import javafx.scene.layout.VBox;
import static one.jpro.platform.sipjs.example.Variables.*;
import static one.jpro.platform.sipjs.example.Variables.sipAlice;

public class BobPage extends VBox {
    public BobPage() {
        // Add title
        var title = new Label("Bob");
        title.getStyleClass().add("title");
        getChildren().add(title);
        WebAPI.getWebAPI(this, webapi -> {
            setup(webapi);
        });
        getStyleClass().add("jpro-sipjs-example-page");
    }

    public void setup(WebAPI webapi) {
        var user1 = new User(webapi, server, sipBob, "Bob", sipAlice);

        var hbox = new HBox(user1);
        hbox.getStyleClass().add("alice-and-bob-hbox");

        getChildren().addAll(hbox);
    }
}
