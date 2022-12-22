package one.jpro.media.player;

import com.jpro.webapi.WebAPI;
import javafx.beans.property.*;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.scene.media.MediaPlayer.Status;
import javafx.stage.Stage;
import javafx.util.Duration;
import one.jpro.media.event.MediaPlayerEvent;
import one.jpro.media.player.impl.FXMediaPlayer;
import one.jpro.media.player.impl.WebMediaPlayer;

/**
 * Media player interface.
 *
 * @author Besmir Beqiri
 */
public interface MediaPlayer extends EventTarget {

    /**
     * Creates a media player (JavaFX/Desktop version).
     *
     * @param source the media source uri
     * @return a {@link MediaPlayer} object.
     */
    static MediaPlayer create(String source) {
        return new FXMediaPlayer(source);
    }

    /**
     * Creates a media player with the given JPro WebAPI.
     * If the application is running in a browser with JPro,
     * then a web version of {@link MediaPlayer} is returned.
     * If the application is not running inside the browser than
     * a desktop version of the media player is returned.
     *
     * @param stage the application stage
     * @param source the media source uri
     * @return a {@link MediaPlayer} object.
     */
    static MediaPlayer create(Stage stage, String source) {
        if (WebAPI.isBrowser()) {
            WebAPI webAPI = WebAPI.getWebAPI(stage);
            return new WebMediaPlayer(webAPI, source);
        }
        return new FXMediaPlayer(source);
    }

    boolean isMute();

    void setMute(boolean value);

    BooleanProperty muteProperty();

    Status getStatus();

    ReadOnlyObjectProperty<Status> statusProperty();

    String getSource();

    ReadOnlyStringProperty sourceProperty();

    Duration getDuration();

    ReadOnlyObjectProperty<Duration> durationProperty();

    double getVolume();

    void setVolume(double value);

    DoubleProperty volumeProperty();

    Duration getCurrentTime();

    ReadOnlyObjectProperty<Duration> currentTimeProperty();

    EventHandler<MediaPlayerEvent> getOnReady();

    void setOnReady(EventHandler<MediaPlayerEvent> value);

    ObjectProperty<EventHandler<MediaPlayerEvent>> onReadyProperty();

    EventHandler<MediaPlayerEvent> getOnPlay();

    void setOnPlay(EventHandler<MediaPlayerEvent> value);

    ObjectProperty<EventHandler<MediaPlayerEvent>> onPlayProperty();

    EventHandler<MediaPlayerEvent> getOnPause();

    void setOnPause(EventHandler<MediaPlayerEvent> value);

    ObjectProperty<EventHandler<MediaPlayerEvent>> onPauseProperty();

    EventHandler<MediaPlayerEvent> getOnStalled();

    void setOnStalled(EventHandler<MediaPlayerEvent> value);

    ObjectProperty<EventHandler<MediaPlayerEvent>> onStalledProperty();

    EventHandler<MediaPlayerEvent> getOnStopped();

    void setOnStopped(EventHandler<MediaPlayerEvent> value);

    ObjectProperty<EventHandler<MediaPlayerEvent>> onStoppedProperty();

    EventHandler<MediaPlayerEvent> getOnError();

    void setOnError(EventHandler<MediaPlayerEvent> value);

    ObjectProperty<EventHandler<MediaPlayerEvent>> onErrorProperty();

    MediaPlayerException getError();

    ReadOnlyObjectProperty<MediaPlayerException> errorProperty();

    void play();

    void pause();

    void stop();

    void seek(Duration seekTime);
}
