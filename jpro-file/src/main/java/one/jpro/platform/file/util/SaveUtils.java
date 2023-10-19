package one.jpro.platform.file.util;

import com.jpro.webapi.WebAPI;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import one.jpro.platform.file.ExtensionFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Save/download utilities.
 *
 * @author Besmir Beqiri
 */
public interface SaveUtils {

    /**
     * Saves a file with the given name and type using the provided save function.
     *
     * @param stage       The stage where the save dialog will be shown.
     * @param fileName    The initial file name.
     * @param fileType    The file type (extension) of the file to save.
     * @param saveFunction The function to be called to save the file. This function takes a File parameter
     *                    representing the file to save and returns a {@link CompletableFuture<File>} that will
     *                    complete with the saved file.
     * @return A {@link CompletableFuture<File>} that will complete with the saved file if the user selects a file
     *         to save, or fail with a NullPointerException if the user cancels the save operation.
     */
    static CompletableFuture<File> saveAs(Stage stage, String fileName, String fileType,
                       Function<File, CompletableFuture<File>> saveFunction) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save file as...");

        fileChooser.setInitialFileName(fileName);
        ExtensionFilter extensionFilter = ExtensionFilter.of(fileType, fileType);
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(extensionFilter.description(),
                extensionFilter.extensions().stream().map(ext -> "*" + ext).toArray(String[]::new));
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setSelectedExtensionFilter(extFilter);

        // Show save dialog
        File saveToFile = fileChooser.showSaveDialog(stage);
        if (saveToFile != null) {
            return saveFunction.apply(saveToFile);
        } else {
            return CompletableFuture.failedFuture(new NullPointerException("File to save to is null"));
        }
    }

    /**
     * Downloads a file with the given name and type using the provided save function.
     *
     * @param stage        The stage is needed to initialise the WebAPI.
     *                     This is required to show the download dialog in a browser environment.
     * @param fileName     The name of the file to be downloaded.
     * @param fileType     The file type (extension) of the file to be downloaded.
     * @param saveFunction The function to be called to save the file. This function takes a File parameter
     *                     representing the file to save and returns a {@link CompletableFuture<File>} that will
     *                     complete with the saved file.
     * @return A {@link CompletableFuture<File>} that will complete with the downloaded file if the download is successful,
     *         or fail with an exception if the download fails or if the operation is not supported in the current environment.
     * @throws UnsupportedOperationException If the download operation is not supported in the current environment.
     */
    static CompletableFuture<File> download(Stage stage, String fileName, String fileType,
                         Function<File, CompletableFuture<File>> saveFunction) {
        final Logger logger = LoggerFactory.getLogger(SaveUtils.class);
        if (WebAPI.isBrowser()) {
            WebAPI webAPI = WebAPI.getWebAPI(stage);
            try {
                File tempFile = File.createTempFile(fileName, fileType);
                return saveFunction.apply(tempFile).thenCompose(file -> {
                    try {
                        final URL fileUrl = tempFile.toURI().toURL();
                        Platform.runLater(() -> webAPI.downloadURL(fileUrl, tempFile::delete));
                        return CompletableFuture.completedFuture(file);
                    } catch (MalformedURLException ex) {
                        return CompletableFuture.failedFuture(ex);
                    }
                }).exceptionallyCompose(ex -> {
                    if (!tempFile.delete()) {
                        logger.warn("Could not delete temporary file {}", tempFile.getAbsolutePath());
                    }
                    logger.error("Error while downloading file", ex);
                    return CompletableFuture.failedFuture(ex);
                });
            } catch (IOException ex) {
                return CompletableFuture.failedFuture(ex);
            }
        } else {
            throw new UnsupportedOperationException("Download is only supported in the browser");
        }
    }
}
