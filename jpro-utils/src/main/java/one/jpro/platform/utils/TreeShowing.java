package one.jpro.platform.utils;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;

/**
 * The {@code TreeShowing} class provides access to the internal {@code "treeShowing"} property of
 * JavaFX {@link javafx.scene.Node Nodes}. This property is essential for determining whether a
 * {@code Node} is currently part of the scene graph and actively being displayed.
 *
 * <p>
 * In JavaFX, a {@code Node} eligible for garbage collection might still be retained in memory
 * due to ongoing animations or background tasks. The internal {@code "treeShowing"} property
 * tracks the visibility and usage of each {@code Node}, but it is not exposed through the standard
 * JavaFX API. The {@code TreeShowing} class bridges this gap, enabling developers to monitor
 * a node's lifecycle and manage resources effectively to prevent memory leaks.
 * </p>
 *
 * <p>
 * By utilizing the {@code TreeShowing} class, developers can:
 * </p>
 *
 * <ul>
 *   <li>Detect when a {@code Node} is removed from the scene graph.</li>
 *   <li>Stop associated animations or background tasks to free up resources.</li>
 * </ul>
 *
 * <p>
 * This facilitates the creation of robust, memory-efficient JavaFX applications by ensuring that
 * unused nodes do not retain unnecessary resources.
 * </p>
 *
 * <p>
 * <strong>Example Usage:</strong>
 * </p>
 *
 * <pre>{@code
 * import javafx.animation.Timeline;
 * import javafx.scene.Node;
 *
 * public class Example {
 *     public void setupNode(Node myNode) {
 *         // Create a Timeline animation
 *         Timeline myTimeline = new Timeline();
 *         myTimeline.setCycleCount(Timeline.INDEFINITE);
 *
 *         // Obtain the TreeShowing instance for the node
 *         TreeShowing treeShowing = TreeShowing.treeShowing(myNode);
 *
 *         // Add a listener to respond to changes in the treeShowing property
 *         treeShowing.addListener((observable, oldValue, showing) -> {
 *             if (showing) {
 *                 // Start the animation when the node is part of the scene graph
 *                 myTimeline.play();
 *             } else {
 *                 // Stop the animation when the node is removed from the scene graph
 *                 myTimeline.stop();
 *             }
 *         });
 *
 *         // Immediately start the animation if the node is already showing
 *         if (treeShowing.get()) {
 *             myTimeline.play();
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p>
 * In this example, the listener monitors the {@code "treeShowing"} property of {@code myNode}.
 * When {@code myNode} is added to the scene graph, the animation starts. Conversely, when
 * {@code myNode} is removed, the animation stops, ensuring that resources are managed
 * efficiently.
 * </p>
 *
 * @author Florian Kirmaier
 */
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
