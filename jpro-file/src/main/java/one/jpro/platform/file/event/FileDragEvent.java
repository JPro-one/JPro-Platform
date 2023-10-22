package one.jpro.platform.file.event;

import javafx.beans.NamedArg;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * File drag event.
 *
 * @author Besmir Beqiri
 */
public class FileDragEvent extends FileEvent {

    /**
     * Common supertype for all file drag event types.
     */
    public static final EventType<FileDragEvent> ANY = new EventType<>(FileEvent.ANY, "FILE_DRAG");

    /**
     * This event occurs when file drag gesture enters a node.
     */
    public static final EventType<FileDragEvent> FILE_DRAG_ENTERED =
            new EventType<>(FileDragEvent.ANY, "FILE_DRAG_ENTERED");

    /**
     * This event occurs when drag gesture exits a node.
     */
    public static final EventType<FileDragEvent> FILE_DRAG_EXITED =
            new EventType<>(FileDragEvent.ANY, "FILE_DRAG_EXITED");

    /**
     * Creates a new instance of the {@code FileDragEvent} class.
     *
     * @param source the source object that fired the event
     * @param target the target object to associate with the event
     * @param eventType the type of the event
     */
    public FileDragEvent(final @NamedArg("source") Object source,
                         final @NamedArg("target") EventTarget target,
                         final @NamedArg("eventType") EventType<FileDragEvent> eventType) {
        super(source, target, eventType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public EventType<FileDragEvent> getEventType() {
        return (EventType<FileDragEvent>) super.getEventType();
    }
}
