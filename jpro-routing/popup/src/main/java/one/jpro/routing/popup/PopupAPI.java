package one.jpro.routing.popup;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import simplefx.experimental.parts.FXFuture;

/**
 * The popupContext is usually a StackPane. Typically, the "RouteNode" of the Routing API, which is a StackPane.
 */
public class PopupAPI {

    static Object POPUP_CONTEXT = new Object();

    /**
     * Closes a popup with the given content.
     */
    public static void closePopup(Node popupNode) {
        Node popup = getPopup(popupNode);
        Pane popupContext = getPopupContext(popup);
        if(popupContext == null) {
            throw new RuntimeException("popupContext must not be null");
        }
        popupContext.getChildren().remove(popup);
    }

    /**
     * Shows a popup with the given content.
     * @param popupContext
     * @param popup
     */
    public static void openPopup(Pane popupContext, Node popup) {
        if(popupContext == null) {
            throw new RuntimeException("popupContext must not be null");
        }
        popup.getProperties().put(POPUP_CONTEXT, popupContext);
        popupContext.getChildren().add(popup);
    }

    /**
     * Shows a loading screen, which is a progress indicator, until the given future is completed.
     * Feel encouraged to copy this implementation, and modify it to your needs.
     */
    public static FXFuture showLoadingScreen(Pane popupContext, FXFuture fu) {
        if(popupContext == null) {
            throw new RuntimeException("popupContext must not be null");
        }
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setMaxWidth(100);
        indicator.setMaxHeight(100);
        StackPane popup = new StackPane(indicator);
        popup.setStyle("-fx-background-color: #00000066;");
        openPopup(popupContext, popup);
        fu.onComplete((v) -> {
            indicator.setProgress(1.0);
            closePopup(popup);
            return null;
        });
        return fu;
    }

    /**
     * Gets the PopupContext from the node of a popup. The node can be any node of the popup.
     */
    public static Pane getPopupContext(Node popup) {
        Pane context = (Pane) popup.getProperties().get(POPUP_CONTEXT);
        if(context == null) {
            return getPopupContext(popup.getParent());
        } else {
            return context;
        }

    }

    public static Node getPopup(Node popupNode) {
        Node context = (Node) popupNode.getProperties().get(POPUP_CONTEXT);
        if(context == null) {
            return getPopup(popupNode.getParent());
        } else {
            return popupNode;
        }
    }

}
