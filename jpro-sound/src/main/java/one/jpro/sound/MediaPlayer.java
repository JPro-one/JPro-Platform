package one.jpro.sound;

import com.jpro.webapi.WebAPI;
import one.jpro.sound.impl.javafx.JavaFXMedia;
import one.jpro.sound.impl.javafx.JavaFXMediaPlayer;
import one.jpro.sound.impl.jpro.JProMedia;
import one.jpro.sound.impl.jpro.JProSound;

public interface MediaPlayer {
    public static MediaPlayer getMediaPlayer(Media media) {
        if(WebAPI.isBrowser()) {
            return new JProSound((JProMedia) media, ((JProMedia) media).webapi);
        } else {
            return new JavaFXMediaPlayer((JavaFXMedia) media);
        }
    }

    public void setVolume(double volume);
    public void setLoop(boolean loop);
    public void play();
    public void pause();
    public void stop();

}
