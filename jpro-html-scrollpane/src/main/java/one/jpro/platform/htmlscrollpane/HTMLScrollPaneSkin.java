package one.jpro.platform.htmlscrollpane;

import com.jpro.webapi.HTMLView;
import com.jpro.webapi.WebAPI;
import de.sandec.jmemorybuddy.CleanupDetector;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.PopupWindow;
import javafx.stage.Window;
import one.jpro.platform.treeshowing.TreeShowing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

public class HTMLScrollPaneSkin extends SkinBase<ScrollPane> {

    private static final Logger logger = LoggerFactory.getLogger(HTMLScrollPaneSkin.class);

    final StackPane viewContent;
    final HTMLView htmlView;
    final Node cssBridgeTarget;
    final String attributes;
    Pane htmlViewContent;
    ChangeListener<Number> widthListener;

    /**
     * Creates a new instance with the given ScrollPane control.
     *
     * @param control the ScrollPane control to be associated with the skin
     */
    public HTMLScrollPaneSkin(ScrollPane control) {
        this(control, "");
    }

    /**
     * Creates a new instance of HTMLScrollPaneSkin with the given ScrollPane control and attributes.
     *
     * @param control    the ScrollPane control to be associated with the skin
     * @param attributes a string containing any additional attributes
     */
    public HTMLScrollPaneSkin(ScrollPane control, String attributes) {
        super(control);

        this.attributes = attributes;

        viewContent = new StackPane();
        htmlView = new HTMLView();
        cssBridgeTarget = new Pane();
        cssBridgeTarget.setId("cssBridgeTarget");
        htmlView.blockMouseInputProperty().set(true);
        htmlView.setMinHeight(500);

        TreeShowing.treeShowing(control).addListener(
                (p, o, n) -> {
                    // add cssBridgeTarget when scene is not null, remove otherwise
                    if (n) {
                        getChildren().add(cssBridgeTarget);
                    } else {
                        getChildren().remove(cssBridgeTarget);
                        workaroundRemovePeerParent(cssBridgeTarget);
                    }
                }
        );

        WebAPI.getWebAPI(control, webapi -> {
            htmlViewContent = new HorizontalStackPane();
            htmlViewContent.setId("scrollpaneskin_htmlViewContent");
            viewContent.getChildren().add(htmlView);
            getChildren().add(viewContent);

            Runnable updateContent = () -> {
                if (TreeShowing.treeShowing(control).get()) {
                    htmlViewContent.getChildren().setAll(control.getContent());
                } else {
                    htmlViewContent.getChildren().clear();
                }
            };

            setupHTMLView(webapi);
            registerChangeListener(control.contentProperty(), e -> {
                logger.debug("Trigger: content");
                updateContent.run();
            });
            registerChangeListener(TreeShowing.treeShowing(control), e -> {
                logger.debug("Trigger: tree-showing");
                updateContent.run();
            });
            updateContent.run();
        });
    }

    /**
     * Sets up the HTMLView for the given WebAPI.
     *
     * @param webapi the WebAPI to be associated with the HTMLView
     */
    public void setupHTMLView(WebAPI webapi) {
        // DO CSS BRIDGE
        PopupControl contentPage = new PopupControl();
        contentPage.setSkin(new WeakPopupControlSkin(htmlViewContent));
        String windowId = webapi.registerWindow(contentPage);

        widthListener = (p, o, n) -> htmlViewContent.prefWidthProperty().set(n.doubleValue());
        ((Region) getNode()).widthProperty().addListener(new WeakChangeListener<>(widthListener));
        htmlViewContent.prefWidthProperty().set(((Region) getNode()).getWidth());

        contentPage.show(cssBridgeTarget, 0, 0);

        Node localCssBridgeTarget = cssBridgeTarget;
        cssBridgeTarget.sceneProperty().addListener((p, o, n) -> {
            if (n != null) {
                contentPage.show(localCssBridgeTarget, 0, 0);
            } else {
                contentPage.hide();
                // set ownerWindow to null?
                workaroundClearNewEventTargets(contentPage.getScene());
                workaroundOwnerWindow(contentPage);
            }
        });

        String number = "" + new Random().nextInt(1000000);
        String id = "scrollelem_" + number;
        String idapp = "scrollelemapp_" + number;

        String content = "<div id=\"" + id + "\" style=\"overflow-x: hidden; overflow-y:scroll; \"><jpro-app loader=\"none\" id=\"" + idapp + "\" href=\"/app/" + windowId + "\" fxwidth=\"true\" fxheight=\"true\" nativeScrolling=\"true\" " + attributes + "></jpro-app></div>";
        logger.debug("Setting content to: " + content);

        webapi.executeScript("jpro." + idapp + " = document.getElementById('" + idapp + "');");
        // Remove the cleanuplistener, because it makes a reference to the stage. Or just remove it, when it's closed.
        CleanupDetector.onCleanup(this,
                new WeakCleanupRunnable(webapi, "console.log('DISPOSING');" +
                        "jpro." + idapp + ".jproimpl.dispose();" +
                        "delete jpro." + idapp + ";" +
                        "console.log('Disposed jpro." + idapp + "');", WebAPI.getWebAPI(contentPage)));

        // Update width and height of the htmlView
        ((Region) getNode()).widthProperty().addListener((p, o, n) ->
                webapi.executeScript("document.getElementById('" + id + "').style.width = '" + n + "px';"));
        webapi.executeScript("document.getElementById('" + id + "').style.width = '" + ((Region) getNode()).getWidth() + "px';");

        ((Region) getNode()).heightProperty().addListener((p, o, n) ->
                webapi.executeScript("document.getElementById('" + id + "').style.height = '" + n + "px';"));
        webapi.executeScript("document.getElementById('" + id + "').style.height = '" + ((Region) getNode()).getHeight() + "px';");

        htmlView.setContent(content);
    }


