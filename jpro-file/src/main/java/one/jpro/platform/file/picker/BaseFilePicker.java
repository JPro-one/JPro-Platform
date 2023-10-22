package one.jpro.platform.file.picker;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import one.jpro.platform.file.ExtensionFilter;

import java.util.Objects;

/**
 * Base file picker implementation.
 *
 * @author Besmir Beqiri
 */
abstract class BaseFilePicker implements FilePicker {

    private final Node node;

    /**
     * Constructs a new instance with the specified Node.
     *
     * @param node the node to which the file picker should be attached
     */
    BaseFilePicker(Node node) {
        this.node = Objects.requireNonNull(node, "node must not be null");
    }

    @Override
    public final Node getNode() {
        return node;
    }

    // initial file name property
    StringProperty initialFileName;

    private final ObservableList<ExtensionFilter> extensionFilters = FXCollections.observableArrayList();

    @Override
    public final ObservableList<ExtensionFilter> getExtensionFilters() {
        return extensionFilters;
    }

    // selected extension filter property
    ObjectProperty<ExtensionFilter> selectedExtensionFilter;

    @Override
    public final ExtensionFilter getSelectedExtensionFilter() {
        return (selectedExtensionFilter != null) ? selectedExtensionFilter.get() : null;
    }

    @Override
    public final void setSelectedExtensionFilter(final ExtensionFilter filter) {
        selectedExtensionFilterProperty().setValue(filter);
    }
}
