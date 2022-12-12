package one.jpro.sound.example;

import com.jpro.webapi.JProApplication;
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
 * JPro media recorder sample.
 *
 * @author Besmir Beqiri
 */
public class JProRecorderSample extends JProApplication {

    @Override
    public void start(Stage primaryStage) {
        var mediaRecorder = MediaRecorder.create(getWebAPI());

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
        var downloadButton = new Button("Download Button");
        downloadButton.setDisable(true);

        var hBox = new HBox(startButton, pauseResumeButton, stopButton, downloadButton);
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
        downloadButton.setOnAction(event -> mediaRecorder.download());

        // media recorder events
        mediaRecorder.setOnStart(event -> {
            startButton.setDisable(true);
            pauseResumeButton.setDisable(false);
            stopButton.setDisable(false);
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
            downloadButton.setDisable(false);
        });
        mediaRecorder.setOnError(event -> System.out.println(mediaRecorder.getError().toString()));

        var scene = new Scene(new StackPane(box), 640, 480);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
