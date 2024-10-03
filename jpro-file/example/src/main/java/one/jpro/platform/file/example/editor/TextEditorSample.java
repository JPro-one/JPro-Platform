package one.jpro.platform.file.example.editor;

import atlantafx.base.theme.CupertinoLight;
import com.jpro.webapi.WebAPI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import org.apache.commons.io.FilenameUtils;
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
 *     <li>FileOpenPicker: Provides an "Open" button that triggers a file picker dialog for selecting text files.</li>
 *     <li>TextArea: Displays the contents of the opened text files in a text area for editing.</li>
 *     <li>FileSavePicker: Allows users to save the edited content back to a file.</li>
 * </ul>
 *
 * @author Besmir Beqiri
 * @see FileDropper
 * @see FileOpenPicker
 * @see FileSavePicker
 */
public class TextEditorSample extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextEditorSample.class);

    private static final PseudoClass FILES_DRAG_OVER_PSEUDO_CLASS = PseudoClass.getPseudoClass("files-drag-over");
    private static final ExtensionFilter TEXT_EXTENSION_FILTER = ExtensionFilter.of("Text files", ".txt", ".srt", ".md", ".csv");
    private final ObjectProperty<File> lastOpenedFile = new SimpleObjectProperty<>(this, "lastOpenedFile");

    @Override
    public void start(Stage stage) {
        stage.setTitle("JPro Text Editor");
        Scene scene = new Scene(createRoot(stage), 1140, 640);
        Optional.ofNullable(CupertinoLight.class.getResource(new CupertinoLight().getUserAgentStylesheet()))
                .map(URL::toExternalForm)
                .ifPresent(scene::setUserAgentStylesheet);
        scene.getStylesheets().add(TextEditorSample.class
                .getResource("/one/jpro/platform/file/example/css/text_editor.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    public Parent createRoot(Stage stage) {
        Label dropLabel = new Label("Drop " + TEXT_EXTENSION_FILTER.description().toLowerCase() + " here!");
        StackPane dropPane = new StackPane(dropLabel);
        dropPane.getStyleClass().add("drop-pane");

        TextArea textArea = new TextArea();
        StackPane contentPane = new StackPane(textArea, dropPane);

        FileDropper fileDropper = FileDropper.create(contentPane);
        fileDropper.setExtensionFilter(TEXT_EXTENSION_FILTER);
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

        Button newButton = new Button("New", new FontIcon(Material2AL.INSERT_DRIVE_FILE));
        newButton.setDefaultButton(true);
        newButton.setOnAction(event -> {
            textArea.clear();
            contentPane.getChildren().setAll(textArea);
        });

        Button openButton = new Button("Open", new FontIcon(Material2AL.FOLDER_OPEN));
        FileOpenPicker fileOpenPicker = FileOpenPicker.create(openButton);
        fileOpenPicker.setSelectedExtensionFilter(TEXT_EXTENSION_FILTER);
        fileOpenPicker.setOnFilesSelected(fileSources -> {
            openFile(fileSources, textArea);
            contentPane.getChildren().setAll(textArea);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox controlsBox = new HBox(newButton, openButton, spacer);
        controlsBox.getStyleClass().add("controls-box");

        final Button saveAsButton;
        if (WebAPI.isBrowser()) {
            saveAsButton = new Button("Download", new FontIcon(Material2AL.CLOUD_DOWNLOAD));
            controlsBox.getChildren().add(saveAsButton);
        } else {
            Button saveButton = new Button("Save", new FontIcon(Material2MZ.SAVE_ALT));
            saveButton.disableProperty().bind(textArea.textProperty().isEmpty());
            saveButton.setOnAction(event -> saveToFile(textArea).apply(lastOpenedFile.get()));
            saveAsButton = new Button("Save As", new FontIcon(Material2MZ.SAVE));
            controlsBox.getChildren().addAll(saveButton, saveAsButton);
        }
        saveAsButton.disableProperty().bind(textArea.textProperty().isEmpty());

        FileSavePicker fileSavePicker = FileSavePicker.create(saveAsButton);
        fileSavePicker.initialFileNameProperty().bind(lastOpenedFile.map(file ->
                FilenameUtils.getName(file.getName())).orElse("subtitle"));
        fileSavePicker.initialDirectoryProperty().bind(lastOpenedFile.map(File::getParentFile));
        fileSavePicker.setSelectedExtensionFilter(TEXT_EXTENSION_FILTER);
        fileSavePicker.setOnFileSelected(file -> saveToFile(textArea).apply(file));

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
        fileSources.stream().findFirst().ifPresentOrElse(fileSource -> // Set the last opened file
                fileSource.uploadFileAsync()
                        .thenCompose(file -> {
                            try {
                                final String fileContent = new String(Files.readAllBytes(file.toPath()));
                                Platform.runLater(() -> textArea.setText(fileContent));
                                return CompletableFuture.completedFuture(file);
                            } catch (IOException ex) {
                                LOGGER.error("Error reading file: {}", file.getAbsolutePath(), ex);
                                return CompletableFuture.failedFuture(ex);
                            }
                        }).thenAccept(lastOpenedFile::set), () -> LOGGER.warn("No file selected"));
    }

    /**
     * Saves the content of a TextArea to the provided file.
     *
     * @param textArea the {@link TextArea} containing the content to save.
     * @return A function accepting a File, saving its contents, and returning a {@link CompletableFuture}.
     */
    private Function<File, CompletableFuture<Void>> saveToFile(TextArea textArea) {
        return file -> CompletableFuture.runAsync(() -> {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(textArea.getText().getBytes());
            } catch (IOException ex) {
                LOGGER.error("Error writing file: {}", file.getAbsolutePath(), ex);
            }
            lastOpenedFile.set(file);
            LOGGER.info("Saved to file: {}", file.getAbsolutePath());
        });
    }
}
