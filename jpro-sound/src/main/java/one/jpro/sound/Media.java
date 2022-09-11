package one.jpro.sound;

import com.jpro.webapi.WebAPI;
import javafx.stage.Stage;
import one.jpro.sound.impl.javafx.JavaFXAudioClip;
import one.jpro.sound.impl.javafx.JavaFXMedia;
import one.jpro.sound.impl.jpro.JProMedia;

import java.net.URL;

public interface Media {
    public static Media getMedia(String url, Stage context) {
        if(WebAPI.isBrowser()) {
            try {
                return new JProMedia(context, new URL(url));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            return new JavaFXMedia(url);
        }
    }
}
