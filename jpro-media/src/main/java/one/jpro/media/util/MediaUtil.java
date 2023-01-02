package one.jpro.media.util;

import com.jpro.webapi.WebAPI;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import one.jpro.media.MediaSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Media utility class.
 *
 * @author Besmir Beqiri
 */
public final class MediaUtil {

    /**
     * Retrieve the media file from the given media source.
     *
     * @param stage Application primary stage.
     * @param mediaSource The media source.
     * @throws IOException if an I/O error occurs
     */
    public static void retrieve(Stage stage, MediaSource mediaSource) throws IOException {
        if (WebAPI.isBrowser()) {
            WebAPI webAPI = WebAPI.getWebAPI(stage);
            if (!mediaSource.isLocal()) {
                final WebAPI.JSFile jsFile = mediaSource.jsFile();
                if (jsFile != null) {
                    webAPI.executeScript("""
                    let download_link = document.createElement("a");
                    download_link.setAttribute("download", "RecordedVideo.webm");
                    download_link.href = %s;
                    download_link.click();
                    """.formatted(jsFile.getObjectURL().getName()));
                }
            }
        } else {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save As...");
            fileChooser.setInitialFileName(Path.of(mediaSource.source()).getFileName().toString());
            // Show save dialog
            final File saveToFile = fileChooser.showSaveDialog(stage);
            if (saveToFile != null) {
                Files.copy(Path.of(mediaSource.source()), saveToFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private MediaUtil() {
        // hide default constructor
    }
}
