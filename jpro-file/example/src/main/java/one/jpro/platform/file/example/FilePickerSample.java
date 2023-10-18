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
 * This class represents a sample application for file picker operations.
 *
 * @author Besmir Beqiri
 */
public class FilePickerSample extends Application {

    private static final ExtensionFilter textExtensionFilter = ExtensionFilter.of("TEXT files", "*.txt", "*.srt", "*.md", "*.csv");
    private static final ExtensionFilter audioExtensionFilter = ExtensionFilter.of("Audio files", "*.mp3", "*.wav", "*.ogg");
    private static final ExtensionFilter videoExtensionFilter = ExtensionFilter.of("Video files", "*.mp4", "*.avi", "*.mkv");
    private static final ExtensionFilter imageExtensionFilter = ExtensionFilter.of("Image files", "*.png", "*.jpg", "*.jpeg");

    @Override
    public void start(Stage stage) {
        stage.setTitle("JPro File Picker");
        var scene = new Scene(createRoot(stage), 1140, 640);
        Optional.ofNullable(CupertinoLight.class.getResource(new CupertinoLight().getUserAgentStylesheet()))
                .map(URL::toExternalForm)
                .ifPresent(scene::setUserAgentStylesheet);
        Optional.ofNullable(FilePickerSample.class.getResource("css/file_picker.css"))
                .map(URL::toExternalForm)
                .ifPresent(scene.getStylesheets()::add);
        stage.setScene(scene);
        stage.show();
    }

    public Parent createRoot(Stage stage) {
        Label clickOnMeLabel = new Label("Click on me to open the file picker!");
        StackPane placeholderPane = new StackPane(clickOnMeLabel);
        placeholderPane.getStyleClass().add("placeholder-pane");
        FileTableView fileTableView = new FileTableView();
        fileTableView.setPlaceholder(placeholderPane);

        final var filePicker = FilePicker.create(placeholderPane);
        filePicker.getExtensionFilters().addAll(textExtensionFilter,
                audioExtensionFilter, videoExtensionFilter, imageExtensionFilter);
        filePicker.setSelectedExtensionFilter(audioExtensionFilter);
        filePicker.setOnFilesSelected(fileSources -> fileTableView.getItems().addAll(fileSources));

        BorderPane rootPane = new BorderPane(fileTableView);
        rootPane.getStyleClass().add("root-pane");

        CheckBox multipleCheckBox = new CheckBox("Multiple");
        filePicker.selectionModeProperty().bind(multipleCheckBox.selectedProperty().map(selected ->
                selected ? SelectionMode.MULTIPLE : SelectionMode.SINGLE));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button clearButton = new Button("Clear");
        clearButton.setOnAction(event -> fileTableView.getItems().clear());

        HBox controlsBox = new HBox(multipleCheckBox, spacer, clearButton);
        controlsBox.getStyleClass().add("controls-box");
        rootPane.setTop(controlsBox);

        return rootPane;
    }
}
