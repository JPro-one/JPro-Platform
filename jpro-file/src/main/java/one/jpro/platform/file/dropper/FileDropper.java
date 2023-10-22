package one.jpro.platform.file.dropper;

import com.jpro.webapi.WebAPI;
import javafx.beans.property.ObjectProperty;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.scene.Node;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.MultipleFileSelector;
import one.jpro.platform.file.event.FileDragEvent;

import java.util.Objects;

/**
 * This interface represents a file dropper that allows users to select and drop files.
 * The implementation of this interface depends on whether the application is running
 * in a browser via JPro server or outside the browser as a desktop application.
 *
 * @author Besmir Beqiri
 */
public interface FileDropper extends MultipleFileSelector, EventTarget {

    /**
     * Creates a file dropper. If the application is running in a
     * browser via JPro server, then a web version of the file
     * dropper is returned. If the application is not running inside
     * the browser than a desktop version is returned.
     *
     * @param node the action node for this file dropper
     * @return a {@link FileDropper} object.
     * @throws NullPointerException if the node is null
     */
    static FileDropper create(Node node) {
        Objects.requireNonNull(node, "node must not be null");
        if (WebAPI.isBrowser()) {
            return new WebFileDropper(node);
        } else {
            return new NativeFileDropper(node);
        }
    }

    /**
     * Returns the target node for this file dropper.
     *
     * @return the target node for this file dropper.
     */
    Node getNode();

    /**
     * Gets the extension filter which is currently used.
     *
     * @return the extension filter or {@code null} if no extension
     */
    ExtensionFilter getExtensionFilter();

    /**
     * Sets the extension filter which is currently used.
     *
     * @param value the extension filter
     */
    void setExtensionFilter(ExtensionFilter value);

    /**
     * Defines the property for the extension filter.
     */
    ObjectProperty<ExtensionFilter> extensionFilterProperty();

    /**
     * Retrieves the event handler to be called when file dragging gesture enters
     * the target node.
     *
     * @return the event handler or <code>null</code>.
     */
    EventHandler<FileDragEvent> getOnDragEntered();

    /**
     * Sets the event handler to be called when file dragging gesture enters
     * the target node.
     *
     * @param value the event handler or <code>null</code>.
     */
    void setOnDragEntered(EventHandler<FileDragEvent> value);

    /**
     * Event handler invoked when file dragging gesture enters the target node.
     */
    ObjectProperty<EventHandler<FileDragEvent>> onDragEnteredProperty();

    /**
     * Retrieves the event handler to be called when file dragging gesture exits
     * the target node.
     *
     * @return the event handler or <code>null</code>.
     */
    EventHandler<FileDragEvent> getOnDragExited();

    /**
     * Sets the event handler to be called when file dragging gesture enters
     * the target node.
     *
     * @param value the event handler or <code>null</code>.
     */
    void setOnDragExited(EventHandler<FileDragEvent> value);

    /**
     * Event handler invoked when file dragging gesture exits the target node.
     */
    ObjectProperty<EventHandler<FileDragEvent>> onDragExitedProperty();
}
