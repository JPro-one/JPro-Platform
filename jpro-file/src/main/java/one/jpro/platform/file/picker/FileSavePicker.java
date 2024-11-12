package one.jpro.platform.file.picker;

import com.jpro.webapi.WebAPI;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Interface for a file save picker component that allows users to select a file destination for saving.
 * Extends the {@link FilePicker} interface to provide additional functionality for saving files.
 *
 * <p>This interface provides methods for handling file save operations in a JavaFX
 * application, with implementations that can work in both web browser and native environments.</p>
 *
 * <p>Use the static {@link #create(Node)} method to obtain an instance of {@code FileSavePicker},
 * which will return a suitable implementation depending on the runtime environment.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * FileSavePicker fileSavePicker = FileSavePicker.create(node);
 * fileSavePicker.setOnFileSelected(file -> {
 *     return return file -> CompletableFuture.runAsync(() -> {
 *             // Handle file saving logic here
 *         });
 * });
 * }</pre>
 *
 * @see FilePicker
 * @see WebFileSavePicker
 * @see NativeFileSavePicker
 *
 * @author Besmir Beqiri
 */
public interface FileSavePicker extends FilePicker {

    /**
     * Creates a new instance of {@code FileSavePicker} associated with the specified {@link Node}.
     * Depending on whether the application is running in a browser or not, it returns an appropriate implementation.
     *
     * @param node the JavaFX node associated with the file save picker
     * @return a new instance of {@code FileSavePicker}
     */
    static FileSavePicker create(Node node) {
        if (WebAPI.isBrowser()) {
            return new WebFileSavePicker(node);
        } else {
            return new NativeFileSavePicker(node);
        }
    }

    /**
     * Gets the handler function that is called when the user selects a file to save.
     * The handler accepts a {@link File} and returns a {@link CompletableFuture}&lt;{@link Void}&gt; indicating
     * when the file save operation has completed.
     *
     * @return the handler function for file selection, or {@code null} if none is set
     */
    Function<File, CompletableFuture<Void>> getOnFileSelected();

    /**
     * Sets the handler function to be called when the user selects a file to save.
     * The handler function should accept a {@link File} object and return a {@link CompletableFuture}&lt;{@link Void}&gt;
     * that completes when the save operation is finished.
     *
     * @param value the handler function to set, or {@code null} to remove any existing handler
     */
    void setOnFileSelected(Function<File, CompletableFuture<Void>> value);

    /**
     * Returns the property representing the handler function that is called when the user selects a file.
     * This property allows for observing changes to the handler and for binding.
     * The handler function accepts a {@link File} and returns a {@link CompletableFuture}&lt;{@link Void}&gt;
     * indicating when the file save operation has completed.
     *
     * @return the property containing the handler function for file selection
     */
    ObjectProperty<Function<File, CompletableFuture<Void>>> onFileSelectedProperty();
}
