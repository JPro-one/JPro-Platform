package one.jpro.platform.file.picker;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * This is an abstract class that implements the {@link FileSavePicker} interface.
 * It provides a base implementation for common functionality shared by native and web implementations.
 *
 * @author Besmir Beqiri
 */
abstract class BaseFileSavePicker extends BaseFilePicker implements FileSavePicker {

    /**
     * {@inheritDoc}
     */
    BaseFileSavePicker(Node node) {
        super(node);
        node.setOnMouseClicked(mouseEvent -> showDialog());
    }

    abstract void showDialog();

    // on files selected property
    ObjectProperty<Function<File, CompletableFuture<File>>> onFileSelected;

    @Override
    public final Function<File, CompletableFuture<File>> getOnFileSelected() {
        return onFileSelected == null ? null : onFileSelected.get();
    }

    @Override
    public final void setOnFileSelected(Function<File, CompletableFuture<File>> value) {
        onFileSelectedProperty().setValue(value);
    }

    @Override
    public final ObjectProperty<Function<File, CompletableFuture<File>>> onFileSelectedProperty() {
        if (onFileSelected == null) {
            onFileSelected = new SimpleObjectProperty<>(this, "onFileSelected");
        }
        return onFileSelected;
    }
}
