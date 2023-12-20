package one.jpro.platform.media.example;

import atlantafx.base.theme.CupertinoLight;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import one.jpro.platform.media.MediaSource;
import one.jpro.platform.media.MediaView;
import one.jpro.platform.media.player.MediaPlayer;
import one.jpro.platform.media.recorder.MediaRecorder;
import one.jpro.platform.media.util.MediaUtil;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

/**
 * Combined media recorder and player example.
 *
 * @author Besmir Beqiri
 */
public class MediaRecorderAndPlayerSample extends Application {

    private StackPane previewPane;
    private MediaView mediaView;
    private Button enableCamButton;
    private Button recordButton;
    private Button pauseButton;
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
        Scene scene = new Scene(createRoot(stage), 760, 540);
        scene.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
        stage.setScene(scene);
        stage.show();
    }

    public Parent createRoot(Stage stage) {
        // run GC every 1s
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    System.out.println("GC");
                    System.gc();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        Pane controlsPane = createControlsPane();
        MediaView cameraView = createCameraView(stage);
        mediaView = MediaView.create(stage);
        controlsPane.maxWidthProperty().bind(mediaView.fitWidthProperty());

        previewPane = new StackPane(enableCamButton);
        mediaView.fitWidthProperty().bind(previewPane.widthProperty());
        mediaView.fitHeightProperty().bind(previewPane.heightProperty());
        preserveRatioCheckBox.setSelected(mediaView.isPreserveRatio());
        previewPane.getStyleClass().add("preview-pane");

        VBox rootPane = new VBox(previewPane, controlsPane);
        rootPane.getStyleClass().add("root-pane");
        cameraView.fitWidthProperty().bind(rootPane.widthProperty());
        cameraView.fitHeightProperty().bind(rootPane.heightProperty()
                .subtract(controlsPane.heightProperty()));
        VBox.setVgrow(previewPane, Priority.ALWAYS);

        Optional.ofNullable(getClass().getResource("css/media_sample.css"))
                .map(URL::toExternalForm)
                .ifPresent(cssResource -> rootPane.getStylesheets().add(cssResource));

        return rootPane;
    }

    private Pane createControlsPane() {
        enableCamButton = new Button("Enable Camera");
        recordButton = new Button("Record");
        recordButton.setDisable(true);
        playButton = new Button("Play");
        playButton.setDisable(true);
        pauseButton = new Button("Pause");
        pauseButton.setDisable(true);
        stopButton = new Button("Stop");
        stopButton.setDisable(true);
        saveButton = new Button("Save As...");
        saveButton.setDisable(true);
        seekSlider = new Slider();
        seekSlider.setDisable(true);
        preserveRatioCheckBox = new CheckBox("Preserve Ratio");
        muteCheckBox = new CheckBox("Mute");
        volumeSlider = new Slider(0, 1, 1);

        FlowPane controlsPane = new FlowPane(recordButton, playButton, pauseButton, stopButton,
                saveButton, seekSlider, preserveRatioCheckBox, muteCheckBox, volumeSlider);
        seekSlider.prefWidthProperty().bind(controlsPane.widthProperty().subtract(32));
        controlsPane.getStyleClass().add("controls-pane");
        return controlsPane;
    }

    private MediaView createCameraView(Stage stage) {
        MediaRecorder mediaRecorder = MediaRecorder.create(stage);
        MediaView cameraView = MediaView.create(mediaRecorder);

        // button events
        enableCamButton.setOnAction(event -> {
            mediaRecorder.enable();
            previewPane.getChildren().setAll(cameraView);
        });
        recordButton.setOnAction(event -> {
            if (!previewPane.getChildren().contains(cameraView)) {
                previewPane.getChildren().setAll(cameraView);
            }
            playButton.setDisable(true);
            mediaRecorder.start();
            pauseButton.setOnAction(event2 -> mediaRecorder.pause());
            stopButton.setOnAction(event2 -> mediaRecorder.stop());
        });
        saveButton.setOnAction(event -> {
            try {
                MediaUtil.retrieve(stage, mediaRecorder.getMediaSource(), "RecordedVideo");
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
            pauseButton.setDisable(false);
            stopButton.setDisable(false);
            saveButton.setDisable(true);
            seekSlider.setDisable(true);
            seekSlider.setValue(0);
        });
        mediaRecorder.setOnPaused(event -> {
            recordButton.setDisable(false);
            pauseButton.setDisable(true);
        });
        mediaRecorder.setOnResume(event -> {
            recordButton.setDisable(true);
            pauseButton.setDisable(false);
        });
        mediaRecorder.setOnStopped(event -> {
            recordButton.setDisable(false);
            pauseButton.setDisable(true);
            stopButton.setDisable(true);
            saveButton.setDisable(false);
            loadMedia(stage, mediaRecorder.getMediaSource());
        });
        mediaRecorder.setOnError(event -> System.out.println(mediaRecorder.getError().toString()));

        return cameraView;
    }

    private void loadMedia(Stage stage, MediaSource mediaSource) {
        MediaPlayer mediaPlayer = MediaPlayer.create(stage, mediaSource);
        mediaPlayer.setMute(muteCheckBox.isSelected());
        mediaPlayer.setVolume(volumeSlider.getValue());
        mediaView.setMediaEngine(mediaPlayer);
        previewPane.getChildren().setAll(mediaView);

        // events
        playButton.setOnAction(event -> {
            mediaPlayer.play();
            pauseButton.setOnAction(event2 -> mediaPlayer.pause());
            stopButton.setOnAction(event2 -> mediaPlayer.stop());
        });
        stopButton.setOnAction(event -> {
            mediaPlayer.stop();
            seekSlider.setValue(0);
        });
        mediaPlayer.durationProperty().addListener((observable) -> {
            Duration duration = mediaPlayer.getDuration();
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
                mediaPlayer.setVolume(volumeSlider.getValue()));
        mediaPlayer.volumeProperty().addListener(observable -> {
            if (!volumeSlider.isValueChanging() && !volumeSlider.isPressed()) {
                volumeSlider.setValue(mediaPlayer.getVolume());
            }
        });

        mediaPlayer.setOnReady(event -> {
            playButton.setDisable(false);
            stopButton.setDisable(false);
            Duration duration = mediaPlayer.getDuration();
            if (duration.isUnknown()) {
                seekSlider.setDisable(true);
            } else {
                seekSlider.setDisable(false);
                seekSlider.setMax(duration.toSeconds());
            }
            mediaPlayer.setVolume(volumeSlider.getValue());
        });
        mediaPlayer.setOnPlaying(event -> {
            playButton.setDisable(true);
            pauseButton.setDisable(false);
        });
        mediaPlayer.setOnPaused(event -> {
            playButton.setDisable(false);
            pauseButton.setDisable(true);
        });
        mediaPlayer.setOnStopped(event -> {
            playButton.setDisable(false);
            pauseButton.setDisable(true);
        });
        mediaPlayer.setOnError(event -> System.out.println("Error: " + mediaPlayer.getError()));
    }

    /**
     * Application entry point.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
