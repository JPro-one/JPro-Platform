package one.jpro.platform.file.example;

import atlantafx.base.theme.CupertinoLight;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.picker.FilePicker;

import java.net.URL;
import java.util.Optional;

/**
 * This class represents a sample application that handles file operations.
 *
 * @author Besmir Beqiri
 */
public class FileHandlerSample extends Application {

    private static final ExtensionFilter textExtensionFilter = ExtensionFilter.of("TEXT files", "*.txt", "*.srt", "*.md", "*.csv");
    private static final ExtensionFilter audioExtensionFilter = ExtensionFilter.of("Audio files", "*.mp3", "*.wav", "*.ogg");
    private static final ExtensionFilter videoExtensionFilter = ExtensionFilter.of("Video files", "*.mp4", "*.avi", "*.mkv");
    private static final ExtensionFilter imageExtensionFilter = ExtensionFilter.of("Image files", "*.png", "*.jpg", "*.jpeg");

    private final FileTableView fileTableView = new FileTableView();
    private final Button importButton = new Button("Import");

    @Override
    public void start(Stage stage) {
        stage.setTitle("JPro File Handler");
        Scene scene = new Scene(createRoot(stage), 1140, 640);
        Optional.ofNullable(CupertinoLight.class.getResource(new CupertinoLight().getUserAgentStylesheet()))
                .map(URL::toExternalForm)
                .ifPresent(scene::setUserAgentStylesheet);
        Optional.ofNullable(FileHandlerSample.class.getResource("css/main.css"))
                .map(URL::toExternalForm)
                .ifPresent(scene.getStylesheets()::add);
        stage.setScene(scene);
        stage.show();
    }

    public Parent createRoot(Stage stage) {
//        final FilePicker<?> filePicker = filePickerFromTablePlaceHolder();
        final FilePicker<?> filePicker = filePickerFromImportButton();
        filePicker.getExtensionFilters().addAll(textExtensionFilter,
                audioExtensionFilter, videoExtensionFilter, imageExtensionFilter);
        filePicker.setSelectedExtensionFilter(audioExtensionFilter);
        filePicker.setOnFilesSelected(fileSources -> fileTableView.getItems().addAll(fileSources));

        CheckBox multipleCheckBox = new CheckBox("Multiple");
        multipleCheckBox.setOnAction(event -> filePicker.setSelectionMode(multipleCheckBox.isSelected() ?
                SelectionMode.MULTIPLE : SelectionMode.SINGLE));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button clearButton = new Button("Clear");
        clearButton.setOnAction(event -> fileTableView.getItems().clear());
        HBox controlsBox = new HBox(multipleCheckBox, spacer, importButton, clearButton);
        controlsBox.getStyleClass().add("controls-box");
        VBox rootPane = new VBox(controlsBox, fileTableView);
        rootPane.getStyleClass().add("root-pane");
        VBox.setVgrow(fileTableView, Priority.ALWAYS);
        return rootPane;
    }

    private FilePicker<?> filePickerFromImportButton() {
        importButton.setDefaultButton(true);
        return FilePicker.create(importButton);
    }

    private FilePicker<?> filePickerFromTablePlaceHolder() {
        final var clickOnMeLabel = new Label("Click on me to open the file picker");
        StackPane tablePlaceHolder = new StackPane(clickOnMeLabel);
        fileTableView.setPlaceholder(tablePlaceHolder);
        return FilePicker.create(tablePlaceHolder);
    }
}
