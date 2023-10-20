package one.jpro.platform.file.dropper;

import com.jpro.webapi.WebAPI;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.FileSource;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * This interface represents a file dropper that allows users to select and drop files.
 * The implementation of this interface depends on whether the application is running
 * in a browser via JPro server or outside the browser as a desktop application.
 *
 * @param <F> the type of the file source
 * @author Besmir Beqiri
 */
public interface FileDropper<F extends FileSource> {

    /**
     * Creates a file dropper. If the application is running in a
     * browser via JPro server, then a web version of the file
     * dropper is returned. If the application is not running inside
     * the browser than a desktop version is returned.
     *
     * @param node the action node for this file dropper
     * @throws NullPointerException if the node is null
     * @return a {@link FileDropper} object.
     */
    static FileDropper<? extends FileSource> create(Node node) {
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
     * Gets the selection mode of the file dropper.
     * The default value is {@link SelectionMode#SINGLE}.
     */
    SelectionMode getSelectionMode();

    /**
     * Sets the selection mode of the file dropper.
     */
    void setSelectionMode(SelectionMode value);

    /**
     * Defines the selection mode of the file dropper.
     * The default value is {@link SelectionMode#SINGLE}.
     */
    ObjectProperty<SelectionMode> selectionModeProperty();

    /**
     * Gets the handler to be called when the user selects files.
     *
     * @return the handler
     */
    Consumer<List<F>> getOnFilesSelected();

    /**
     * Sets the handler to be called when the user selects files.
     *
     * @param value the handler
     */
    void setOnFilesSelected(Consumer<List<F>> value);

    /**
     * Defines the handler to be called when the user selects files.
     * The handler returns the selected files or {@code null} if
     * no file has been selected.
     */
    ObjectProperty<Consumer<List<F>>> onFilesSelectedProperty();

    /**
     * Returns a boolean value indicating if files are currently being dragged
     * over the target node.
     *
     * @return {@code true} if files are currently being dragged over the target node,
     *         {@code false} otherwise.
     */
    boolean isFilesDragOver();

    /**
     * Defines the property for the files drag over state.
     */
    ReadOnlyBooleanProperty filesDragOverProperty();
}
