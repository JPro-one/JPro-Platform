package one.jpro.platform.media.event;

import javafx.beans.NamedArg;
import javafx.event.Event;
import javafx.event.EventType;
import one.jpro.platform.media.player.MediaPlayer;

/**
 * Media player event.
 *
 * @author Besmir Beqiri
 */
public class MediaPlayerEvent extends Event {

    /**
     * Common supertype for all media player's event types.
     */
    public static final EventType<MediaPlayerEvent> ANY = new EventType<>(Event.ANY, "MEDIA_PLAYER");

    /**
     * This event occurs when the media player is ready to play.
     */
    public static final EventType<MediaPlayerEvent> MEDIA_PLAYER_READY =
            new EventType<>(ANY, "MEDIA_PLAYER_READY");

    /**
     * Fired when there are fatal errors that stop playing.
     */
    public static final EventType<MediaPlayerEvent> MEDIA_PLAYER_ERROR =
            new EventType<>(ANY, "MEDIA_PLAYER_ERROR");

    /**
     * Fired when media player starts.
     */
    public static final EventType<MediaPlayerEvent> MEDIA_PLAYER_PLAY =
            new EventType<>(ANY, "MEDIA_PLAYER_PLAY");

    /**
     * Fired when media playing is paused.
     */
    public static final EventType<MediaPlayerEvent> MEDIA_PLAYER_PAUSE =
            new EventType<>(ANY, "MEDIA_PLAYER_PAUSE");

    /**
     * Fired when media playing ends, either when the MediaPlayer ends,
     * or after the {@link MediaPlayer#stop()} method is called.
     */
    public static final EventType<MediaPlayerEvent> MEDIA_PLAYER_STOP =
            new EventType<>(ANY, "MEDIA_PLAYER_STOP");

    /**
     * Fired when the player <code>currentTime</code> reaches
     * <code>stopTime</code> and <i>will be</i> repeating.
     */
    public static final EventType<MediaPlayerEvent> MEDIA_PLAYER_REPEAT =
            new EventType<>(ANY, "MEDIA_PLAYER_REPEAT");

    /**
     * Fired when media playback has ended.
     */
    public static final EventType<MediaPlayerEvent> MEDIA_PLAYER_END_OF_MEDIA =
            new EventType<>(ANY, "MEDIA_PLAYER_END_OF_MEDIA");

    /**
     * The stalled event occurs when the media player is trying
     * to get media data, but data is not available.
     */
    public static final EventType<MediaPlayerEvent> MEDIA_PLAYER_STALLED =
            new EventType<>(ANY, "MEDIA_PLAYER_STALLED");

    /**
     * Creates new instance of MediaPlayerEvent.
     *
     * @param source event source
     * @param eventType event target
     */
    public MediaPlayerEvent(final @NamedArg("source") MediaPlayer source,
                            final @NamedArg("eventType") EventType<? extends Event> eventType) {
        super(source, source, eventType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public EventType<? extends MediaPlayerEvent> getEventType() {
        return (EventType<? extends MediaPlayerEvent>) super.getEventType();
    }
}
