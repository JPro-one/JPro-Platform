package one.jpro.platform.routing.popup;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import one.jpro.platform.routing.Filters;
import one.jpro.platform.routing.Request;
import one.jpro.platform.routing.filter.container.Container;
import one.jpro.platform.routing.filter.container.ContainerFilter;
import simplefx.experimental.parts.FXFuture;
import one.jpro.platform.routing.Filter;

import java.util.Objects;

/**
 * Provides utility methods for managing popups in a JPro application.
 * Popups are always placed into whatever Pane you’ve registered via
 * registerPopupContainer — and you never pass the container in manually.
 */
public class PopupAPI {

    /**
     * Map any “context holder” Node → the Pane that should host popups under it.
     * Call registerPopupContainer(...) once at startup.
     */
    public static final ContextManager<Pane> POPUP_CONTAINER_CONTEXT = new ContextManager<>();

    /**
     * Map every popup root Node → itself, so we can discover the popup from any child.
     */
    public static final ContextManager<Node> POPUP_CONTEXT = new ContextManager<>();

    /**
     * Register a node under which all popups should live.
     *
     * @param container the Pane into which popups will be added
     */
    public static void registerPopupContainer(Pane container) {
        Objects.requireNonNull(container,     "container must not be null");
        POPUP_CONTAINER_CONTEXT.setContext(container, container);
    }

    /**
     * Opens a popup by looking up its container from the nearest registered holder.
     *
     * @param contextHolder any Node that was previously registered
     * @param popup the Node to show as a popup
     */
    public static void openPopup(Node contextHolder, Node popup) {
        Objects.requireNonNull(popup, "popup must not be null");
        // find the Pane we registered under this holder (or its parents)
        Pane container = POPUP_CONTAINER_CONTEXT.getContext(contextHolder);

        // mark “this node is a popup root”
        POPUP_CONTEXT.setContext(popup, popup);
        // also carry forward the container lookup for children
        POPUP_CONTAINER_CONTEXT.setContext(popup, container);

        container.getChildren().add(popup);
    }

    /**
     * Closes whichever popup contains the given node.
     *
     * @param anyNodeInPopup any Node inside (or equal to) the popup
     */
    public static void closePopup(Node anyNodeInPopup) {
        // find the popup root
        Node popup = POPUP_CONTEXT.getContext(anyNodeInPopup);
        // find its container
        Pane container = POPUP_CONTAINER_CONTEXT.getContext(popup);
        container.getChildren().remove(popup);
    }

    /**
     * Shows a translucent loading‐indicator popup bound to an FXFuture.
     *
     * @param contextHolder any Node that was registered as a container holder
     * @param fxFuture when this completes, the loading popup will auto-close
     * @param <T> the future’s result type
     */
    public static <T> FXFuture<T> showLoadingScreen(Node contextHolder, FXFuture<T> fxFuture) {
        Objects.requireNonNull(contextHolder, "contextHolder must not be null");

        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setMaxWidth(100);
        indicator.setMaxHeight(100);

        StackPane popup = new StackPane(indicator);
        // semi-transparent black backdrop
        popup.setStyle("-fx-background-color: #00000066;");

        openPopup(contextHolder, popup);

        fxFuture.onComplete(v -> {
            indicator.setProgress(1.0);
            closePopup(popup);
        });

        return fxFuture;
    }

    /**
     * Returns a filter which adds a Stackpane, which is used as a popup container.
     * @return a filter which adds a Stackpane, which is used as a popup container
     */
    public static Filter createPopupContainerFilter() {
        return ContainerFilter.create(
                () -> new PopupContainer(), PopupContainer.class);
    }

    private static class PopupContainer extends StackPane implements Container {

        // the actual properties
        private final ObjectProperty<Node>    contentProperty = new SimpleObjectProperty<>(this, "content");
        private final ObjectProperty<Request> requestProperty = new SimpleObjectProperty<>(this, "request");

        public PopupContainer() {
            // register this StackPane as the host for all popups below it
            registerPopupContainer(this);
            contentProperty.addListener((observable, oldValue, newValue) -> {
                if(oldValue != null) {
                    getChildren().remove(oldValue);
                }
                if(newValue != null) {
                    getChildren().add(newValue);
                }
            });
        }

        // Methods not really relevant here, but requried for the API.

        // contentProperty / getContent / setContent
        @Override
        public ObjectProperty<Node> contentProperty() {
            return contentProperty;
        }

        @Override
        public Node getContent() {
            return contentProperty.get();
        }

        @Override
        public void setContent(Node content) {
            this.contentProperty.set(content);
        }

        // requestProperty / getRequest / setRequest
        @Override
        public ObjectProperty<Request> requestProperty() {
            return requestProperty;
        }

        @Override
        public Request getRequest() {
            return requestProperty.get();
        }

        @Override
        public void setRequest(Request request) {
            this.requestProperty.set(request);
        }
    }
}
