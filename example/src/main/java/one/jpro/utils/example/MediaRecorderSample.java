package one.jpro.utils.example;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import one.jpro.media.recorder.MediaRecorder;

/**
 * Media recorder sample.
 *
 * @author Besmir Beqiri
 */
public class MediaRecorderSample extends Application {

    private MediaRecorder mediaRecorder;

    @Override
    public void start(Stage stage) {
        var mediaRecorder = MediaRecorder.create(stage);

        var enableCamera = new Button("Enable Camera");
        var previewLabel = new Label("Camera View");
        previewLabel.setFont(Font.font("Roboto Bold", 24));

        var cameraView = mediaRecorder.getCameraView();
        cameraView.setPrefSize(640.0, 480.0);
        cameraView.setStyle("-fx-background-color: black, white; -fx-background-insets: 0, 1;");

        var startButton = new Button("Start Recording");
        var pauseResumeButton = new Button("Pause Recording");
        pauseResumeButton.setDisable(true);
        var stopButton = new Button("Stop Recording");
        stopButton.setDisable(true);
        var saveButton = new Button("Save As...");
        saveButton.setDisable(true);

        var hBox = new HBox(startButton, pauseResumeButton, stopButton, saveButton);
        hBox.setAlignment(Pos.CENTER);
        hBox.setSpacing(12.0);

        var box = new VBox(enableCamera, previewLabel, cameraView, hBox);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(640.0);
        box.setSpacing(8.0);

        // button events
        enableCamera.setOnAction(event -> mediaRecorder.enable());
        startButton.setOnAction(event -> mediaRecorder.start());
        pauseResumeButton.setOnAction(event -> mediaRecorder.pause());
        stopButton.setOnAction(event -> mediaRecorder.stop());
        saveButton.setOnAction(event -> mediaRecorder.retrieve());

        // media recorder events
        mediaRecorder.setOnStart(event -> {
            startButton.setDisable(true);
            pauseResumeButton.setDisable(false);
            stopButton.setDisable(false);
            saveButton.setDisable(true);
        });
        mediaRecorder.setOnPause(event -> {
            pauseResumeButton.setText("Resume Recording");
            pauseResumeButton.setOnAction(event2 -> mediaRecorder.resume());
        });
        mediaRecorder.setOnResume(event1 -> {
            pauseResumeButton.setText("Pause Recording");
            pauseResumeButton.setOnAction(event2 -> mediaRecorder.pause());
        });
        mediaRecorder.setOnStopped(event -> {
            startButton.setDisable(false);
            pauseResumeButton.setDisable(true);
            stopButton.setDisable(true);
            saveButton.setDisable(false);
        });
        mediaRecorder.setOnError(event -> System.out.println(mediaRecorder.getError().toString()));

        var root = new StackPane(box);
        root.setPadding(new Insets(16.0));
        var scene = new Scene(root, 720, 640);
        scene.getStylesheets().add(new PrimerLight().getUserAgentStylesheet());
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
