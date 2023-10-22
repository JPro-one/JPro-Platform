package one.jpro.platform.file.example.upload;

import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.Styles;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.picker.FileOpenPicker;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import java.net.URL;
import java.util.Optional;

/**
 * {@code FileUploaderSample} is an application that demonstrates the use of a file picker
 * to select and manage files for upload. It employs the JPro {@link FileOpenPicker}
 * class to handle the file selection functionality and incorporates a {@link TableView}
 * to display the selected files.
 *
 * <p>
 * The application allows the user to choose files based on various file extension filters.
 * It also provides options to clear the selected files and to set the selection mode (single or multiple).
 *
 * @author Besmir Beqiri
 * @see FileOpenPicker
 * @see ExtensionFilter
 */
public final class FileUploaderSample extends Application {

    private final ExtensionFilter textExtensionFilter = ExtensionFilter.of("Text files", ".txt", ".srt", ".md", ".csv");
    private final ExtensionFilter audioExtensionFilter = ExtensionFilter.of("Audio files", ".mp3", ".wav", ".ogg");
    private final ExtensionFilter videoExtensionFilter = ExtensionFilter.of("Video files", ".mp4", ".avi", ".mkv");
    private final ExtensionFilter imageExtensionFilter = ExtensionFilter.of("Image files", ".png", ".jpg", ".jpeg");

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

    /**
     * Creates the root UI component of the application.
     *
     * @param stage the primary stage for this JavaFX application
     * @return the root UI component
     */
    public Parent createRoot(Stage stage) {
        FileTableView fileTableView = new FileTableView();
        Label clickOnMeLabel = new Label("Click on me to open the file picker!");
        StackPane placeholderPane = new StackPane(clickOnMeLabel);
        placeholderPane.getStyleClass().add("placeholder-pane");
        fileTableView.setPlaceholder(placeholderPane);

        FileOpenPicker fileOpenPicker = FileOpenPicker.create(placeholderPane);
        fileOpenPicker.getExtensionFilters().addAll(textExtensionFilter,
                audioExtensionFilter, videoExtensionFilter, imageExtensionFilter);
        fileOpenPicker.setSelectedExtensionFilter(videoExtensionFilter);
        fileOpenPicker.setOnFilesSelected(fileSources -> fileTableView.getItems().addAll(fileSources));

        BorderPane rootPane = new BorderPane(fileTableView);
        rootPane.getStyleClass().add("root-pane");

        ChoiceBox<SelectionMode> selectionModeComboBox = new ChoiceBox<>();
        selectionModeComboBox.getItems().addAll(SelectionMode.SINGLE, SelectionMode.MULTIPLE);
        selectionModeComboBox.getSelectionModel().select(SelectionMode.SINGLE);
        selectionModeComboBox.setConverter(selectionModeStringConverter);
        fileOpenPicker.selectionModeProperty().bind(selectionModeComboBox.valueProperty());

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

    /**
     * A StringConverter for converting between a SelectionMode and its textual representation.
     */
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
