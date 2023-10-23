package one.jpro.platform.file.example.upload;

import atlantafx.base.controls.RingProgressIndicator;
import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.Styles;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.FileSource;
import one.jpro.platform.file.picker.FileOpenPicker;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import java.net.URL;
import java.util.List;
import java.util.Optional;

/**
 * {@code FileUploaderSample} is an application that demonstrates the use of a file picker
 * to select and manage files for upload. It employs the JPro {@link FileOpenPicker}
 * class to handle the file selection functionality and incorporates a {@link TableView}
 * to display the selected files.
 * <p>
 * The application provides options to clear the selected files and to set the selection
 * mode (single or multiple).
 *
 * @author Besmir Beqiri
 * @see FileOpenPicker
 * @see ExtensionFilter
 */
public final class FileUploaderSample extends Application {

    private FileTableView fileTableView = new FileTableView();
    private final DoubleProperty overallUploadProgress =
            new SimpleDoubleProperty(this, "overallUploadProgress", 0.0);

    @Override
    public void start(Stage stage) {
        stage.setTitle("JPro File Uploader");
        var scene = new Scene(createRoot(stage), 1140, 640);
        Optional.ofNullable(CupertinoLight.class.getResource(new CupertinoLight().getUserAgentStylesheet()))
                .map(URL::toExternalForm)
                .ifPresent(scene::setUserAgentStylesheet);
        scene.getStylesheets().add(FileUploaderSample.class
                .getResource("/one/jpro/platform/file/example/css/file_uploader.css").toExternalForm());
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
        fileTableView = new FileTableView();
        Label clickOnMeLabel = new Label("Click on me to open the file picker!");
        StackPane placeholderPane = new StackPane(clickOnMeLabel);
        placeholderPane.getStyleClass().add("placeholder-pane");
        fileTableView.setPlaceholder(placeholderPane);

        FileOpenPicker fileOpenPicker = FileOpenPicker.create(placeholderPane);
        fileOpenPicker.setOnFilesSelected(this::addAllFiles);

        BorderPane rootPane = new BorderPane(fileTableView);
        rootPane.getStyleClass().add("root-pane");

        ChoiceBox<SelectionMode> selectionModeComboBox = new ChoiceBox<>();
        selectionModeComboBox.getItems().addAll(SelectionMode.SINGLE, SelectionMode.MULTIPLE);
        selectionModeComboBox.getSelectionModel().select(SelectionMode.MULTIPLE);
        selectionModeComboBox.setConverter(selectionModeStringConverter);
        fileOpenPicker.selectionModeProperty().bind(selectionModeComboBox.valueProperty());

        Button uploadButton = new Button("Upload All", new FontIcon(Material2AL.CLOUD_UPLOAD));
        uploadButton.disableProperty().bind(Bindings.isEmpty(fileTableView.getItems())
                .or(overallUploadProgress.isEqualTo(1)));
        uploadButton.setOnAction(event -> fileTableView.getItems().forEach(FileSource::uploadFileAsync));

        Button clearButton = new Button("Clear", new FontIcon(Material2AL.CLEAR));
        clearButton.getStyleClass().add(Styles.DANGER);
        clearButton.disableProperty().bind(Bindings.isEmpty(fileTableView.getItems()));
        clearButton.setOnAction(event -> {
            fileTableView.getItems().clear();
            overallUploadProgress.unbind();
            overallUploadProgress.set(0.0);
        });

        Label selectionModeLabel = new Label("Selection Mode:");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox controlsBox = new HBox(selectionModeLabel, selectionModeComboBox, spacer, uploadButton, clearButton);
        controlsBox.getStyleClass().add("controls-box");
        rootPane.setTop(controlsBox);

        RingProgressIndicator progressIndicator = new RingProgressIndicator();
        progressIndicator.progressProperty().bind(overallUploadProgress);
        progressIndicator.setStringConverter(progressStringConverter);
        Label overallProgressLabel = new Label("Overall Upload Progress: ");
        HBox progressBox = new HBox(overallProgressLabel, progressIndicator);
        progressBox.getStyleClass().add("progress-box");
        rootPane.setBottom(progressBox);

        return rootPane;
    }

    /**
     * Adds all selected files to the file table view and binds the overall upload progress
     * to the average progress of all files.
     *
     * @param fileSources the list of selected file sources to add
     */
    private void addAllFiles(List<? extends FileSource> fileSources) {
        fileTableView.getItems().setAll(fileSources);
        // Bind the overall upload progress to the average progress of all files.
        overallUploadProgress.bind(Bindings.createDoubleBinding(() ->
                        fileSources.stream()
                                .mapToDouble(FileSource::getProgress)
                                .reduce(0.0, Double::sum) / fileSources.size(),
                fileSources.stream()
                        .map(FileSource::progressProperty)
                        .toList().toArray(new ReadOnlyDoubleProperty[fileSources.size()])));
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

    /**
     * A StringConverter for converting between a progress double value and its textual
     * representation as a percentage.
     */
    private final StringConverter<Double> progressStringConverter = new StringConverter<>() {
        @Override
        public String toString(Double value) {
            return String.format("%.0f%%", value * 100);
        }

        @Override
        public Double fromString(String string) {
            return Double.parseDouble(string) / 100;
        }
    };
}
