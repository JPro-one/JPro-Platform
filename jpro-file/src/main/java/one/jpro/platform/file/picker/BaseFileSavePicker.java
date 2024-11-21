package one.jpro.platform.file.picker;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import one.jpro.platform.file.util.NodeUtils;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * This is an abstract class that implements the {@link FileSavePicker} interface.
 * It provides a base implementation for common functionality shared by native and web implementations.
 *
 * @see FileSavePicker
 * @see NativeFileSavePicker
 * @see WebFileSavePicker
 *
 * @author Besmir Beqiri
 * @author Indrit Beqiri
 */
abstract class BaseFileSavePicker extends BaseFilePicker implements FileSavePicker {

    /**
     * Constructs a new {@code BaseFileSavePicker} associated with the specified {@link Node}.
     * Adds a mouse click event handler to the node that triggers the {@link #showDialog()} method.
     * The event handler is added using {@link Node#addEventHandler} to avoid overriding any
     * existing mouse click event handlers on the node.
     *
     * @param node the JavaFX node associated with the file save picker
     */
    BaseFileSavePicker(Node node) {
        super(node);
        NodeUtils.addEventHandler(node, MouseEvent.MOUSE_CLICKED, mouseEvent -> showDialog());
    }

    /**
     * Displays the file save dialog to the user. This method must be implemented by subclasses
     * to provide the specific logic for showing the file save dialog, depending on whether the
     * application is running in a native environment or within a web browser.
     */
    abstract void showDialog();

    // on files selected property
    ObjectProperty<Function<File, CompletableFuture<Void>>> onFileSelected;

    @Override
    public final Function<File, CompletableFuture<Void>> getOnFileSelected() {
        return onFileSelected == null ? null : onFileSelected.get();
    }

    @Override
    public final void setOnFileSelected(Function<File, CompletableFuture<Void>> value) {
        onFileSelectedProperty().setValue(value);
    }

    @Override
    public final ObjectProperty<Function<File, CompletableFuture<Void>>> onFileSelectedProperty() {
        if (onFileSelected == null) {
            onFileSelected = new SimpleObjectProperty<>(this, "onFileSelected");
        }
        return onFileSelected;
    }
}
