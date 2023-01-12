package one.jpro.utils.example;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import one.jpro.media.MediaSource;
import one.jpro.media.player.MediaPlayer;
import one.jpro.media.MediaView;
import one.jpro.media.recorder.MediaRecorder;
import javafx.scene.media.MediaPlayer.Status;
import one.jpro.media.util.MediaUtil;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

/**
 * Combined media recorder and player example.
 *
 * @author Besmir Beqiri
 */
public class MediaRecorderAndPlayerSample extends Application {

    public static final double MEDIA_VIEW_WIDTH = 640.0;
    public static final double MEDIA_VIEW_HEIGHT = 480.0;

    private StackPane previewPane;
    private MediaView mediaView;
    private MediaView cameraView;
    private Button enableCamButton;
    private Button recordButton;
    private Button pauseResumeButton;
    private Button playButton;
    private Button stopButton;
    private Slider seekSlider;
    private CheckBox preserveRatioCheckBox;
    private CheckBox muteCheckBox;
    private Slider volumeSlider;
    private Button saveButton;

    @Override
    public void start(Stage stage) {
        stage.setTitle("JPro Camera Recorder and Player");

        var controlsPane = createControlsPane();
        cameraView = createCameraView(stage);
        mediaView = MediaView.create(stage);
        controlsPane.maxWidthProperty().bind(mediaView.fitWidthProperty());

        previewPane = new StackPane(enableCamButton);
        mediaView.fitWidthProperty().bind(previewPane.widthProperty());
        mediaView.fitHeightProperty().bind(previewPane.heightProperty());
        preserveRatioCheckBox.setSelected(mediaView.isPreserveRatio());
        previewPane.getStyleClass().add("preview-pane");

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

    private Pane createControlsPane() {
        enableCamButton = new Button("Enable Camera");
        recordButton = new Button("Record");
        recordButton.setDisable(true);
        playButton = new Button("Play");
        playButton.setDisable(true);
        pauseResumeButton = new Button("Pause");
        pauseResumeButton.setDisable(true);
        stopButton = new Button("Stop");
        stopButton.setDisable(true);
        saveButton = new Button("Save As...");
        saveButton.setDisable(true);
        seekSlider = new Slider();
        seekSlider.setDisable(true);
        preserveRatioCheckBox = new CheckBox("Preserve Ratio");
        muteCheckBox = new CheckBox("Mute");
        volumeSlider = new Slider(0, 100, 100);

        var controlsPane = new FlowPane(recordButton, playButton, pauseResumeButton, stopButton,
                saveButton, seekSlider, preserveRatioCheckBox, muteCheckBox, volumeSlider);
        seekSlider.prefWidthProperty().bind(controlsPane.widthProperty().subtract(32));
        controlsPane.getStyleClass().add("controls-pane");
        return controlsPane;
    }

    private MediaView createCameraView(Stage stage) {
        var mediaRecorder = MediaRecorder.create(stage);
        var cameraView = MediaView.create(mediaRecorder);

        // button events
        enableCamButton.setOnAction(event -> {
            mediaRecorder.enable();
            previewPane.getChildren().setAll(cameraView);
        });
        recordButton.setOnAction(event -> {
            if (!previewPane.getChildren().contains(cameraView)) {
                previewPane.getChildren().setAll(cameraView);
            }
            mediaRecorder.start();
            pauseResumeButton.setOnAction(event2 -> mediaRecorder.pause());
            stopButton.setOnAction(event2-> mediaRecorder.stop());
        });
        saveButton.setOnAction(event -> {
            try {
                MediaUtil.retrieve(stage, mediaRecorder.getMediaSource());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        preserveRatioCheckBox.setOnAction(event -> {
            mediaView.setPreserveRatio(preserveRatioCheckBox.isSelected());
            cameraView.setPreserveRatio(preserveRatioCheckBox.isSelected());
        });

        // media recorder events
        mediaRecorder.setOnReady(event -> recordButton.setDisable(false));
        mediaRecorder.setOnStart(event -> {
            recordButton.setDisable(true);
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
            recordButton.setDisable(false);
            pauseResumeButton.setDisable(true);
            stopButton.setDisable(true);
            saveButton.setDisable(false);
            loadMedia(stage, mediaRecorder.getMediaSource());
        });
        mediaRecorder.setOnError(event -> System.out.println(mediaRecorder.getError().toString()));

        return cameraView;
    }

    private MediaPlayer loadMedia(Stage stage, MediaSource mediaSource) {
        var mediaPlayer = MediaPlayer.create(stage, mediaSource);
        mediaView.setMediaEngine(mediaPlayer);
        previewPane.getChildren().setAll(mediaView);

        // events
        playButton.setOnAction(event -> {
            if (mediaPlayer.getStatus() == Status.READY ||
                    mediaPlayer.getStatus() == Status.PAUSED ||
                    mediaPlayer.getStatus() == Status.STOPPED) {
                mediaPlayer.play();
            }
        });
        stopButton.setOnAction(event -> mediaPlayer.stop());
        mediaPlayer.durationProperty().addListener((observable) -> {
            final var duration = mediaPlayer.getDuration();
            if (duration.isUnknown()) {
                seekSlider.setDisable(true);
            } else {
                seekSlider.setDisable(false);
                seekSlider.setMax(duration.toSeconds());
            }
        });

        seekSlider.setOnMousePressed(mouseEvent ->
                mediaPlayer.seek(Duration.seconds(seekSlider.getValue())));
        seekSlider.setOnMouseDragged(mouseEvent ->
                mediaPlayer.seek(Duration.seconds(seekSlider.getValue())));
        mediaPlayer.currentTimeProperty().addListener(observable -> {
            if (mediaPlayer.getDuration().greaterThan(Duration.ZERO)
                    && !seekSlider.isDisabled()
                    && !seekSlider.isValueChanging()
                    && !seekSlider.isPressed()) {
                seekSlider.setValue(mediaPlayer.getCurrentTime().toSeconds());
            }
        });

        muteCheckBox.setOnAction(event -> {
            mediaPlayer.setMute(muteCheckBox.isSelected());
            volumeSlider.setDisable(muteCheckBox.isSelected());
        });
        volumeSlider.valueProperty().addListener(observable ->
                mediaPlayer.setVolume(volumeSlider.getValue() / 100.0));
        mediaPlayer.volumeProperty().addListener(observable -> {
            if (!volumeSlider.isValueChanging() && !volumeSlider.isPressed()) {
                volumeSlider.setValue(mediaPlayer.getVolume() * 100.0);
            }
        });

        mediaPlayer.setOnReady(event -> {
            playButton.setDisable(false);
            stopButton.setDisable(false);
            final var duration = mediaPlayer.getDuration();
            if (duration.isUnknown()) {
                seekSlider.setDisable(true);
            } else {
                seekSlider.setDisable(false);
                seekSlider.setMax(duration.toSeconds());
            }
            mediaPlayer.setVolume(volumeSlider.getValue() / 100.0);
        });
        mediaPlayer.setOnPlaying(event -> {
            playButton.setDisable(true);
            pauseResumeButton.setDisable(false);
            pauseResumeButton.setText("Pause");
            pauseResumeButton.setOnAction(event2 -> mediaPlayer.pause());
        });
        mediaPlayer.setOnPause(event -> {
            playButton.setDisable(false);
            pauseResumeButton.setDisable(true);
        });
        mediaPlayer.setOnStopped(event -> {
            playButton.setDisable(false);
            pauseResumeButton.setDisable(true);
        });
        mediaPlayer.setOnError(event -> System.out.println("Error: " + mediaPlayer.getError()));

        return mediaPlayer;
    }

    /**
     * Application entry point.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
