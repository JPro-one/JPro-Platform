package one.jpro.platform.file;

import javafx.beans.property.*;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an abstract source for file operations, offering a unified interface
 * for interacting with files that may originate from different platforms or locations
 * (e.g., local file system, web-based storage).
 * <p>
 * This class encapsulates common file attributes such as name, size, and object URL,
 * as well as actions like uploading. It employs the Property pattern for these attributes,
 * allowing for easy binding and observation in a JavaFX application.
 * <p>
 * This is a sealed class, which can only be subclassed by specific types ({@link NativeFileSource},
 * {@link WebFileSource}), ensuring a controlled set of implementations. This makes it easier to handle
 * file operations consistently across various types of file sources.
 * <p>
 * The class provides both synchronous and asynchronous methods for file uploading, allowing flexibility
 * in how file operations are handled.
 *
 * @author Besmir Beqiri
 */
public sealed abstract class FileSource permits NativeFileSource, WebFileSource {

    // The native platform-specific file object
    private final Object platformFile;

    /**
     * Constructor to initialize the FileSource.
     *
     * @param platformFile the platform-specific file object
     */
    public FileSource(Object platformFile) {
        this.platformFile = platformFile;
    }

    /**
     * Returns the platform-specific file object.
     *
     * @return the platform-specific file object
     */
    public Object getPlatformFile() {
        return platformFile;
    }

    /**
     * Abstract method to get the name of the file.
     * To be implemented by subclasses.
     *
     * @return the name of the file
     */
    abstract String _getName();

    // name property
    private ReadOnlyStringWrapper nameProperty;

    /**
     * Returns the name of the file.
     *
     * @return the name of the file
     */
    public final String getName() {
        return nameProperty == null ? _getName() : nameProperty.get();
    }

    /**
     * Returns the name property of the file.
     *
     * @return the ReadOnlyStringProperty for name
     */
    public final ReadOnlyStringProperty nameProperty() {
        if (nameProperty == null) {
            nameProperty = new ReadOnlyStringWrapper(this, "name", _getName());
        }
        return nameProperty.getReadOnlyProperty();
    }

    /**
     * Abstract method to get the size of the file.
     * To be implemented by subclasses.
     *
     * @return the size of the file
     */
    abstract long _getSize();

    // size property
    private ReadOnlyLongWrapper sizeProperty;

    /**
     * Returns the size of the file.
     *
     * @return the size of the file
     */
    public final long getSize() {
        return sizeProperty == null ? _getSize() : sizeProperty.get();
    }

    /**
     * Returns the size property of the file.
     *
     * @return the ReadOnlyLongProperty for size
     */
    public final ReadOnlyLongProperty sizeProperty() {
        if (sizeProperty == null) {
            sizeProperty = new ReadOnlyLongWrapper(this, "size", _getSize());
        }
        return sizeProperty.getReadOnlyProperty();
    }

    /**
     * Abstract method to get the object URL of the file.
     * To be implemented by subclasses.
     *
     * @return the object URL of the file
     */
    abstract String _getObjectURL();

    // objectURL property
    private ReadOnlyStringWrapper objectURLProperty;

    /**
     * Returns the object URL of the file.
     *
     * @return the object URL of the file
     */
    public final String getObjectURL() {
        return objectURLProperty == null ? _getObjectURL() : objectURLProperty.get();
    }

    /**
     * Returns the object URL property of the file.
     *
     * @return the ReadOnlyStringProperty for object URL
     */
    public final ReadOnlyStringProperty objectURLProperty() {
        if (objectURLProperty == null) {
            objectURLProperty = new ReadOnlyStringWrapper(this, "objectURL", _getObjectURL());
        }
        return objectURLProperty.getReadOnlyProperty();
    }

    /**
     * Gets the current upload progress.
     *
     * @return the current upload progress as a double value between 0.0 and 1.0
     */
    public abstract double getProgress();

    /**
     * Returns a read-only double property representing the current upload progress.
     *
     * @return the ReadOnlyDoubleProperty for the upload progress
     */
    public abstract ReadOnlyDoubleProperty progressProperty();

    /**
     * Retrieves the File object representing the uploaded file.
     *
     * @return the uploaded File object, or null if the file has not been uploaded yet
     */
    public abstract File getUploadedFile();

    /**
     * Returns a read-only object property representing the uploaded file.
     *
     * @return the ReadOnlyObjectProperty for the uploaded file
     */
    public abstract ReadOnlyObjectProperty<File> uploadedFileProperty();

    /**
     * Initiates the file upload process synchronously.
     * <p>
     * This method will start the upload operation and should be called to begin the upload.
     * </p>
     */
    public abstract void uploadFile();

    /**
     * Initiates the file upload process asynchronously.
     *
     * @return a CompletableFuture representing the result of the asynchronous upload operation,
     * which will complete with the uploaded File object
     */
    public abstract CompletableFuture<File> uploadFileAsync();
}
