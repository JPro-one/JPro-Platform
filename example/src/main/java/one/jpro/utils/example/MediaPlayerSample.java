package one.jpro.utils.example;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import one.jpro.media.player.MediaView;

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

        // media player
        var mediaPlayer = MediaPlayer.create(stage, new MediaSource(MEDIA_SOURCE));
        var mediaView = MediaView.create(mediaPlayer);

        // controls
        var playPauseButton = new Button("Play");
        playPauseButton.setDisable(true);
        var stopButton = new Button("Stop");
        stopButton.setDisable(true);
        var seekSlider = new Slider();
        seekSlider.getStyleClass().add("seek-slider");
        var preserveRatioCheckBox = new CheckBox("Preserve Ratio");
        preserveRatioCheckBox.setSelected(mediaView.isPreserveRatio());
        var muteCheckBox = new CheckBox("Mute");
        var volumeSlider = new Slider(0, 100, mediaPlayer.getVolume() * 100.0);
        volumeSlider.getStyleClass().add("volume-slider");

        // events
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

        mediaPlayer.setOnReady(event -> {
            playPauseButton.setDisable(false);
            stopButton.setDisable(false);
            seekSlider.setMax(mediaPlayer.getDuration().toSeconds());
            volumeSlider.setValue(mediaPlayer.getVolume() * 100.0);
        });
        mediaPlayer.setOnPlaying(event -> playPauseButton.setText("Pause"));
        mediaPlayer.setOnPause(event -> playPauseButton.setText("Play"));
        mediaPlayer.setOnStopped(event -> playPauseButton.setText("Play"));
        mediaPlayer.setOnError(event -> System.out.println("Error: " + mediaPlayer.getError()));

        // interface
        var controlsPane = new FlowPane(playPauseButton, stopButton, seekSlider,
                preserveRatioCheckBox, muteCheckBox, volumeSlider);
        controlsPane.getStyleClass().add("controls-pane");
        var rootPane = new VBox(mediaView, controlsPane);
        rootPane.getStyleClass().add("root-pane");
        mediaView.fitWidthProperty().bind(rootPane.widthProperty());
        mediaView.fitHeightProperty().bind(rootPane.heightProperty()
                .subtract(controlsPane.heightProperty()));

        var scene = new Scene(rootPane, 1140, 640);
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
