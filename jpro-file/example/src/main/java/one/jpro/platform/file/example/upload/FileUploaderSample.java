package one.jpro.platform.file.example.upload;

import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.Styles;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.picker.FilePicker;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import java.net.URL;
import java.util.Optional;

/**
 * This class represents a sample application for file picker operations.
 *
 * @author Besmir Beqiri
 */
public class FileUploaderSample extends Application {

    private static final ExtensionFilter textExtensionFilter = ExtensionFilter.of("Text files", ".txt", ".srt", ".md", ".csv");
    private static final ExtensionFilter audioExtensionFilter = ExtensionFilter.of("Audio files", ".mp3", ".wav", ".ogg");
    private static final ExtensionFilter videoExtensionFilter = ExtensionFilter.of("Video files", ".mp4", ".avi", ".mkv");
    private static final ExtensionFilter imageExtensionFilter = ExtensionFilter.of("Image files", ".png", ".jpg", ".jpeg");

    @Override
    public void start(Stage stage) {
        stage.setTitle("JPro File Picker");
        var scene = new Scene(createRoot(stage), 1140, 640);
        Optional.ofNullable(CupertinoLight.class.getResource(new CupertinoLight().getUserAgentStylesheet()))
                .map(URL::toExternalForm)
                .ifPresent(scene::setUserAgentStylesheet);
        Optional.ofNullable(FileUploaderSample.class.getResource("/one/jpro/platform/file/example/css/file_picker.css"))
                .map(URL::toExternalForm)
                .ifPresent(scene.getStylesheets()::add);
        stage.setScene(scene);
        stage.show();
    }

    public Parent createRoot(Stage stage) {
        FileTableView fileTableView = new FileTableView();
        Label clickOnMeLabel = new Label("Click on me to open the file picker!");
        StackPane placeholderPane = new StackPane(clickOnMeLabel);
        placeholderPane.getStyleClass().add("placeholder-pane");
        fileTableView.setPlaceholder(placeholderPane);

        final var filePicker = FilePicker.create(placeholderPane);
        filePicker.getExtensionFilters().addAll(textExtensionFilter,
                audioExtensionFilter, videoExtensionFilter, imageExtensionFilter);
        filePicker.setSelectedExtensionFilter(videoExtensionFilter);
        filePicker.setOnFilesSelected(fileSources -> fileTableView.getItems().addAll(fileSources));

        BorderPane rootPane = new BorderPane(fileTableView);
        rootPane.getStyleClass().add("root-pane");

        ChoiceBox<SelectionMode> selectionModeComboBox = new ChoiceBox<>();
        selectionModeComboBox.getItems().addAll(SelectionMode.SINGLE, SelectionMode.MULTIPLE);
        selectionModeComboBox.getSelectionModel().select(SelectionMode.SINGLE);
        selectionModeComboBox.setConverter(selectionModeStringConverter);
        filePicker.selectionModeProperty().bind(selectionModeComboBox.valueProperty());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button clearButton = new Button("Clear", new FontIcon(Material2AL.CLEAR));
        clearButton.getStyleClass().add(Styles.DANGER);
        clearButton.disableProperty().bind(Bindings.isEmpty(fileTableView.getItems()));
        clearButton.setOnAction(event -> fileTableView.getItems().clear());

        Label selectionModeLabel = new Label("Selection Mode:");
        HBox controlsBox = new HBox(selectionModeLabel, selectionModeComboBox, spacer, clearButton);
        controlsBox.getStyleClass().add("controls-box");
        rootPane.setTop(controlsBox);

        return rootPane;
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
