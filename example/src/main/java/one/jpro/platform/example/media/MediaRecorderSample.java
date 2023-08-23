package one.jpro.platform.example.media;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import one.jpro.platform.media.MediaView;
import one.jpro.platform.media.recorder.MediaRecorder;
import one.jpro.platform.media.util.MediaUtil;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

/**
 * Media recorder example.
 *
 * @author Besmir Beqiri
 */
public class MediaRecorderSample extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("JPro Camera Recorder");
        Scene scene = new Scene(createRoot(stage), 760, 540);
        scene.getStylesheets().addAll(new PrimerLight().getUserAgentStylesheet());
        stage.setScene(scene);
        stage.show();
    }

    public Parent createRoot(Stage stage) {
        // Controls
        Button enableCamButton = new Button("Enable Cam");
        Button startButton = new Button("Start");
        startButton.setDisable(true);
        Button pauseButton = new Button("Pause");
        pauseButton.setDisable(true);
        Button stopButton = new Button("Stop");
        stopButton.setDisable(true);
        Button saveButton = new Button("Save As...");
        saveButton.setDisable(true);

        StackPane previewPane = new StackPane(enableCamButton);
        previewPane.getStyleClass().add("preview-pane");

        // Create media recorder and media view
        MediaRecorder mediaRecorder = MediaRecorder.create(stage);
        MediaView cameraView = MediaView.create(mediaRecorder);

        FlowPane controlsPane = new FlowPane(startButton, pauseButton, stopButton, saveButton);
        controlsPane.getStyleClass().add("controls-pane");

        // Control events
        enableCamButton.setOnAction(event -> mediaRecorder.enable());
        startButton.setOnAction(event -> mediaRecorder.start());
        pauseButton.setOnAction(event -> mediaRecorder.pause());
        stopButton.setOnAction(event -> mediaRecorder.stop());
        saveButton.setOnAction(event -> {
            try {
                MediaUtil.retrieve(stage, mediaRecorder.getMediaSource(), "RecordedVideo");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        // Event handlers
        mediaRecorder.setOnReady(event -> {
            startButton.setDisable(false);
            previewPane.getChildren().setAll(cameraView);
        });
        mediaRecorder.setOnStart(event -> {
            startButton.setDisable(true);
            pauseButton.setDisable(false);
            stopButton.setDisable(false);
            saveButton.setDisable(true);
        });
        mediaRecorder.setOnPaused(event -> {
            startButton.setDisable(false);
            pauseButton.setDisable(true);
        });
        mediaRecorder.setOnResume(event -> {
            startButton.setDisable(true);
            pauseButton.setDisable(false);
        });
        mediaRecorder.setOnStopped(event -> {
            startButton.setDisable(false);
            pauseButton.setDisable(true);
            stopButton.setDisable(true);
            saveButton.setDisable(false);
        });
        mediaRecorder.setOnError(event -> System.out.println(mediaRecorder.getError().toString()));

        // User interface
        VBox rootPane = new VBox(previewPane, controlsPane);
        rootPane.getStyleClass().add("root-pane");
        VBox.setVgrow(previewPane, Priority.ALWAYS);
        cameraView.fitWidthProperty().bind(rootPane.widthProperty());
        cameraView.fitHeightProperty().bind(rootPane.heightProperty()
                .subtract(controlsPane.heightProperty()));

        Optional.ofNullable(getClass().getResource("css/media_sample.css"))
                .map(URL::toExternalForm)
                .ifPresent(cssResource -> rootPane.getStylesheets().add(cssResource));

        return rootPane;
    }

    /**
     *  Application entry point.
     */
    public static void main(String[] args) {
        launch(args);
    }
}