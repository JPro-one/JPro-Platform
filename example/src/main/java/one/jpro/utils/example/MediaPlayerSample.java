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
import one.jpro.media.player.MediaPlayer;
import one.jpro.media.player.MediaView;

/**
 * Media player sample.
 *
 * @author Besmir Beqiri
 */
public class MediaPlayerSample extends Application {

    public static final String MEDIA_SOURCE = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";

    @Override
    public void start(Stage stage) {
        // media player
        var mediaPlayer = MediaPlayer.create(stage, MEDIA_SOURCE);
        var mediaView = MediaView.create(mediaPlayer);
        mediaView.setPreserveRatio(false);
        mediaView.setStyle("-fx-background-color: black, white; -fx-background-insets: 0, 1;");

        // controls
        var playPauseButton = new Button("Play");
        playPauseButton.setPrefWidth(80.0);
        playPauseButton.setDisable(true);
        var stopButton = new Button("Stop");
        stopButton.setPrefWidth(80.0);
        stopButton.setDisable(true);
        var seekSlider = new Slider();
        seekSlider.setPrefWidth(420.0);
        var preserveRatioCheckBox = new CheckBox("Preserve Ratio");
        preserveRatioCheckBox.setSelected(mediaView.isPreserveRatio());
        var muteCheckBox = new CheckBox("Mute");
        var volumeSlider = new Slider(0, 100, mediaPlayer.getVolume() * 100.0);
        volumeSlider.setPrefWidth(120.0);

        // events
        playPauseButton.setOnAction(event -> {
            if (mediaPlayer.getStatus() == Status.PLAYING) {
                mediaPlayer.pause();
            } else if (mediaPlayer.getStatus() == Status.READY ||
                    mediaPlayer.getStatus() == Status.PAUSED ||
                    mediaPlayer.getStatus() == Status.STOPPED) {
                mediaPlayer.play();
            }
        });
        stopButton.setOnAction(event -> mediaPlayer.stop());
        seekSlider.setOnMouseReleased(mouseEvent ->
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
            if (!volumeSlider.isValueChanging() && !volumeSlider.isPressed()) {
                volumeSlider.setValue(mediaPlayer.getVolume() * 100.0);
            }
        });

        mediaPlayer.setOnReady(event -> {
            playPauseButton.setDisable(false);
            stopButton.setDisable(false);
            seekSlider.setMax(mediaPlayer.getDuration().toSeconds());
            mediaPlayer.setVolume(mediaPlayer.getVolume() * 100.0);
        });
        mediaPlayer.setOnPlay(event -> playPauseButton.setText("Pause"));
        mediaPlayer.setOnPause(event -> playPauseButton.setText("Play"));
        mediaPlayer.setOnStopped(event -> playPauseButton.setText("Play"));
        mediaPlayer.setOnError(event -> System.out.println("Error: " + mediaPlayer.getError()));

        // interface
        var controlsPane = new FlowPane(playPauseButton, stopButton, seekSlider,
                preserveRatioCheckBox, muteCheckBox, volumeSlider);
        controlsPane.setAlignment(Pos.CENTER);
        controlsPane.setHgap(8.0);
        controlsPane.setVgap(8.0);
        controlsPane.setPadding(new Insets(8.0));
        controlsPane.maxWidthProperty().bind(mediaView.fitWidthProperty());
        var vbox = new VBox(mediaView, controlsPane);
        mediaView.fitWidthProperty().bind(vbox.widthProperty());
        mediaView.fitHeightProperty().bind(vbox.heightProperty().subtract(controlsPane.heightProperty()));
        vbox.setSpacing(8.0);
        var root = new AnchorPane(vbox);
        AnchorPane.setTopAnchor(vbox, 16.0);
        AnchorPane.setRightAnchor(vbox, 16.0);
        AnchorPane.setBottomAnchor(vbox, 16.0);
        AnchorPane.setLeftAnchor(vbox, 16.0);
        var scene = new Scene(root, 1100, 720);
        scene.getStylesheets().add(new PrimerLight().getUserAgentStylesheet());
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
