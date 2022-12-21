package one.jpro.utils.example;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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
        mediaPlayer.setOnReady(event -> {
            playPauseButton.setDisable(false);
            stopButton.setDisable(false);
            seekSlider.setMax(mediaPlayer.getDuration().toSeconds());
        });
        mediaPlayer.setOnPlay(event -> playPauseButton.setText("Pause"));
        mediaPlayer.setOnPause(event -> playPauseButton.setText("Play"));
        mediaPlayer.setOnStopped(event -> playPauseButton.setText("Play"));
        mediaPlayer.setOnError(event -> System.out.println("Error: " + mediaPlayer.getError()));

        // interface
        HBox.setHgrow(seekSlider, Priority.ALWAYS);
        var controlsBox = new HBox(playPauseButton, stopButton, seekSlider);
        controlsBox.setAlignment(Pos.CENTER_LEFT);
        controlsBox.setSpacing(8.0);
        controlsBox.setPadding(new Insets(8.0));
        controlsBox.maxWidthProperty().bind(mediaView.fitWidthProperty());
        var vbox = new VBox(mediaView, controlsBox);
        mediaView.fitWidthProperty().bind(vbox.widthProperty());
        mediaView.fitHeightProperty().bind(vbox.heightProperty().subtract(controlsBox.heightProperty()));
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
