package one.jpro.platform.file.example.upload;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import one.jpro.platform.file.FileSource;

import java.io.File;

/**
 * TableCell implementation that displays an "Upload" button for each cell.
 * Clicking the button triggers the uploadFile() method on the corresponding item.
 *
 * @param <S> The type of the TableView items.
 * @author Besmir Beqiri
 */
public class UploadButtonTableCell<S extends FileSource> extends TableCell<S, File> {

    private final Button startUploadButton;

    /**
     * Constructs a custom table cell containing an upload button, that when is clicked,
     * the selected item's uploadFileAsync() method is called to asynchronously to upload a file.
     * Once the upload is completed, the button's text is updated to "Completed" and it is disabled.
     */
    public UploadButtonTableCell() {
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setAlignment(Pos.CENTER);

        startUploadButton = new Button("Start upload");
        startUploadButton.setOnAction(event -> {
            final var selectedItem = getTableView().getItems().get(getIndex());
            if (selectedItem != null) {
                // start the upload asynchronously
                selectedItem.uploadFileAsync().thenAccept(file ->
                        // update the button text when the upload is completed
                        Platform.runLater(() -> {
                            startUploadButton.setDisable(true);
                            startUploadButton.setText("Completed");
                        }));
            }
        });
    }

    @Override
    protected void updateItem(File file, boolean empty) {
        super.updateItem(file, empty);

        if (empty) {
            setGraphic(null);
        } else {
            setGraphic(startUploadButton);
        }
    }
}