    static class WeakPopupControlSkin implements Skin {
        WeakReference<Node> whtmlViewContent;

        public WeakPopupControlSkin(Node n) {
            super();
            whtmlViewContent = new WeakReference<>(n);
        }

        @Override
        public Skinnable getSkinnable() {
            return null;
        }

        @Override
        public Node getNode() {
            return whtmlViewContent.get();
        }

        @Override
        public void dispose() {
        }
    }

    static class HorizontalStackPane extends StackPane {
        @Override
        public Orientation getContentBias() {
            return Orientation.HORIZONTAL;
        }

        @Override
        protected double computeMaxHeight(double v) {
            double v2 = v > 0 ? v : prefWidth(-1);
            return super.computeMaxHeight(v2);
        }

        @Override
        protected double computePrefHeight(double v) {
            double v2 = v > 0 ? v : prefWidth(-1);
            return super.computePrefHeight(v2);
        }

        @Override
        protected double computeMaxWidth(double v) {
            double v2 = v > 0 ? v : prefWidth(-1);
            return super.computeMaxWidth(v2);
        }
    }

    static class WeakCleanupRunnable implements Runnable {
        WeakReference<WebAPI> webAPI;
        WeakReference<WebAPI> webAPI2;
        String action;

        public WeakCleanupRunnable(WebAPI webAPI, String action, WebAPI webAPI2) {
            if (webAPI == webAPI2) {
                throw new RuntimeException("Got same WebAPI twice!");
            }
            this.webAPI = new WeakReference<>(webAPI);
            this.action = action;
            this.webAPI2 = new WeakReference<>(webAPI2);
        }

        public void run() {
            WebAPI webapi = webAPI.get();
            if (webapi != null) {
                webapi.executeScript(action);
            }
            Platform.runLater(() -> {
                WebAPI webapi2 = webAPI2.get();
                if (webapi2 != null) {
                    webapi2.closeInstance();
                }
            });
        }
    }

    /**
     * Workaround method to remove the parent reference of a Node's peer using reflection.
     *
     * @param node the Node whose peer's parent reference needs to be removed
     */
    public static void workaroundRemovePeerParent(Node node) {
        // set cssBridgeTarget.peer.parent to null with reflection
        // It's important to use getDeclaredField, because the field is private
        // The field have to be set to be accessible
        try {
            Field parentField = Node.class.getDeclaredField("peer");
            parentField.setAccessible(true);
            Object peer = parentField.get(node);
            if (peer != null) {
                var ngNode = peer.getClass().getClassLoader().loadClass("com.sun.javafx.sg.prism.NGNode");
                parentField = ngNode.getDeclaredField("parent");
                parentField.setAccessible(true);
                parentField.set(peer, null);
            }
        } catch (Exception ex) {
            logger.error("Error while removing peer parent", ex);
        }
    }

    /**
     * Workaround method to clear the newEventTargets list of the mouseHandler in a Scene using reflection.
     *
     * @param scene the Scene whose mouseHandler's newEventTargets list needs to be cleared
     */
    public static void workaroundClearNewEventTargets(Scene scene) {
        // Clear scene.mouseHandler.newEventTargets with reflection
        // It's important to use getDeclaredField, because the field is private
        // The field have to be set to be accessible
        try {
            Field mouseHandlerField = Scene.class.getDeclaredField("mouseHandler");
            mouseHandlerField.setAccessible(true);
            Object mouseHandler = mouseHandlerField.get(scene);
            if (mouseHandler != null) {
                var mouseHandlerClass = mouseHandler.getClass();
                Field newEventTargetsField = mouseHandlerClass.getDeclaredField("newEventTargets");
                newEventTargetsField.setAccessible(true);
                List<Object> newEventTargets = (List<Object>) newEventTargetsField.get(mouseHandler);
                if (newEventTargets != null) {
                    newEventTargets.clear();
                }
            }
        } catch (Exception ex) {
            logger.error("Error while clearing newEventTargets", ex);
        }
    }

    /**
     * Workaround method to set the ownerWindow of a Window to null using reflection.
     *
     * @param window the Window whose ownerWindow needs to be set to null
     */
    public static void workaroundOwnerWindow(Window window) {
        // Set ownerWindow to null with reflection
        // It's important to use getDeclaredField, because the field is private
        // The field have to be set to be accessible
        try {
            Field ownerNodeField = PopupWindow.class.getDeclaredField("ownerWindow");
            ownerNodeField.setAccessible(true);
            Property<Object> p = (Property<Object>) ownerNodeField.get(window);
            p.setValue(null);
        } catch (Exception ex) {
            logger.error("Error while setting ownerWindow to null", ex);
        }
    }
}