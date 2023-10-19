package one.jpro.platform.file.example.editor;

import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.Styles;
import com.jpro.webapi.WebAPI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.FileSource;
import one.jpro.platform.file.dropper.FileDropper;
import one.jpro.platform.file.picker.FilePicker;
import one.jpro.platform.file.util.SaveUtils;
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
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * This class serves as sample of a text editor application that enables users to
 * open, edit, and save text files.
 * <p>
 *
 * Functionalities:
 * <ul>
 *     <li>FileDropper: Allows users to open text files by dragging and dropping them onto a designated area.</li>
 *     <li>File Picker: Provides an "Open" button that triggers a file picker dialog for selecting text files.</li>
 *     <li>Multi-File Support: Enables users to open either a single file or multiple files simultaneously.</li>
 *     <li>Text Area: Displays the contents of the opened text files in a text area for editing.</li>
 *     <li>Save Feature: Allows users to save the edited content back to a file.</li>
 *     <li>Reset: Provides an option to clear the text area, reverting it to the initial state.</li>
 * </ul>
 *
 * @author Besmir Beqiri
 */

public class TextEditorSample extends Application {

    private static final Logger logger = LoggerFactory.getLogger(TextEditorSample.class);

    private static final PseudoClass FILES_DRAG_OVER_PSEUDO_CLASS = PseudoClass.getPseudoClass("files-drag-over");
    private static final PseudoClass SUPPORTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("supported");
    private final ExtensionFilter textExtensionFilter = ExtensionFilter.of("Text files", ".txt", ".srt", ".md", ".csv");
    private final Random random = new Random();
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
        Label dropLabel = new Label("Drop here text files only!");
        StackPane dropPane = new StackPane(dropLabel);
        dropPane.getStyleClass().add("drop-pane");

        final var fileDropper = FileDropper.create(dropPane);
        fileDropper.setExtensionFilter(textExtensionFilter);
        fileDropper.filesDragOverProperty().addListener(observable ->
                dropPane.pseudoClassStateChanged(FILES_DRAG_OVER_PSEUDO_CLASS,
                        fileDropper.isFilesDragOver()));
        fileDropper.filesDragOverSupportedProperty().addListener(observable ->
                dropPane.pseudoClassStateChanged(SUPPORTED_PSEUDO_CLASS,
                        fileDropper.isFilesDragOverSupported()));

        BorderPane rootPane = new BorderPane(dropPane);
        rootPane.getStyleClass().add("root-pane");
        TextArea textArea = new TextArea();
        fileDropper.setOnFilesSelected(fileSources -> {
            appendFilesContent(fileSources, textArea);
            rootPane.setCenter(textArea);
        });

        Button openButton = new Button("Open", new FontIcon(Material2AL.FOLDER_OPEN));
        openButton.setDefaultButton(true);
        final var filePicker = FilePicker.create(openButton);
        filePicker.getExtensionFilters().add(textExtensionFilter);
        filePicker.setSelectedExtensionFilter(textExtensionFilter);
        filePicker.setOnFilesSelected(fileSources -> {
            appendFilesContent(fileSources, textArea);
            rootPane.setCenter(textArea);
        });

        ChoiceBox<SelectionMode> selectionModeComboBox = new ChoiceBox<>();
        selectionModeComboBox.getItems().addAll(SelectionMode.SINGLE, SelectionMode.MULTIPLE);
        selectionModeComboBox.getSelectionModel().select(SelectionMode.SINGLE);
        selectionModeComboBox.setConverter(selectionModeStringConverter);
        fileDropper.selectionModeProperty().bind(selectionModeComboBox.valueProperty());
        filePicker.selectionModeProperty().bind(selectionModeComboBox.valueProperty());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button clearButton = new Button("Clear", new FontIcon(Material2AL.CLEAR));
        clearButton.getStyleClass().add(Styles.DANGER);
        clearButton.disableProperty().bind(textArea.textProperty().isEmpty());
        clearButton.setOnAction(event -> {
            rootPane.setCenter(dropPane);
            textArea.clear();
        });

        Label selectionModeLabel = new Label("Selection Mode:");
        HBox controlsBox = new HBox(selectionModeLabel, selectionModeComboBox, spacer, openButton);
        controlsBox.getStyleClass().add("controls-box");

        if (WebAPI.isBrowser()) {
            Button downloadButton = new Button("Download", new FontIcon(Material2AL.CLOUD_DOWNLOAD));
            downloadButton.disableProperty().bind(textArea.textProperty().isEmpty());
            downloadButton.setOnAction(event ->
                    SaveUtils.download(stage, "subtitle_", ".srt", saveToFile(textArea)));
            controlsBox.getChildren().addAll(downloadButton, clearButton);
        } else {
            Button saveButton = new Button("Save", new FontIcon(Material2MZ.SAVE_ALT));
            saveButton.disableProperty().bind(textArea.textProperty().isEmpty());
            saveButton.setOnAction(event -> Optional.ofNullable(lastSavedFile)
                    .map(saveToFile(textArea))
                    .orElseGet(() -> SaveUtils.saveAs(stage, "subtitle_" + random.nextInt(),
                            ".srt", saveToFile(textArea)))
                    .thenAccept(file -> lastSavedFile = file));
            Button saveAsButton = new Button("Save As", new FontIcon(Material2MZ.SAVE));
            saveAsButton.disableProperty().bind(textArea.textProperty().isEmpty());
            saveAsButton.setOnAction(event -> SaveUtils.saveAs(stage, "subtitle_" + random.nextInt(),
                    ".srt", saveToFile(textArea)).thenAccept(file -> lastSavedFile = file));
            controlsBox.getChildren().addAll(saveButton, saveAsButton, clearButton);
        }

        rootPane.setTop(controlsBox);
        return rootPane;
    }

    private void appendFilesContent(List<? extends FileSource> fileSources, TextArea textArea) {
        final StringBuilder content = new StringBuilder();
        fileSources.forEach(fileSource -> fileSource.uploadFileAsync().thenAcceptAsync(file -> {
            try {
                String fileContent = new String(Files.readAllBytes(file.toPath()));
                content.append(fileContent);
                content.append("\n=================================================================================\n");
                Platform.runLater(() -> textArea.setText(content.toString()));
            } catch (IOException ex) {
                logger.error("Error reading file: " + ex.getMessage(), ex);
            }
        }));
    }

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

    private final StringConverter<SelectionMode> selectionModeStringConverter = new StringConverter<>() {

        @Override
        public String toString(SelectionMode selectionMode) {
            return selectionMode == SelectionMode.MULTIPLE ? "Multiple" : "Single";
        }

        @Override
        public SelectionMode fromString(String string) {
            return "Multiple".equals(string) ? SelectionMode.MULTIPLE : SelectionMode.SINGLE;
        }
    };
}
