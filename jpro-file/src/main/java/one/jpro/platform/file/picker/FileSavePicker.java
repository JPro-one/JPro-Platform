package one.jpro.platform.file.picker;

import com.jpro.webapi.WebAPI;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * {@link FilePicker} interface extension for file save operations.
 *
 * @author Besmir Beqiri
 */
public interface FileSavePicker extends FilePicker {

    static FileSavePicker create(Node node) {
        if (WebAPI.isBrowser()) {
            return new WebFileSavePicker(node);
        } else {
            return new NativeFileSavePicker(node);
        }
    }

    /**
     * Gets the handler to be called when the user selects a file.
     *
     * @return the event handler or <code>null</code>.
     */
    Function<File, CompletableFuture<Void>> getOnFileSelected();

    /**
     * Sets the handler to be called when the user selects a file.
     *
     * @param value the event handler or <code>null</code>.
     */
    void setOnFileSelected(Function<File, CompletableFuture<Void>> value);

    /**
     * Defines the handler to be called when the user selects a file.
     * The handler returns the selected files or {@code null} if
     * no file has been selected.
     */
    ObjectProperty<Function<File, CompletableFuture<Void>>> onFileSelectedProperty();
}
