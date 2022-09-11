package one.jpro.sound.impl.javafx;

import one.jpro.sound.MediaPlayer;

public class JavaFXMediaPlayer implements MediaPlayer {

    javafx.scene.media.MediaPlayer mediaPlayer;

    public JavaFXMediaPlayer(JavaFXMedia media) {
        mediaPlayer = new javafx.scene.media.MediaPlayer(media.media);
    }

    public void setVolume(double volume) {
        mediaPlayer.setVolume(volume);
    }

    public void setLoop(boolean loop) {
        mediaPlayer.setCycleCount(javafx.scene.media.MediaPlayer.INDEFINITE);
    }

    public void play() {
        mediaPlayer.play();
    }

    public void pause() {
        mediaPlayer.pause();
    }

    public void stop() {
        mediaPlayer.stop();
    }

}
