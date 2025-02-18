package one.jpro.platform.file.picker;

import com.jpro.webapi.WebAPI;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Represents a {@link FileSavePicker} implementation for JavaFX applications
 * running on the web via JPro server. This class specializes for downloading
 * a file and save it in the native file system.
 *
 * @author Besmir Beqiri
 */
public class WebFileSavePicker extends BaseFileSavePicker {

    private static final Logger logger = LoggerFactory.getLogger(WebFileSavePicker.class);

    public WebFileSavePicker(Node node) {
        super(node);
    }

    // temp directory property
    private ObjectProperty<File> tempDirectory;

    /**
     * Returns the temporary directory.
     *
     * @return the temporary directory as a File object.
     */
    public final File getTempDirectory() {
        return (tempDirectory != null) ? tempDirectory.get() : null;
    }

    /**
     * Sets the temporary directory.
     *
     * @param value the temporary directory as a File object.
     */
    public final void setTempDirectory(final File value) {
        tempDirectoryProperty().set(value);
    }

    /**
     * Retrieves the property that represents the temporary directory
     * where the files will be saved before offered for downloading.
     */
    public final ObjectProperty<File> tempDirectoryProperty() {
        if (tempDirectory == null) {
            tempDirectory = new SimpleObjectProperty<>(this, "tempDirectory");
        }
        return tempDirectory;
    }

    @Override
    final void showDialog() {
        final String fileName = getInitialFileName() == null ? "filename" : getInitialFileName();
        final ExtensionFilter extensionFilter = findSelectedFilter();
        final String fileType = extensionFilter == null ? "" : extensionFilter.extensions().get(0);
        final Function<File, CompletableFuture<Void>> onFileSelected = getOnFileSelected();
        if (onFileSelected != null) {
            try {
                final Path tmpDir = getTempDirectory() != null ? getTempDirectory().toPath() : null;
                final File tempFile = FileStorage.createTempFile(tmpDir, fileName, fileType).toFile();
                onFileSelected.apply(tempFile)
                        .thenCompose(nothing -> {
                            try {
                                final URL fileUrl = tempFile.toURI().toURL();
                                final WebAPI webAPI = WebAPI.getWebAPI(getNode().getScene().getWindow());
                                Platform.runLater(() -> webAPI.downloadURL(fileUrl, tempFile::delete));
                                return CompletableFuture.completedFuture(nothing);
                            } catch (IOException ex) {
                                return CompletableFuture.failedFuture(ex);
                            }
                        }).exceptionallyCompose(ex -> {
                            if (!tempFile.delete()) {
                                logger.warn("Could not delete temporary file {}", tempFile.getAbsolutePath());
                            }
                            return CompletableFuture.failedFuture(ex);
                        });
            } catch (IOException ex) {
                logger.error("Error creating temporary file for download", ex);
            }
        }
    }
}
