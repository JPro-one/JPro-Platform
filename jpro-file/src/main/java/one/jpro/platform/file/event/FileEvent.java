package one.jpro.platform.file.event;

import javafx.beans.NamedArg;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.input.InputEvent;

/**
 * File event.
 *
 * @author Besmir Beqiri
 */
public class FileEvent extends InputEvent {

    /**
     * Common supertype for all file event types.
     */
    public static final EventType<FileEvent> ANY = new EventType<>(InputEvent.ANY, "FILE");

    private final transient DataTransfer dataTransfer;

    /**
     * Creates a new instance of the {@code FileEvent} with the
     * specified event source, target and type.
     *
     * @param source the source object that fired the event
     * @param target the target object to associate with the event
     * @param eventType the type of the event
     */
    public FileEvent(final @NamedArg("source") Object source,
                     final @NamedArg("target") EventTarget target,
                     final @NamedArg("eventType") EventType<? extends FileEvent> eventType) {
        super(source, target, eventType);
        this.dataTransfer = new DataTransfer();
    }

    /**
     * Returns the data transfer instance associated with this event.
     *
     * @return the data transfer instance
     */
    public final DataTransfer getDataTransfer() {
        return dataTransfer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EventType<? extends FileEvent> getEventType() {
        return (EventType<? extends FileEvent>) super.getEventType();
    }
}
