package one.jpro.sound.impl.javafx;

import one.jpro.sound.AudioClip;

public class JavaFXAudioClip implements AudioClip {
    javafx.scene.media.AudioClip clip;

    public JavaFXAudioClip(String source) {
        clip = new javafx.scene.media.AudioClip(source);
    }

    public void play() {
        clip.play();
    }

    public void setVolume(double volume) {
        clip.setVolume(volume);
    }
}
