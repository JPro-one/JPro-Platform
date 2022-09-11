package one.jpro.sound.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import one.jpro.sound.Media;
import one.jpro.sound.MediaPlayer;
import one.jpro.sound.AudioClip;

public class JProSoundSample extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        VBox pin = new VBox();

        pin.getChildren().add(new Label(("Hello!")));

        //String dong = getClass().getResource("/one/jpro/sound/example/audio/dong.mp3").toExternalForm().toString();
        String dong = getClass().getResource("/one/jpro/sound/example/audio/timer.wav").toExternalForm().toString();
        Media media = Media.getMedia(dong, primaryStage);
        MediaPlayer player = MediaPlayer.getMediaPlayer(media);

        AudioClip clip = AudioClip.getAudioClip(dong, primaryStage);

        Button buttonPlayer = new Button("mediaplayer.play()");
        buttonPlayer.setOnAction(e -> {
            player.play();
        });
        pin.getChildren().add(buttonPlayer);

        Button buttonStop = new Button("mediaplayer.stop()");
        buttonStop.setOnAction(e -> {
            player.stop();
        });
        pin.getChildren().add(buttonStop);



        Button buttonClip = new Button("audioclip.play()");
        buttonClip.setOnAction(e -> {
            clip.play();
        });
        pin.getChildren().add(buttonClip);



        Scene scene = new Scene(pin);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
