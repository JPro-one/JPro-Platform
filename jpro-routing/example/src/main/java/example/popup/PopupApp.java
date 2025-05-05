package example.popup;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import one.jpro.platform.routing.Filters;
import one.jpro.platform.routing.Response;
import one.jpro.platform.routing.Route;
import one.jpro.platform.routing.RouteApp;
import one.jpro.platform.routing.dev.DevFilter;
import one.jpro.platform.routing.popup.PopupAPI;
import one.jpro.platform.routing.popup.simplepopup.SimplePopups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simplefx.experimental.parts.FXFuture;

import static one.jpro.platform.routing.Route.get;
import static one.jpro.platform.routing.Route.redirect;

/**
 * The {@code PopupApp} class extends {@code RouteApp} to demonstrate the use of popups in a JPro application.
 * It showcases how to create and display simple informational popups and a loading screen based on asynchronous tasks.
 */
public class PopupApp extends RouteApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(PopupApp.class);

    @Override
    public Route createRoute() {
        return Route.empty()
                .and(redirect("/", "/popup"))
                .and(get("/popup", (r) -> Response.node(popupSampleButtons())))
                .filter(Filters.FullscreenFilter(true))
                .filter(DevFilter.create())
                .filter(PopupAPI.createPopupContainerFilter())
                ;
    }

    /**
     * Creates a {@code Node} containing buttons that demonstrate the popup functionality.
     * One button shows a simple informational popup and the other displays a loading screen
     * tied to a background task.
     *
     * @return A {@code Node} with configured buttons for demonstrating popup functionality.
     */
    public Node popupSampleButtons() {
        VBox result = new VBox(8);

        Button showPopupButton = new Button("Show Popup");
        showPopupButton.setOnAction(event ->
                PopupAPI.openPopup(showPopupButton, SimplePopups.infoPopup("Title", "This is a simple popup")));

        Button showLoadingScreen = new Button("Show Loading Screen");
        showLoadingScreen.setOnAction(event ->
                PopupAPI.showLoadingScreen(showPopupButton, createWaitFuture(3000)));

        result.getChildren().addAll(showPopupButton, showLoadingScreen);

        return result;
    }

    /**
     * Creates a {@code FXFuture} that takes a specified number of milliseconds to complete.
     *
     * @param millis the number of milliseconds to sleep
     * @return A {@code FXFuture} object.
     */
    private FXFuture<Object> createWaitFuture(long millis) {
        return FXFuture.runBackground(() -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ex) {
                LOGGER.error("An error occurred while sleeping", ex);
            }
            return null;
        });
    }
}
