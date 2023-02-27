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
 * <p>For finite duration media, playback may be positioned at any point in time
 * between <code>0.0</code> and the duration of the media. <code>MediaPlayer</code>
 * refines this definition by adding the {@link #startTimeProperty startTime} and
 * {@link #stopTimeProperty stopTime}
 * properties which in effect define a virtual media source with time position
 * constrained to <code>[startTime,stopTime]</code>. Media playback
 * commences at <code>startTime</code> and continues to <code>stopTime</code>.
 * The interval defined by these two endpoints is termed a <i>cycle</i> with
 * duration being the difference of the stop and start times. This cycle
 * may be set to repeat a specific or indefinite number of times. The total
 * duration of media playback is then the product of the cycle duration and the
 * number of times the cycle is played. If the stop time of the cycle is reached
 * and the cycle is to be played again, the event handler registered with the
 * {@link #onRepeatProperty onRepeat} property is invoked. If the stop time is
 * reached, then the event handler registered with the {@link #onEndOfMediaProperty onEndOfMedia}
 * property is invoked regardless of whether the cycle is to be repeated or not.
 * A zero-relative index of which cycle is presently being played is maintained
 * by {@link #currentCountProperty currentCount}.
 * </p>
 *
 * <p>All operations of a <code>MediaPlayer</code> are inherently asynchronous.
 * When the given <code>MediaSource</code> is loaded, use the {@link #setOnReady(EventHandler)}
 * to get notified when the <code>MediaPlayer</code> is ready to play. Other
 * event handlers like {@link #setOnPlaying(EventHandler)}, {@link #setOnPaused(EventHandler)},
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
     * A value representing an effectively infinite number of playback cycles.
     * When {@link #cycleCountProperty cycleCount} is set to this value, the player
     * will replay the <code>MediaSource</code> until stopped or paused.
     */
    int INDEFINITE = -1; // Note: this is a count, not a Duration.

    /**
     * Creates a media player. If the application is running in a
     * browser via JPro server, then a web version of media player
     * is returned. If the application is not running inside the
     * browser than a desktop version of the media player is returned.
     *
     * @param stage       the application stage
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
     * Sets the {@link #autoPlayProperty autoPlay} property value.
     *
     * @param value whether to enable auto-playback
     */
    void setAutoPlay(boolean value);

    /**
     * Retrieves the {@link #autoPlayProperty autoPlay} property value.
     *
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
     * Retrieves the start time. The default value is <code>Duration.ZERO</code>.
     *
     * @return the start time
     */
    Duration getStartTime();

    /**
     * Sets the start time. Its effect will be clamped to the range
     * <code>[{@link Duration#ZERO},&nbsp;{@link #stopTimeProperty stopTime})</code>.
     * Invoking this method will have no effect if media duration is {@link Duration#INDEFINITE}.
     *
     * @param value the start time
     */
    void setStartTime(Duration value);

    /**
     * The time offset where media should start playing, or restart from when
     * repeating. When playback is stopped, the current time is reset to this
     * value. If this value is positive, then the first time the media is
     * played there might be a delay before playing begins unless the play
     * position can be set to an arbitrary time within the media. This could
     * occur for example for a video which does not contain a lookup table
     * of the offsets of intra-frames in the video stream. In such a case the
     * video frames would need to be skipped over until the position of the
     * first intra-frame before the start time was reached. The default value is
     * <code>Duration.ZERO</code>.
     *
     * <p>Constraints: <code>0&nbsp;&le;&nbsp;startTime&nbsp;&lt;&nbsp;{@link #stopTimeProperty stopTime}</code>
     */
    ObjectProperty<Duration> startTimeProperty();

    /**
     * Retrieves the stop time. The default value is <code>{@link #getDuration()}</code>.
     * Note that <code>{@link MediaPlayer#durationProperty}</code> may have the value
     * <code>Duration.UNKNOWN</code> if media initialization is not complete.
     *
     * @return the stop time
     */
    Duration getStopTime();

    /**
     * Sets the stop time. Its effect will be clamped to
     * the range <code>({@link #startTimeProperty startTime},&nbsp;{@link MediaPlayer#durationProperty Media.duration}]</code>.
     * Invoking this method will have no effect if media duration is {@link Duration#INDEFINITE}.
     *
     * @param value the stop time
     */
    void setStopTime(Duration value);

    /**
     * The time offset where media should stop playing or restart when repeating.
     * The default value is <code>{@link #getDuration()}</code>.
     *
     * <p>Constraints: <code>{@link #startTimeProperty startTime}&nbsp;&lt;&nbsp;stopTime&nbsp;&le;&nbsp;{@link MediaPlayer#durationProperty MediaPlayer.duration}</code>
     */
    ObjectProperty<Duration> stopTimeProperty();

    /**
     * Retrieves the cycle count.
     *
     * @return the cycle count.
     */
    int getCycleCount();

    /**
     * Sets the cycle count. Its effect will be constrained to <code>[1,{@link Integer#MAX_VALUE}]</code>.
     * Invoking this method will have no effect if media duration is {@link Duration#INDEFINITE}.
     *
     * @param value the cycle count
     */
    void setCycleCount(int value);

    /**
     * The number of times the media will be played.  By default,
     * <code>cycleCount</code> is set to <code>1</code>
     * meaning the media will only be played once. Setting <code>cycleCount</code>
     * to a value greater than 1 will cause the media to play the given number
     * of times or until stopped. If set to {@link #INDEFINITE INDEFINITE},
     * playback will repeat until stop() or pause() is called.
     *
     * <p>constraints: <code>cycleCount&nbsp;&ge;&nbsp;1</code>
     */
    IntegerProperty cycleCountProperty();

    /**
     * Retrieves the index of the current cycle.
     *
     * @return the current cycle index
     */
    int getCurrentCount();

    /**
     * The number of completed playback cycles. On the first pass, the value should be 0.
     * On the second pass, the value should be 1 and so on. It is incremented at the end
     * of each cycle just prior to seeking back to {@link #startTimeProperty startTime},
     * i.e., when {@link #stopTimeProperty stopTime} or the end of media has been reached.
     */
    ReadOnlyIntegerProperty currentCountProperty();

    /**
     * Retrieves the cycle duration in seconds.
     *
     * @return the cycle duration
     */
    Duration getCycleDuration();

    /**
     * The amount of time between the {@link #startTimeProperty startTime} and
     * {@link #stopTimeProperty stopTime} of this player.
     * For the total duration of the MediaSource use the
     * {@link #durationProperty MediaPlayer.duration} property.
     */
    ReadOnlyObjectProperty<Duration> cycleDurationProperty();

    /**
     * Retrieves the total playback duration including all cycles (repetitions).
     *
     * @return the total playback duration
     */
    Duration getTotalDuration();

    /**
     * The total amount of play time if allowed to play until finished. If
     * <code>cycleCount</code> is set to <code>INDEFINITE</code> then this will
     * also be INDEFINITE. If the media resource duration is UNKNOWN, then this will
     * likewise be UNKNOWN. Otherwise, total duration will be the product of
     * cycleDuration and cycleCount.
     */
    ReadOnlyObjectProperty<Duration> totalDurationProperty();

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
    EventHandler<MediaPlayerEvent> getOnPaused();

    /**
     * Sets the {@link Status#PAUSED} event handler.
     *
     * @param value the event handler or <code>null</code>.
     */
    void setOnPaused(EventHandler<MediaPlayerEvent> value);

    /**
     * Event handler invoked when the status changes to <code>PAUSED</code>.
     */
    ObjectProperty<EventHandler<MediaPlayerEvent>> onPausedProperty();

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
     * Retrieves the repeat event handler.
     *
     * @return the event handler or <code>null</code>.
     */
    EventHandler<MediaPlayerEvent> getOnRepeat();

    /**
     * Sets the repeat event handler.
     *
     * @param value the event handler or <code>null</code>.
     */
    void setOnRepeat(EventHandler<MediaPlayerEvent> value);

    /**
     * Event handler invoked when the player <code>currentTime</code> reaches
     * <code>stopTime</code> and <i>will be</i> repeating. This callback is made
     * prior to seeking back to <code>startTime</code>.
     */
    ObjectProperty<EventHandler<MediaPlayerEvent>> onRepeatProperty();

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
