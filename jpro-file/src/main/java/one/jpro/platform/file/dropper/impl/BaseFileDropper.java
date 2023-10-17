package one.jpro.platform.file.dropper.impl;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.FileSource;
import one.jpro.platform.file.dropper.FileDropper;

/**
 * This is an abstract class that implements the {@link FileDropper} interface.
 * It provides a base implementation for common functionality shared by different file dropper implementations.
 *
 * @param <F> the type of FileSource used by the file dropper
 * @author Besmir Beqiri
 */
abstract class BaseFileDropper<F extends FileSource<?>> implements FileDropper<F> {

    private final Node node;

    public BaseFileDropper(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

    // extension filter property
    ObjectProperty<ExtensionFilter> extensionFilter;

    @Override
    public final ExtensionFilter getExtensionFilter() {
        return (extensionFilter != null) ? extensionFilter.get() : null;
    }

    @Override
    public final void setExtensionFilter(final ExtensionFilter filter) {
        extensionFilterProperty().setValue(filter);
    }

    @Override
    public final ObjectProperty<ExtensionFilter> extensionFilterProperty() {
        if (extensionFilter == null) {
            extensionFilter = new SimpleObjectProperty<>(this, "extensionFilter");
        }
        return extensionFilter;
    }

    // selection mode property
    ObjectProperty<SelectionMode> selectionMode;

    @Override
    public final SelectionMode getSelectionMode() {
        return selectionMode == null ? SelectionMode.SINGLE : selectionMode.get();
    }

    @Override
    public final void setSelectionMode(final SelectionMode value) {
        selectionModeProperty().setValue(value);
    }

    @Override
    public ObjectProperty<SelectionMode> selectionModeProperty() {
        if (selectionMode == null) {
            selectionMode = new SimpleObjectProperty<>(this, "selectionMode", SelectionMode.SINGLE);
        }
        return selectionMode;
    }
}
