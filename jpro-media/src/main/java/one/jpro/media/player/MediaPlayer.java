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

    /**
     * Retrieves the muteProperty value.
     *
     * @return the mute setting
     */
    boolean isMute();

    /**
     * Sets the muteProperty value.
     *
     * @param value the mute setting
     */
    void setMute(boolean value);

    /**
     * Whether the player audio is muted. A value of <code>true</code> indicates
     * that audio is <i>not</i> being produced. The value of this property has
     * no effect on {@link #volumeProperty volume}, i.e., if the audio is muted and then
     * unmuted, audio playback will resume at the same audible level provided
     * of course that the <code>volume</code> property has not been modified
     * meanwhile. The default value is <code>false</code>.
     */
    BooleanProperty muteProperty();

    /**
     * Retrieves the current player status.
     *
     * @return the playback {@link Status}
     */
    Status getStatus();

    /**
     * The current state of the MediaPlayer.
     */
    ReadOnlyObjectProperty<Status> statusProperty();

    String getSource();

    ReadOnlyStringProperty sourceProperty();

    Duration getDuration();

    /**
     * The total duration of play time if allowed to play until finished.
     * If the Media duration is UNKNOWN, then this will likewise be UNKNOWN.
     *
     * @return the duration of the media
     */
    ReadOnlyObjectProperty<Duration> durationProperty();

    /**
     * Retrieves the audio playback volume.
     * The default value is <code>1.0</code>.
     *
     * @return the audio volume
     */
    double getVolume();

    /**
     * Sets the audio playback volume. Its effect will be clamped to the range
     * <code>[0.0,&nbsp;1.0]</code>.
     *
     * @param value the volume
     */
    void setVolume(double value);

    DoubleProperty volumeProperty();

    /**
     * Retrieves the current media time.
     *
     * @return the current media time
     */
    Duration getCurrentTime();

    /**
     * The current media playback time. This property is read-only: use
     * {@link #seek(javafx.util.Duration)} to change playback to a different
     * stream position.
     *
     * @return the current playback time
     */
    ReadOnlyObjectProperty<Duration> currentTimeProperty();

    /**
     * Retrieves the {@link Status#READY} event handler.
     *
     * @return the event handler or <code>null</code>.
     */
    EventHandler<MediaPlayerEvent> getOnReady();

    /**
     * Sets the {@link Status#READY} event handler.
     * @param value the event handler or <code>null</code>.
     */
    void setOnReady(EventHandler<MediaPlayerEvent> value);

    /**
     * Event handler invoked when the status changes to <code>READY</code>.
     */
    ObjectProperty<EventHandler<MediaPlayerEvent>> onReadyProperty();

    EventHandler<MediaPlayerEvent> getOnPlaying();

    void setOnPlaying(EventHandler<MediaPlayerEvent> value);

    ObjectProperty<EventHandler<MediaPlayerEvent>> onPlayingProperty();

    /**
     * Retrieves the {@link Status#PAUSED} event handler.
     * @return the event handler or <code>null</code>.
     */
    EventHandler<MediaPlayerEvent> getOnPause();

    /**
     * Sets the {@link Status#PAUSED} event handler.
     * @param value the event handler or <code>null</code>.
     */
    void setOnPause(EventHandler<MediaPlayerEvent> value);

    /**
     * Event handler invoked when the status changes to <code>PAUSED</code>.
     */
    ObjectProperty<EventHandler<MediaPlayerEvent>> onPauseProperty();

    /**
     * Retrieves the {@link Status#STOPPED} event handler.
     *
     * @return the event handler or <code>null</code>.
     */
    EventHandler<MediaPlayerEvent> getOnStopped();

    /**
     * Sets the {@link Status#STOPPED} event handler.
     *
     * @param value the event handler or <code>null</code>.
     */
    void setOnStopped(EventHandler<MediaPlayerEvent> value);

    /**
     * Event handler invoked when the status changes to <code>STOPPED</code>.
     */
    ObjectProperty<EventHandler<MediaPlayerEvent>> onStoppedProperty();

    /**
     * Retrieves the {@link Status#STALLED} event handler.
     *
     * @return the event handler or <code>null</code>.
     */
    EventHandler<MediaPlayerEvent> getOnStalled();

    /**
     * Sets the {@link Status#STALLED} event handler.
     *
     * @param value the event handler or <code>null</code>.
     */
    void setOnStalled(EventHandler<MediaPlayerEvent> value);

    /**
     * Event handler invoked when the status changes to <code>STALLED</code>.
     */
    ObjectProperty<EventHandler<MediaPlayerEvent>> onStalledProperty();

    /**
     * Retrieves the event handler for errors.
     *
     * @return the event handler.
     */
    EventHandler<MediaPlayerEvent> getOnError();

    /**
     * Sets the event handler to be called when an error occurs.
     *
     * @param value the event handler or <code>null</code>.
     */
    void setOnError(EventHandler<MediaPlayerEvent> value);

    /**
     * Event handler invoked when an error occurs.
     */
    ObjectProperty<EventHandler<MediaPlayerEvent>> onErrorProperty();

    /**
     * Retrieve the value of the {@link #errorProperty error} property or <code>null</code>
     * if there is no error.
     *
     * @return a {@link MediaPlayerException} or <code>null</code>.
     */
    MediaPlayerException getError();

    /**
     * Observable property set to a {@link MediaPlayerException} if an error occurs.
     */
    ReadOnlyObjectProperty<MediaPlayerException> errorProperty();

    /**
     * Starts playing the media. If previously paused, then playback resumes
     * where it was paused. If playback was stopped, playback starts
     * from the beginning. When playing actually starts the
     * {@link #statusProperty status} will be set to {@link Status#PLAYING}.
     */
    void play();

    /**
     * Pauses the player. Once the player is actually paused the {@link #statusProperty status}
     * will be set to {@link Status#PAUSED}.
     */
    void pause();

    /**
     * Stops playing the media. This operation resets playback to zero.
     * Once the player is actually stopped, the {@link #statusProperty status}
     * will be set to {@link Status#STOPPED}. The only transitions out of <code>STOPPED</code> status
     * are to {@link Status#PAUSED} and {@link Status#PLAYING} which occur after
     * invoking {@link #pause()} or {@link #play()}, respectively.
     * While stopped, the player will not respond to playback position changes
     * requested by {@link #seek(Duration)}.
     */
    void stop();

    /**
     * Seeks the player to a new playback time. Invoking this method will have no effect
     * while the player status is {@link Status#STOPPED} or media duration is {@link Duration#INDEFINITE}.
     *
     * @param seekTime the requested playback time
     */
    void seek(Duration seekTime);
}
