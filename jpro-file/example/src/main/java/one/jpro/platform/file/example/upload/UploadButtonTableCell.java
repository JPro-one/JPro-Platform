package one.jpro.platform.file.example.upload;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import one.jpro.platform.file.FileSource;
import one.jpro.platform.file.UploadStatus;

/**
 * TableCell with a button that starts, cancels or retries the upload of the row's file,
 * depending on its {@link UploadStatus}.
 *
 * @param <S> The type of the TableView items.
 * @author Besmir Beqiri
 */
public class UploadButtonTableCell<S extends FileSource> extends TableCell<S, UploadStatus> {

    private final Button button;

    public UploadButtonTableCell() {
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setAlignment(Pos.CENTER);

        button = new Button();
        button.setOnAction(event -> {
            final var item = getTableView().getItems().get(getIndex());
            if (item == null) return;
            if (item.getUploadStatus() == UploadStatus.UPLOADING) {
                item.cancelUpload();
            } else {
                item.uploadFileAsync();
            }
        });
    }

    @Override
    protected void updateItem(UploadStatus status, boolean empty) {
        super.updateItem(status, empty);

        if (empty || status == null) {
            setGraphic(null);
            return;
        }
        setGraphic(button);
        button.setDisable(false);
        switch (status) {
            case NOT_STARTED -> button.setText("Start upload");
            case UPLOADING -> button.setText("Cancel");
            case FAILED, CANCELLED -> button.setText("Retry");
            case COMPLETED -> {
                button.setText("Completed");
                button.setDisable(true);
            }
        }
    }
}
