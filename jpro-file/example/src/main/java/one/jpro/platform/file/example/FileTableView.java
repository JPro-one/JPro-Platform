package one.jpro.platform.file.example;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import one.jpro.platform.file.FileSource;

import java.io.File;

/**
 * A custom TableView for displaying FileSource objects.
 * It displays the file name, file size, upload progress, and provides a button to start the upload.
 *
 * @author Besmir Beqiri
 */
public class FileTableView extends TableView<FileSource<?>> {

    private static final String DEFAULT_STYLE_CLASS = "file-source-table-view";
    private static final String FILE_NAME_COLUMN = "File Name";
    private static final String FILE_SIZE_COLUMN = "File Size";

    @SuppressWarnings("unchecked")
    public FileTableView() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<FileSource<?>, String> fileNameColumn = new TableColumn<>(FILE_NAME_COLUMN);
        fileNameColumn.setCellValueFactory(data -> data.getValue().nameProperty());

        TableColumn<FileSource<?>, Long> fileSizeColumn = new TableColumn<>(FILE_SIZE_COLUMN);
        fileSizeColumn.setCellValueFactory(data -> data.getValue().sizeProperty().asObject());
        fileSizeColumn.setCellFactory(col -> new FileSizeTableCell<>());
        fileSizeColumn.setMaxWidth(80.0);

        TableColumn<FileSource<?>, Double> uploadProgressColumn = new TableColumn<>("Upload progress");
        uploadProgressColumn.setCellValueFactory(data -> data.getValue().progressProperty().asObject());
        uploadProgressColumn.setCellFactory(col -> new UploadIndicatorTableCell<>());
        uploadProgressColumn.setMaxWidth(120.0);

        TableColumn<FileSource<?>, File> uploadButtonColumn = new TableColumn<>("Start upload");
        uploadButtonColumn.setCellValueFactory(data -> data.getValue().uploadedFileProperty());
        uploadButtonColumn.setCellFactory(col -> new UploadButtonTableCell<>());
        uploadButtonColumn.setMaxWidth(120.0);

        getColumns().addAll(fileNameColumn, fileSizeColumn, uploadProgressColumn, uploadButtonColumn);
    }
}
