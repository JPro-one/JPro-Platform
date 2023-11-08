package one.jpro.platform.file.util;

import com.jpro.webapi.WebAPI;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.FileStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Provides utility methods for saving and downloading files.
 *
 * @author Besmir Beqiri
 */
public interface SaveUtils {

    /**
     * Saves a file using the provided save function.
     * This method should be used only for desktop/native applications.
     *
     * @param fileToSave   the file to be saved
     * @param fileType     the file extension to be appended to the file name if it does not already have it
     * @param saveFunction the save function to be used to save the file
     * @return a CompletableFuture representing the asynchronous operation of saving the file
     * @throws NullPointerException if the fileToSave parameter is null
     */
    static CompletableFuture<File> save(@NotNull final File fileToSave,
                                        @Nullable String fileType,
                                        @NotNull final Function<File, CompletableFuture<File>> saveFunction) {
        Objects.requireNonNull(fileToSave, "File to save to cannot be null");
        if (fileType == null) {
            fileType = "";
        }

        String filePath = fileToSave.getAbsolutePath();
        final int lastDotIndex = filePath.lastIndexOf(".");
        if (lastDotIndex > 0) {
            filePath = filePath.substring(0, lastDotIndex) + fileType;
        }
        return saveFunction.apply(new File(filePath));
    }

    /**
     * Saves a file with the given name and type using the provided save function.
     * This method should be used only for desktop/native applications.
     *
     * @param stage        The stage where the save dialog will be shown.
     * @param fileName     The initial file name.
     * @param fileType     The file type (extension) of the file to save.
     * @param saveFunction The function to be called to save the file. This function takes a File parameter
     *                     representing the file to save and returns a {@link CompletableFuture<File>} that will
     *                     complete with the saved file.
     * @return A {@link CompletableFuture<File>} that will complete with the saved file if the user selects a file
     * to save, or fail with a NullPointerException if the user cancels the save operation.
     */
    static CompletableFuture<File> saveAs(@NotNull final Stage stage,
                                          @NotNull final String fileName,
                                          @Nullable final String fileType,
                                          @NotNull final Function<File, CompletableFuture<File>> saveFunction) {
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
     * or fail with an exception if the download fails or if the operation is not supported in the current environment.
     * @throws UnsupportedOperationException If the download operation is not supported in the current environment.
     */
    static CompletableFuture<File> download(@NotNull final Stage stage,
                                            @NotNull final String fileName,
                                            @Nullable final String fileType,
                                            @NotNull final Function<File, CompletableFuture<Void>> saveFunction) throws IOException {
        return download(stage, FileStorage.JPRO_TMP_DIR.toFile(), fileName, fileType, saveFunction);
    }

    /**
     * Downloads a file with the given name and type using the provided save function.
     *
     * @param stage        The stage is needed to initialise the WebAPI.
     *                     This is required to show the download dialog in a browser environment.
     * @param tmpDir       The temporary directory where the file will be saved before being offered for download.
     * @param fileName     The name of the file to be downloaded.
     * @param fileType     The file type (extension) of the file to be downloaded.
     * @param saveFunction The function to be called to save the file. This function takes a File parameter
     *                     representing the file to save and returns a {@link CompletableFuture<File>} that will
     *                     complete with the saved file.
     * @return A {@link CompletableFuture<Void>} that will complete with the downloaded file if the download is successful,
     * or fail with an exception if the download fails or if the operation is not supported in the current environment.
     * @throws UnsupportedOperationException If the download operation is not supported in the current environment.
     */
    static CompletableFuture<File> download(@NotNull final Stage stage,
                                            @NotNull final File tmpDir,
                                            @NotNull final String fileName,
                                            @Nullable final String fileType,
                                            @NotNull final Function<File, CompletableFuture<Void>> saveFunction) throws IOException {
        if (WebAPI.isBrowser()) {
            WebAPI webAPI = WebAPI.getWebAPI(stage);
            final Logger logger = LoggerFactory.getLogger(SaveUtils.class);
            final File tempFile = FileStorage.createTempFile(tmpDir.toPath(), fileName, fileType).toFile();
            return saveFunction.apply(tempFile).thenCompose(nothing -> {
                try {
                    final URL fileUrl = tempFile.toURI().toURL();
                    Platform.runLater(() -> webAPI.downloadURL(fileUrl, tempFile::delete));
                    return CompletableFuture.completedFuture(tempFile);
                } catch (IOException ex) {
                    return CompletableFuture.failedFuture(ex);
                }
            }).exceptionallyCompose(ex -> {
                if (!tempFile.delete()) {
                    logger.warn("Could not delete temporary file {}", tempFile.getAbsolutePath());
                }
                logger.error("Error while downloading file", ex);
                return CompletableFuture.failedFuture(ex);
            });
        } else {
            throw new UnsupportedOperationException("Download is only supported in the browser");
        }
    }
}
