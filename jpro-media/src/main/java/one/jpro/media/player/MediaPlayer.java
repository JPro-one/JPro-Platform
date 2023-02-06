package one.jpro.media.player;

import com.jpro.webapi.WebAPI;
import javafx.beans.property.*;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.scene.media.MediaPlayer.Status;
import javafx.stage.Stage;
import javafx.util.Duration;
import one.jpro.media.MediaEngine;
import one.jpro.media.MediaSource;
import one.jpro.media.MediaView;
import one.jpro.media.event.MediaPlayerEvent;
import one.jpro.media.player.impl.FXMediaPlayer;
import one.jpro.media.player.impl.WebMediaPlayer;

/**
 * The MediaPlayer provides functionality to easily play media, both on
 * desktop/mobile and web platforms.
 * <p>
 * This class provides the controls for playing media.
 * It is used in combination with the {@link MediaSource} and {@link MediaView}
 * classes to display and control media playback. <code>MediaPlayer</code> does
 * not contain any visual elements so must be used in combination with the
 * {@link MediaView} class to view any video track which may be present.
 *
 * <p><code>MediaPlayer</code> provides the {@link #play()}, {@link #pause()},
 * {@link #stop()} and {@link #seek(javafx.util.Duration) seek()} controls
 * as playback functionalities. It also provides the {@link #muteProperty mute}
 * and {@link #volumeProperty volume} properties which control audio playback
 * characteristics. Use the {@link #statusProperty status} property to
 * determine the current status of the <code>MediaPlayer</code> and
 * {@link #currentTimeProperty currentTime} property to determine the current
 * time position of the media.
 *
 * <p>All operations of a <code>MediaPlayer</code> are inherently asynchronous.
 * When the given <code>MediaSource</code> is loaded, use the {@link #setOnReady(EventHandler)}
 * to get notified when the <code>MediaPlayer</code> is ready to play. Other
 * event handlers like {@link #setOnPlaying(EventHandler)}, {@link #setOnPause(EventHandler)},
 * {@link #setOnStopped(EventHandler)}, {@link #setOnEndOfMedia(EventHandler)} and
 * {@link #setOnStalled(EventHandler)} can be used to get notified of the
 * corresponding events.
 *
 * <p>The same <code>MediaPlayer</code> object may be shared among multiple
 * <code>MediaView</code>s. This will not affect the player itself. In
 * particular, the property settings of the view will not have any effect on
 * media playback.</p>
 *
 * @see MediaSource
 * @see MediaView
 *
 * @author Besmir Beqiri
 */
public interface MediaPlayer extends MediaEngine, EventTarget {

    /**
     * Creates a media player. If the application is running in a
     * browser via JPro server, then a web version of media player
     * is returned. If the application is not running inside the
     * browser than a desktop version of the media player is returned.
     *
     * @param stage the application stage
     * @param mediaSource the media source
     * @return a {@link MediaPlayer} object.
     */
    static MediaPlayer create(Stage stage, MediaSource mediaSource) {
        if (WebAPI.isBrowser()) {
            WebAPI webAPI = WebAPI.getWebAPI(stage);
            return new WebMediaPlayer(webAPI, mediaSource);
        }
        if (mediaSource.isLocal()) {
            return new FXMediaPlayer(mediaSource);
        }
        throw new IllegalArgumentException("Incorrect media source provided!");
    }

    /**
     * Sets the {@link #autoPlayProperty autoPlay} property value.
     * @param value whether to enable auto-playback
     */
    void setAutoPlay(boolean value);

    /**
     * Retrieves the {@link #autoPlayProperty autoPlay} property value.
     * @return the value.
     */
    boolean isAutoPlay();

    /**
     * Whether playing should start as soon as possible. For a new player this
     * will occur once the player has reached the READY state. The default
     * value is <code>false</code>.
     *
     * @see javafx.scene.media.MediaPlayer.Status
     */
    BooleanProperty autoPlayProperty();

    /**
     * Retrieves the current media source.
     *
     * @return {@link MediaSource} object
     */
    MediaSource getMediaSource();

    /**
     * The media source for the current player. It holds the information
     * on where it resource is located and accessed either via a string
     * form URI locally, or via {@link WebAPI.JSFile} remotely.
     */
    ReadOnlyObjectProperty<MediaSource> mediaSourceProperty();

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
     * The current status of the MediaPlayer.
     */
    ReadOnlyObjectProperty<Status> statusProperty();

    /**
     * Gets the duration for the given media source. If the duration cannot be
     * obtained when this method is invoked, a {@link Duration#UNKNOWN} value will be returned.
     */
    Duration getDuration();

    /**
     * The total duration of play time if allowed to play until finished.
     * If the Media duration is {@link Duration#UNKNOWN},
     * then this will likewise be {@link Duration#UNKNOWN}.
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
     * {@link #seek(Duration)} to change playback to a different
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
     *
     * @param value the event handler or <code>null</code>.
     */
    void setOnReady(EventHandler<MediaPlayerEvent> value);

    /**
     * Event handler invoked when the status changes to <code>READY</code>.
     */
    ObjectProperty<EventHandler<MediaPlayerEvent>> onReadyProperty();

    /**
     * Retrieves the {@link Status#PLAYING} event handler.
     *
     * @return the event handler or <code>null</code>.
     */
    EventHandler<MediaPlayerEvent> getOnPlaying();

    /**
     * Sets the {@link Status#PLAYING} event handler.
     *
     * @param value the event handler or <code>null</code>.
     */
    void setOnPlaying(EventHandler<MediaPlayerEvent> value);

    /**
     * Event handler invoked when the status changes to <code>PLAYING</code>.
     */
    ObjectProperty<EventHandler<MediaPlayerEvent>> onPlayingProperty();

    /**
     * Retrieves the {@link Status#PAUSED} event handler.
     *
     * @return the event handler or <code>null</code>.
     */
    EventHandler<MediaPlayerEvent> getOnPause();

    /**
     * Sets the {@link Status#PAUSED} event handler.
     *
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
     * Retrievers the event handler invoked when the end of media is reached naturally.
     *
     * @return the event handler or <code>null</code>.
     */
    EventHandler<MediaPlayerEvent> getOnEndOfMedia();

    /**
     * Sets the event handler invoked when the end of media is reached naturally.
     *
     * @param value the event handler or <code>null</code>.
     */
    void setOnEndOfMedia(EventHandler<MediaPlayerEvent> value);

    /**
     * Event handler invoked when the end of media is reached naturally.
     */
    ObjectProperty<EventHandler<MediaPlayerEvent>> onEndOfMediaProperty();

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
     * Retrieve the value of the {@link #errorProperty error}
     * property or <code>null</code> if there is no error.
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
