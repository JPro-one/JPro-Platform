package one.jpro.media.recorder.event;

import javafx.beans.NamedArg;
import javafx.event.Event;
import javafx.event.EventType;
import one.jpro.media.recorder.MediaRecorder;

/**
 * Media recorder event.
 *
 * @author Besmir Beqiri
 */
public class MediaRecorderEvent extends Event {

    /**
     * Common supertype for all media stream event types.
     */
    public static final EventType<MediaRecorderEvent> ANY = new EventType<>(Event.ANY, "MEDIA_RECORDER");

    /**
     * Fires periodically each time timeslice milliseconds of media have been recorded (or when the entire media
     * has been recorded, if timeslice wasn't specified). The event, of type BlobEvent, contains the recorded
     * media in its data property.
     */
    public static final EventType<MediaRecorderEvent> MEDIA_RECORDER_DATA_AVAILABLE =
            new EventType<>(ANY, "MEDIA_RECORDER_DATA_AVAILABLE");

    /**
     * Fired when there are fatal errors that stop recording.
     * The received event is based on the MediaRecorderErrorEvent interface, whose error property
     * contains a DOMException that describes the actual error that occurred.
     */
    public static final EventType<MediaRecorderEvent> MEDIA_RECORDER_ERROR =
            new EventType<>(ANY, "MEDIA_RECORDER_ERROR");

    /**
     * Fired when media recording starts.
     */
    public static final EventType<MediaRecorderEvent> MEDIA_RECORDER_START =
            new EventType<>(ANY, "MEDIA_RECORDER_PAUSED");

    /**
     * Fired when media recording ends, either when the MediaStream ends,
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
