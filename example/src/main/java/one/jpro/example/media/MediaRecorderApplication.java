package one.jpro.example.media;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

/**
 * Simple camera recorder application using JPro-Media library.
 * This is the example shown on the documentation.
 *
 * @author Besmir Beqiri
 */
public class MediaRecorderApplication extends Application {

    public void start(Stage stage) {
        stage.setTitle("JPro Camera Recorder");

        // Create the media recorder and the media view.
        MediaRecorder mediaRecorder = MediaRecorder.create(stage);
        MediaView cameraView = MediaView.create(mediaRecorder);

        // Controls
        Button enableCamButton = new Button("Enable Cam");
        Button startButton = new Button("Start");
        startButton.setDisable(true);
        Button stopButton = new Button("Stop");
        stopButton.setDisable(true);
        Button saveButton = new Button("Save As...");
        saveButton.setDisable(true);

        StackPane previewPane = new StackPane(enableCamButton);

        FlowPane controlsPane = new FlowPane(startButton, stopButton, saveButton);
        controlsPane.setAlignment(Pos.CENTER);
        controlsPane.setHgap(8);
        controlsPane.setVgap(8);
        controlsPane.setPadding(new Insets(8));

        // Control events
        enableCamButton.setOnAction(event -> mediaRecorder.enable());
        startButton.setOnAction(event -> mediaRecorder.start());
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
            stopButton.setDisable(false);
            saveButton.setDisable(true);
        });
        mediaRecorder.setOnStopped(event -> {
            startButton.setDisable(false);
            stopButton.setDisable(true);
            saveButton.setDisable(false);
        });
        mediaRecorder.setOnError(event -> System.out.println(mediaRecorder.getError().toString()));

        VBox rootPane = new VBox(previewPane, controlsPane);
        VBox.setVgrow(previewPane, Priority.ALWAYS);
        cameraView.fitWidthProperty().bind(rootPane.widthProperty());
        cameraView.fitHeightProperty().bind(rootPane.heightProperty()
                .subtract(controlsPane.heightProperty()));

        Scene scene = new Scene(rootPane, 760, 540);
        stage.setScene(scene);
        stage.show();
    }
}
