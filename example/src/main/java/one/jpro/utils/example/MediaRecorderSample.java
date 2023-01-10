package one.jpro.utils.example;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import one.jpro.media.MediaView;
import one.jpro.media.recorder.MediaRecorder;
import one.jpro.media.util.MediaUtil;

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

        var enableCamButton = new Button("Enable Cam");
        var startButton = new Button("Start");
        startButton.setDisable(true);
        var pauseResumeButton = new Button("Pause");
        pauseResumeButton.setDisable(true);
        var stopButton = new Button("Stop");
        stopButton.setDisable(true);
        var saveButton = new Button("Save As...");
        saveButton.setDisable(true);

        var previewPane = new StackPane(enableCamButton);
        previewPane.getStyleClass().add("preview-pane");

        var mediaRecorder = MediaRecorder.create(stage);
        var cameraView = MediaView.create(mediaRecorder);

        var controlsPane = new FlowPane(startButton, pauseResumeButton, stopButton, saveButton);
        controlsPane.getStyleClass().add("controls-pane");

        // button events
        enableCamButton.setOnAction(event -> mediaRecorder.enable());
        startButton.setOnAction(event -> mediaRecorder.start());
        pauseResumeButton.setOnAction(event -> mediaRecorder.pause());
        stopButton.setOnAction(event -> mediaRecorder.stop());
        saveButton.setOnAction(event -> {
            try {
                MediaUtil.retrieve(stage, mediaRecorder.getMediaSource());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        // media recorder events
        mediaRecorder.setOnReady(event -> {
            startButton.setDisable(false);
            previewPane.getChildren().setAll(cameraView);
        });
        mediaRecorder.setOnStart(event -> {
            startButton.setDisable(true);
            pauseResumeButton.setDisable(false);
            stopButton.setDisable(false);
            saveButton.setDisable(true);
        });
        mediaRecorder.setOnPause(event -> {
            pauseResumeButton.setText("Resume");
            pauseResumeButton.setOnAction(event2 -> mediaRecorder.resume());
        });
        mediaRecorder.setOnResume(event1 -> {
            pauseResumeButton.setText("Pause");
            pauseResumeButton.setOnAction(event2 -> mediaRecorder.pause());
        });
        mediaRecorder.setOnStopped(event -> {
            startButton.setDisable(false);
            pauseResumeButton.setDisable(true);
            stopButton.setDisable(true);
            saveButton.setDisable(false);
        });
        mediaRecorder.setOnError(event -> System.out.println(mediaRecorder.getError().toString()));

        var rootPane = new VBox(previewPane, controlsPane);
        rootPane.getStyleClass().add("root-pane");
        cameraView.fitWidthProperty().bind(rootPane.widthProperty());
        cameraView.fitHeightProperty().bind(rootPane.heightProperty()
                .subtract(controlsPane.heightProperty()));

        VBox.setVgrow(previewPane, Priority.ALWAYS);
        var scene = new Scene(rootPane, 760, 540);
        scene.getStylesheets().addAll(new PrimerLight().getUserAgentStylesheet());
        Optional.ofNullable(getClass().getResource("css/media_sample.css"))
                .map(URL::toExternalForm)
                .ifPresent(cssResource -> scene.getStylesheets().add(cssResource));
        stage.setScene(scene);
        stage.show();
    }

    /**
     *  Application entry point.
     */
    public static void main(String[] args) {
        launch(args);
    }
}