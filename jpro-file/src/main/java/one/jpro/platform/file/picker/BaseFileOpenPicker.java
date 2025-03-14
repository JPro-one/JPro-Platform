package one.jpro.platform.file.picker;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import one.jpro.platform.file.FileSource;

import java.util.List;
import java.util.function.Consumer;

/**
 * This is an abstract class that implements the {@link FileOpenPicker} interface.
 * It provides a base implementation for common functionality shared by native and web implementations.
 *
 * @author Besmir Beqiri
 */
abstract class BaseFileOpenPicker extends BaseFilePicker implements FileOpenPicker {

    /**
     * {@inheritDoc}
     */
    BaseFileOpenPicker(Node node) {
        super(node);
    }

    // selection mode property
    ObjectProperty<SelectionMode> selectionMode;

    @Override
    public final SelectionMode getSelectionMode() {
        return selectionMode == null ? SelectionMode.SINGLE : selectionMode.get();
    }

    @Override
    public final void setSelectionMode(final SelectionMode value) {
        selectionModeProperty().set(value);
    }

    // on files selected property
    ObjectProperty<Consumer<List<? extends FileSource>>> onFilesSelected;

    @Override
    public final Consumer<List<? extends FileSource>> getOnFilesSelected() {
        return onFilesSelected == null ? null : onFilesSelected.get();
    }

    @Override
    public final void setOnFilesSelected(Consumer<List<? extends FileSource>> value) {
        onFilesSelectedProperty().set(value);
    }

    @Override
    public ObjectProperty<Consumer<List<? extends FileSource>>> onFilesSelectedProperty() {
        if (onFilesSelected == null) {
            onFilesSelected = new SimpleObjectProperty<>(this, "onFilesSelected");
        }
        return onFilesSelected;
    }
}
