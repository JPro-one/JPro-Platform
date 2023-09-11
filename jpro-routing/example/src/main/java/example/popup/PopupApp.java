package example.popup;

import one.jpro.routing.Route;
import one.jpro.routing.RouteApp;
import one.jpro.routing.dev.DevFilter;
import one.jpro.routing.popup.PopupAPI;
import one.jpro.routing.popup.simplepopup.SimplePopup;
import one.jpro.routing.popup.simplepopup.SimplePopups;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import simplefx.experimental.parts.FXFuture;

import static one.jpro.routing.RouteUtils.*;

public class PopupApp extends RouteApp {

    public static void main(String[] args) {
        launch(args);
    }


    public Route createRoute() {
        return Route.empty()
                .and(redirect("/", "/popup"))
                .and(getNode("/popup", (r) -> popupSampleButtons()))
                .filter(DevFilter.create());
    }

    public Node popupSampleButtons() {
        VBox result = new VBox();

        Button button1 = new Button("Show Popup1");

        Button showLoading = new Button("Show Loading");


        button1.setOnAction(e -> {
            SimplePopup popup = SimplePopups.infoPopup("Title", "This is a simple popup");
            PopupAPI.openPopup(getRouteNode(), popup);
        });

        showLoading.setOnAction(e -> {
            // Create Future, which takes 5 seconds to complete
            FXFuture<Object> future = FXFuture.runBackground(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
                return null;
            });

            PopupAPI.showLoadingScreen(getRouteNode(), future);
        });

        result.getChildren().addAll(button1, showLoading);

        return result;
    }
}
