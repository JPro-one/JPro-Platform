package one.jpro.example.media;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import one.jpro.media.MediaSource;
import one.jpro.media.MediaView;
import one.jpro.media.player.MediaPlayer;

/**
 * Simple media player application using JPro-Media library.
 * This is the example shown on the documentation.
 *
 * @author Besmir Beqiri
 */
public class MediaPlayerApplication extends Application {

    public void start(Stage stage) {
        stage.setTitle("JPro Media Player");

        // Provide a media source
        String source = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
        MediaSource mediaSource = new MediaSource(source);

        // Create the media player and the media view.
        MediaPlayer mediaPlayer = MediaPlayer.create(stage, mediaSource);
        MediaView mediaView = MediaView.create(mediaPlayer);

        // Media controls
        Button playPauseButton = new Button("Play");
        playPauseButton.setDisable(true);
        Button stopButton = new Button("Stop");
        stopButton.setDisable(true);

        // Control events
        stopButton.setOnAction(event -> mediaPlayer.stop());

        // Event handlers
        mediaPlayer.setOnReady(event -> {
            playPauseButton.setOnAction(event2 -> mediaPlayer.play());
            playPauseButton.setDisable(false);
            stopButton.setDisable(false);
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
        mediaPlayer.setOnError(event -> System.out.println("Error: " + mediaPlayer.getError()));

        // User interface
        FlowPane controlsPane = new FlowPane(playPauseButton, stopButton);
        controlsPane.setAlignment(Pos.CENTER);
        controlsPane.setHgap(8);
        controlsPane.setVgap(8);
        controlsPane.setPadding(new Insets(8));
        VBox rootPane = new VBox(mediaView, controlsPane);
        VBox.setVgrow(mediaView, Priority.ALWAYS);

        Scene scene = new Scene(rootPane, 1140, 640);
        stage.setScene(scene);
        stage.show();
    }
}
