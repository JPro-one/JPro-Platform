package one.jpro.platform.treeshowing;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;

public class TreeShowing {
    private static final Object KEY_TREE_SHOWING = new Object();

    public static BooleanProperty treeShowing(Node node) {
        BooleanProperty prop = (BooleanProperty) node.getProperties().get(KEY_TREE_SHOWING);
        if (prop == null) {
            prop = new TreeShowingProperty(node);
            node.getProperties().put(KEY_TREE_SHOWING, prop);
        }
        return prop;
    }

    public static boolean isTreeShowing(Node node) {
        return node.getScene() != null &&
                node.getScene().getWindow() != null &&
                node.getScene().getWindow().isShowing();
    }

    private static class TreeShowingProperty extends SimpleBooleanProperty {

        Scene scene = null;
        Window window = null;

        TreeShowingProperty(Node node) {
            super(node, "treeVisible", false);

            Runnable updateValue = () -> {
                Scene myScene = node.getScene();
                set(myScene != null &&
                        myScene.getWindow() != null &&
                        myScene.getWindow().isShowing());
            };

            ChangeListener<Boolean> updateVisibleListener = (p, o, value) -> updateValue.run();

            ChangeListener<Window> updateWindowListener = (p, o, ignore) -> {
                Scene scene = node.getScene();
                Window changedWindow = scene == null ? null : scene.getWindow();

                if (window != null) {
                    window.showingProperty().removeListener(updateVisibleListener);
                }
                window = changedWindow;
                if (changedWindow != null) {
                    changedWindow.showingProperty().addListener(updateVisibleListener);
                    updateVisibleListener.changed(null, null, changedWindow.isShowing());
                } else {
                    updateValue.run();
                }
            };

            ChangeListener<Scene> updateSceneListener = (p, o, ignore) -> {
                Scene changedScene = node.getScene();
                if (scene != null) {
                    scene.windowProperty().removeListener(updateWindowListener);
                }
                scene = changedScene;
                if (changedScene != null) {
                    changedScene.windowProperty().addListener(updateWindowListener);
                    updateWindowListener.changed(null, null, changedScene.getWindow());
                } else {
                    updateWindowListener.changed(null, null, null);
                    updateValue.run();
                }
            };

            node.sceneProperty().addListener(updateSceneListener);
            updateSceneListener.changed(null, null, node.getScene());
        }
    }
}
