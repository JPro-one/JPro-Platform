package one.jpro.platform.file.example.editor;

import atlantafx.base.theme.CupertinoLight;
import com.jpro.webapi.WebAPI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.FileSource;
import one.jpro.platform.file.dropper.FileDropper;
import one.jpro.platform.file.picker.FileOpenPicker;
import one.jpro.platform.file.picker.FileSavePicker;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * This class serves as sample of a text editor application that enables
 * users to open, edit, and save text files. They can either drag and drop
 * files onto the application or select them via a file picker dialog.
 * <p>
 * <p>
 * Functionalities:
 * <ul>
 *     <li>FileDropper: Allows users to open text files by dragging and dropping them onto a designated area.</li>
 *     <li>FilePicker: Provides an "Open" button that triggers a file picker dialog for selecting text files.</li>
 *     <li>TextArea: Displays the contents of the opened text files in a text area for editing.</li>
 *     <li>Save Feature: Allows users to save the edited content back to a file.</li>
 * </ul>
 *
 * @author Besmir Beqiri
 * @see FileDropper
 * @see FileOpenPicker
 */
public class TextEditorSample extends Application {

    private static final Logger logger = LoggerFactory.getLogger(TextEditorSample.class);

    private static final PseudoClass FILES_DRAG_OVER_PSEUDO_CLASS = PseudoClass.getPseudoClass("files-drag-over");
    private final ExtensionFilter textExtensionFilter = ExtensionFilter.of("Text files", ".txt", ".srt", ".md", ".csv");
    private File lastSavedFile;

    @Override
    public void start(Stage stage) {
        stage.setTitle("JPro File Dropper");
        Scene scene = new Scene(createRoot(stage), 1140, 640);
        Optional.ofNullable(CupertinoLight.class.getResource(new CupertinoLight().getUserAgentStylesheet()))
                .map(URL::toExternalForm)
                .ifPresent(scene::setUserAgentStylesheet);
        Optional.ofNullable(TextEditorSample.class.getResource("/one/jpro/platform/file/example/css/file_dropper.css"))
                .map(URL::toExternalForm)
                .ifPresent(scene.getStylesheets()::add);
        stage.setScene(scene);
        stage.show();
    }

    public Parent createRoot(Stage stage) {
        Label dropLabel = new Label("Open or drop " + textExtensionFilter.description().toLowerCase() + " here!");
        StackPane dropPane = new StackPane(dropLabel);
        dropPane.getStyleClass().add("drop-pane");

        TextArea textArea = new TextArea();
        StackPane contentPane = new StackPane(textArea, dropPane);

        FileDropper fileDropper = FileDropper.create(contentPane);
        fileDropper.setExtensionFilter(textExtensionFilter);
        fileDropper.setOnDragEntered(event -> {
            dropPane.pseudoClassStateChanged(FILES_DRAG_OVER_PSEUDO_CLASS, true);
            contentPane.getChildren().setAll(textArea, dropPane);
        });
        fileDropper.setOnDragExited(event ->
                dropPane.pseudoClassStateChanged(FILES_DRAG_OVER_PSEUDO_CLASS, false));
        fileDropper.setOnFilesSelected(fileSources -> {
            openFile(fileSources, textArea);
            contentPane.getChildren().setAll(textArea);
        });

        Button openButton = new Button("Open", new FontIcon(Material2AL.FOLDER_OPEN));
        openButton.setDefaultButton(true);
        FileOpenPicker fileOpenPicker = FileOpenPicker.create(openButton);
        fileOpenPicker.setSelectedExtensionFilter(textExtensionFilter);
        fileOpenPicker.setOnFilesSelected(fileSources -> {
            openFile(fileSources, textArea);
            contentPane.getChildren().setAll(textArea);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox controlsBox = new HBox(openButton, spacer);
        controlsBox.getStyleClass().add("controls-box");

        final Button retriveButton;
        if (WebAPI.isBrowser()) {
            retriveButton = new Button("Download", new FontIcon(Material2AL.CLOUD_DOWNLOAD));
            controlsBox.getChildren().add(retriveButton);
        } else {
            Button saveButton = new Button("Save", new FontIcon(Material2MZ.SAVE_ALT));
            saveButton.disableProperty().bind(textArea.textProperty().isEmpty());
            saveButton.setOnAction(event -> saveToFile(textArea).apply(lastSavedFile));
            retriveButton = new Button("Save As", new FontIcon(Material2MZ.SAVE));
            controlsBox.getChildren().addAll(saveButton, retriveButton);
        }
        retriveButton.disableProperty().bind(textArea.textProperty().isEmpty());
        FileSavePicker fileSavePicker = FileSavePicker.create(retriveButton);
        fileSavePicker.setInitialFileName("subtitle");
        fileSavePicker.setSelectedExtensionFilter(ExtensionFilter.of("Subtitle format (.srt)", ".srt"));
        fileSavePicker.setOnFileSelected(file -> saveToFile(textArea).apply(file)
                .thenApply(saveToFile -> lastSavedFile = saveToFile));

        BorderPane rootPane = new BorderPane(contentPane);
        rootPane.getStyleClass().add("root-pane");
        rootPane.setTop(controlsBox);
        return rootPane;
    }

    /**
     * Opens a file and sets the content of the file to the specified TextArea.
     *
     * @param fileSources a list of FileSources that represent the files to be opened
     * @param textArea    the TextArea where the content of the file will be displayed
     */
    private void openFile(List<? extends FileSource> fileSources, TextArea textArea) {
        fileSources.stream().findFirst().ifPresentOrElse(fileSource ->
                fileSource.uploadFileAsync().thenCompose(file -> {
                    try {
                        final String fileContent = new String(Files.readAllBytes(file.toPath()));
                        Platform.runLater(() -> textArea.setText(fileContent));
                        return CompletableFuture.completedFuture(file);
                    } catch (IOException ex) {
                        logger.error("Error reading file: " + ex.getMessage(), ex);
                        return CompletableFuture.failedFuture(ex);
                    }
                }).thenApply(file -> lastSavedFile = file), () -> logger.warn("No file selected"));
    }

    /**
     * Saves the content of a TextArea to the provided file.
     *
     * @param textArea the {@link TextArea} containing the content to save.
     * @return A function accepting a File, saving its contents, and returning a {@link CompletableFuture}.
     */
    private Function<File, CompletableFuture<File>> saveToFile(TextArea textArea) {
        return file -> CompletableFuture.supplyAsync(() -> {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(textArea.getText().getBytes());
            } catch (IOException ex) {
                logger.error("Error writing file: " + ex.getMessage(), ex);
            }
            return file;
        });
    }
}
