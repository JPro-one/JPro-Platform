package one.jpro.platform.file.example.upload;

import atlantafx.base.theme.Styles;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import one.jpro.platform.file.FileSource;

/**
 * Represents a custom table cell used to display an upload progress indicator.
 * After calling {@link FileSource#uploadFile()} method on the corresponding item,
 * the progress bar will indicate the current uploading progress.
 *
 * @param <S> The type of the TableView items.
 * @author Besmir Beqiri
 */
public class UploadIndicatorTableCell<S extends FileSource> extends TableCell<S, Double> {

    private final ProgressBar progressBar;

    public UploadIndicatorTableCell() {
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        progressBar = new ProgressBar();
        progressBar.getStyleClass().add(Styles.MEDIUM);
    }

    @Override
    protected void updateItem(Double progress, boolean empty) {
        super.updateItem(progress, empty);

        if (progress == null || empty) {
            progressBar.setProgress(0.0);
            setGraphic(null);
        } else {
            progressBar.setProgress(progress);
            setGraphic(progressBar);
        }
    }
}
