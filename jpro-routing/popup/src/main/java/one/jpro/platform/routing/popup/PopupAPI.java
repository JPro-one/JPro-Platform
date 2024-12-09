package one.jpro.platform.routing.popup;

import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import simplefx.experimental.parts.FXFuture;

import java.util.Objects;

/**
 * Provides utility methods for managing popups in a JPro application.
 * This includes opening and closing popups, as well as showing a loading screen.
 *
 * @author Florian Kirmaier
 */
public class PopupAPI {

    public static final Object POPUP_CONTEXT = new Object();

    /**
     * Opens a popup within the specified context.
     *
     * @param popupContext the pane that will serve as the context for the popup
     * @param popup        the node representing the popup to be displayed
     * @throws RuntimeException if {@code popupContext} is null
     */
    public static void openPopup(Pane popupContext, Node popup) {
        Objects.requireNonNull(popupContext, "popupContext must not be null");
        popup.getProperties().put(POPUP_CONTEXT, popupContext);
        popupContext.getChildren().add(popup);
    }

    /**
     * Closes the specified popup node.
     *
     * @param popupNode the popup node to be closed
     */
    public static void closePopup(Node popupNode) {
        Node popup = getPopup(popupNode);
        Pane popupContext = getPopupContext(popup);
        popupContext.getChildren().remove(popup);
    }

    /**
     * Shows a loading screen on the specified popup context and binds it to the completion of a future.
     *
     * @param <T>          the type of the result produced by the future
     * @param popupContext the pane that will serve as the context for the loading screen
     * @param fxFuture     the future whose completion will trigger the removal of the loading screen
     * @return the {@link FXFuture} passed as an argument
     * @throws RuntimeException if {@code popupContext} is null
     */
    public static <T> FXFuture<T> showLoadingScreen(Pane popupContext, FXFuture<T> fxFuture) {
        Objects.requireNonNull(popupContext, "popupContext must not be null");

        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setMaxWidth(100);
        indicator.setMaxHeight(100);
        StackPane popup = new StackPane(indicator);
        popup.setStyle("-fx-background-color: #00000066;");
        openPopup(popupContext, popup);
        fxFuture.onComplete((v) -> {
            indicator.setProgress(1.0);
            closePopup(popup);
        });
        return fxFuture;
    }

    /**
     * Retrieves the popup context for a given popup node.
     *
     * @param popup the node for which to find the popup context
     * @return the pane that serves as the context for the popup
     */
    public static Pane getPopupContext(Node popup) {
        Pane context = (Pane) popup.getProperties().get(POPUP_CONTEXT);
        return (context == null) ? getPopupContext(popup.getParent()) : context;
    }

    /**
     * Retrieves the popup node from a given node, if it exists.
     *
     * @param popupNode the node from which to retrieve the popup
     * @return the node representing the popup, or the input node if no popup context is found
     */
    public static Node getPopup(Node popupNode) {
        Node context = (Node) popupNode.getProperties().get(POPUP_CONTEXT);
        return (context == null) ? getPopup(popupNode.getParent()) : popupNode;
    }
}
