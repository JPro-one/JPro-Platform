package one.jpro.sound.impl.javafx;

import one.jpro.sound.Media;

public class JavaFXMedia implements Media {
    javafx.scene.media.Media media;

    public JavaFXMedia(String url) {
        media = new javafx.scene.media.Media(url);
    }

}
