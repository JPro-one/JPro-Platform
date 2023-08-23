package one.jpro.platform.example.media;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import one.jpro.platform.media.MediaSource;
import one.jpro.platform.media.MediaView;
import one.jpro.platform.media.player.MediaPlayer;

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
        Scene scene = new Scene(createRoot(stage), 1140, 640);
        scene.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        stage.setScene(scene);
        stage.show();
    }

    public Parent createRoot(Stage stage) {
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
        stopButton.setOnAction(event -> {
            mediaPlayer.stop();
            seekSlider.setValue(0);
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
            playPauseButton.setOnAction(event2 -> mediaPlayer.play());
            playPauseButton.setDisable(false);
            stopButton.setDisable(false);
            seekSlider.setMax(mediaPlayer.getDuration().toSeconds());
            volumeSlider.setValue(mediaPlayer.getVolume() * 100.0);
        });
        mediaPlayer.setOnPlaying(event -> {
            playPauseButton.setText("Pause");
            playPauseButton.setOnAction(event2 -> mediaPlayer.pause());
        });
        mediaPlayer.setOnPaused(event -> {
            playPauseButton.setText("Play");
            playPauseButton.setOnAction(event2 -> mediaPlayer.play());
        });
        mediaPlayer.setOnStopped(event -> {
            playPauseButton.setText("Play");
            playPauseButton.setOnAction(event2 -> mediaPlayer.play());
        });
        mediaPlayer.setOnEndOfMedia(event -> {
            playPauseButton.setText("Play");
            playPauseButton.setOnAction(event2 -> mediaPlayer.play());
        });
        mediaPlayer.setOnError(event -> System.out.println(mediaPlayer.getError().toString()));

        // User interface
        FlowPane controlsPane = new FlowPane(playPauseButton, stopButton, seekSlider,
                preserveRatioCheckBox, muteCheckBox, volumeSlider);
        controlsPane.getStyleClass().add("controls-pane");
        VBox rootPane = new VBox(mediaView, controlsPane);
        rootPane.getStyleClass().add("root-pane");
        VBox.setVgrow(mediaView, Priority.ALWAYS);

        Optional.ofNullable(getClass().getResource("css/media_sample.css"))
                .map(URL::toExternalForm)
                .ifPresent(cssResource -> rootPane.getStylesheets().add(cssResource));

        // Stop media player and release resources when we switch views via routing links
        rootPane.sceneProperty().addListener(observable -> {
            if (rootPane.getScene() == null) {
                mediaPlayer.stop();
            }
        });

        return rootPane;
    }

    public static void main(String[] args) {
        launch(args);
    }
}