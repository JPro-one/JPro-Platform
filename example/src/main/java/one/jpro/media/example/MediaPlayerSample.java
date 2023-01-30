package one.jpro.media.example;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.scene.media.MediaPlayer.Status;
import javafx.stage.Stage;
import javafx.util.Duration;
import one.jpro.media.MediaSource;
import one.jpro.media.player.MediaPlayer;
import one.jpro.media.MediaView;

import java.net.URL;
import java.util.Optional;

/**
 * Media player example.
 *
 * @author Besmir Beqiri
 */
public class MediaPlayerSample extends Application {

    public static final String MEDIA_SOURCE = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";

    @Override
    public void start(Stage stage) {
        stage.setTitle("JPro Media Player");

        // Media player
        MediaPlayer mediaPlayer = MediaPlayer.create(stage, new MediaSource(MEDIA_SOURCE));
        MediaView mediaView = MediaView.create(mediaPlayer);

        // Controls
        Button playPauseButton = new Button("Play");
        playPauseButton.setDisable(true);
        Button stopButton = new Button("Stop");
        stopButton.setDisable(true);
        Slider seekSlider = new Slider();
        seekSlider.setPrefWidth(480);
        CheckBox preserveRatioCheckBox = new CheckBox("Preserve Ratio");
        preserveRatioCheckBox.setSelected(mediaView.isPreserveRatio());
        CheckBox muteCheckBox = new CheckBox("Mute");
        Slider volumeSlider = new Slider(0, 100, mediaPlayer.getVolume() * 100.0);

        // Control events
        playPauseButton.setOnAction(event -> {
            if (mediaPlayer.getStatus() == Status.READY ||
                    mediaPlayer.getStatus() == Status.PAUSED ||
                    mediaPlayer.getStatus() == Status.STOPPED) {
                mediaPlayer.play();
            } else {
                mediaPlayer.pause();
            }
        });
        stopButton.setOnAction(event -> mediaPlayer.stop());

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

        preserveRatioCheckBox.setOnAction(event ->
                mediaView.setPreserveRatio(preserveRatioCheckBox.isSelected()));

        muteCheckBox.setOnAction(event -> {
            mediaPlayer.setMute(muteCheckBox.isSelected());
            volumeSlider.setDisable(muteCheckBox.isSelected());
        });

        volumeSlider.valueProperty().addListener(observable ->
                mediaPlayer.setVolume(volumeSlider.getValue() / 100.0));
        mediaPlayer.volumeProperty().addListener(observable -> {
            if (!volumeSlider.isValueChanging()) {
                volumeSlider.setValue(mediaPlayer.getVolume() * 100.0);
            }
        });

        // Player event handlers
        mediaPlayer.setOnReady(event -> {
            playPauseButton.setDisable(false);
            stopButton.setDisable(false);
            seekSlider.setMax(mediaPlayer.getDuration().toSeconds());
            volumeSlider.setValue(mediaPlayer.getVolume() * 100.0);
        });
        mediaPlayer.setOnPlaying(event -> playPauseButton.setText("Pause"));
        mediaPlayer.setOnPause(event -> playPauseButton.setText("Play"));
        mediaPlayer.setOnStopped(event -> playPauseButton.setText("Play"));
        mediaPlayer.setOnError(event -> mediaPlayer.getError().printStackTrace());

        // User interface
        FlowPane controlsPane = new FlowPane(playPauseButton, stopButton, seekSlider,
                preserveRatioCheckBox, muteCheckBox, volumeSlider);
        controlsPane.getStyleClass().add("controls-pane");
        VBox rootPane = new VBox(mediaView, controlsPane);
        rootPane.getStyleClass().add("root-pane");
        VBox.setVgrow(mediaView, Priority.ALWAYS);

        Scene scene = new Scene(rootPane, 1140, 640);
        scene.getStylesheets().add(new PrimerLight().getUserAgentStylesheet());
        Optional.ofNullable(getClass().getResource("css/media_sample.css"))
                .map(URL::toExternalForm)
                .ifPresent(cssResource -> scene.getStylesheets().add(cssResource));
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}