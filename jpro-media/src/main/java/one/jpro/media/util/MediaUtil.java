package one.jpro.media.util;

import com.jpro.webapi.WebAPI;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import one.jpro.media.MediaSource;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Media utility class.
 *
 * @author Besmir Beqiri
 */
public final class MediaUtil {

    /**
     * Retrieve the media file from the given media source.
     *
     * @param stage application primary stage
     * @param mediaSource the media source
     * @param fileName the file name without extension
     * @return a {@link MediaSource} object referring the new resource if the application runs on a desktop/device.
     * When the application runs on the web (via JPro), it returns the same media source as the one provided as an
     * input parameter. Otherwise, if an error occurs, <code>null</code> is returned.
     * is returned
     * @throws IOException if an I/O error occurs
     */
    public static MediaSource retrieve(Stage stage, MediaSource mediaSource, String fileName) throws IOException {
        if (WebAPI.isBrowser()) {
            WebAPI webAPI = WebAPI.getWebAPI(stage);
            if (!mediaSource.isLocal()) {
                final WebAPI.JSFile jsFile = mediaSource.jsFile();
                if (jsFile != null) {
                    webAPI.executeScript("""
                    let download_link = document.createElement("a");
                    download_link.setAttribute("download", "$fileName.webm");
                    download_link.href = %s;
                    download_link.click();
                    """.formatted(jsFile.getObjectURL().getName())
                            .replace("$fileName.webm", fileName));
                }
                return mediaSource;
            }
        } else {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save media file...");
            final Optional<File> optionalFile = mediaSource.file();
            if (optionalFile.isPresent()) {
                final File mediaFile = optionalFile.get();
                final String initialFileName = getExtension(mediaFile.getName())
                        .map(ext -> fileName + "." + ext)
                        .orElseGet(() -> mediaFile.toPath().getFileName().toString());
                fileChooser.setInitialFileName(initialFileName);
                // Show save dialog
                final File saveToFile = fileChooser.showSaveDialog(stage);
                if (saveToFile != null) {
                    Files.copy(mediaFile.toPath(), saveToFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    return new MediaSource(saveToFile.toURI().toString());
                }
            }
        }

        return null;
    }

    /**
     * Get the file extension.
     *
     * @param fileName the file name
     * @return a string containing the file extension
     */
    public static Optional<String> getExtension(String fileName) {
        return Optional.ofNullable(fileName)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(fileName.lastIndexOf(".") + 1));
    }

    private MediaUtil() {
        // hide default constructor
    }
}
