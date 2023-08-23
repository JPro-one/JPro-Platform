package one.jpro.platform.media.event;

import javafx.beans.NamedArg;
import javafx.event.Event;
import javafx.event.EventType;
import one.jpro.platform.media.recorder.MediaRecorder;

/**
 * Media recorder event.
 *
 * @author Besmir Beqiri
 */
public class MediaRecorderEvent extends Event {

    /**
     * Common supertype for all media recorder's event types.
     */
    public static final EventType<MediaRecorderEvent> ANY = new EventType<>(Event.ANY, "MEDIA_RECORDER");

    /**
     * This event occurs when the media recorder is ready to record.
     */
    public static final EventType<MediaRecorderEvent> MEDIA_RECORDER_READY =
            new EventType<>(ANY, "MEDIA_RECORDER_READY");

    /**
     * Fires periodically each time timeslice milliseconds of media have been recorded (or when the entire media
     * has been recorded, if timeslice wasn't specified). The event, of type BlobEvent, contains the recorded
     * media in its data property.
     */
    public static final EventType<MediaRecorderEvent> MEDIA_RECORDER_DATA_AVAILABLE =
            new EventType<>(ANY, "MEDIA_RECORDER_DATA_AVAILABLE");

    /**
     * Fired when there are fatal errors that stop recording.
     */
    public static final EventType<MediaRecorderEvent> MEDIA_RECORDER_ERROR =
            new EventType<>(ANY, "MEDIA_RECORDER_ERROR");

    /**
     * Fired when media recording starts.
     */
    public static final EventType<MediaRecorderEvent> MEDIA_RECORDER_START =
            new EventType<>(ANY, "MEDIA_RECORDER_START");

    /**
     * Fired when media recording ends, either when the MediaRecorder ends,
     * or after the MediaRecorder.stop() method is called.
     */
    public static final EventType<MediaRecorderEvent> MEDIA_RECORDER_STOP =
            new EventType<>(ANY, "MEDIA_RECORDER_STOP");

    /**
     * Fired when media recording is paused.
     */
    public static final EventType<MediaRecorderEvent> MEDIA_RECORDER_PAUSE =
            new EventType<>(ANY, "MEDIA_RECORDER_PAUSE");

    /**
     * Fired when media recording resumes after being paused.
     */
    public static final EventType<MediaRecorderEvent> MEDIA_RECORDER_RESUME =
            new EventType<>(ANY, "MEDIA_RECORDER_RESUME");

    /**
     * Creates new instance of MediaRecorderEvent.
     *
     * @param source event source
     * @param eventType event target
     */
    public MediaRecorderEvent(final @NamedArg("source") MediaRecorder source,
                              final @NamedArg("eventType") EventType<? extends Event> eventType) {
        super(source, source, eventType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public EventType<? extends MediaRecorderEvent> getEventType() {
        return (EventType<? extends MediaRecorderEvent>) super.getEventType();
    }
}
