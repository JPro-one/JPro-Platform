package one.jpro.media.recorder.impl;

import com.sun.javafx.event.EventHandlerManager;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import one.jpro.media.MediaSource;
import one.jpro.media.event.MediaRecorderEvent;
import one.jpro.media.recorder.MediaRecorder;
import one.jpro.media.recorder.MediaRecorderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link MediaRecorder} implementations.
 *
 * @author Besmir Beqiri
 */
abstract class BaseMediaRecorder implements MediaRecorder {

    private final Logger log = LoggerFactory.getLogger(BaseMediaRecorder.class);

    // media source property (read-only)
    ReadOnlyObjectWrapper<MediaSource> mediaSource;

    @Override
    public MediaSource getMediaSource() {
        return (mediaSource == null) ? null : mediaSource.get();
    }

    void setMediaSource(MediaSource value) {
        mediaResourcePropertyImpl().set(value);
    }

    @Override
    public ReadOnlyObjectProperty<MediaSource> mediaSourceProperty() {
        return mediaResourcePropertyImpl().getReadOnlyProperty();
    }

    ReadOnlyObjectWrapper<MediaSource> mediaResourcePropertyImpl() {
        if (mediaSource == null) {
            mediaSource = new ReadOnlyObjectWrapper<>(this, "mediaSource");
        }
        return mediaSource;
    }

    // state property
    private ReadOnlyObjectWrapper<State> state;

    @Override
    public State getState() {
        return (state == null) ? State.INACTIVE : state.get();
    }

    protected void setState(State value) {
        statePropertyImpl().set(value);
    }

    /**
     * The current state of the MediaRecorder object (inactive, recording, or paused.)
     */
    @Override
    public ReadOnlyObjectProperty<State> stateProperty() {
        return statePropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<State> statePropertyImpl() {
        if (state == null) {
            state = new ReadOnlyObjectWrapper<>(this, "state", State.INACTIVE);
        }
        return state;
    }

    // On data available event handler
    private ObjectProperty<EventHandler<MediaRecorderEvent>> onDataAvailable;

    @Override
    public final EventHandler<MediaRecorderEvent> getOnDataAvailable() {
        return (onDataAvailable == null) ? null : onDataAvailable.get();
    }

    @Override
    public final void setOnDataAvailable(EventHandler<MediaRecorderEvent> value) {
        onDataAvailableProperty().set(value);
    }

    @Override
    public final ObjectProperty<EventHandler<MediaRecorderEvent>> onDataAvailableProperty() {
        if (onDataAvailable == null) {
            onDataAvailable = new SimpleObjectProperty<>(this, "onDataAvailable") {

                @Override
                protected void invalidated() {
                    eventHandlerManager.setEventHandler(MediaRecorderEvent.MEDIA_RECORDER_DATA_AVAILABLE, get());
                }
            };
        }
        return onDataAvailable;
    }

    // On start event handler
    private ObjectProperty<EventHandler<MediaRecorderEvent>> onStart;

    @Override
    public final EventHandler<MediaRecorderEvent> getOnStart() {
        return (onStart == null) ? null : onStart.get();
    }

    @Override
    public final void setOnStart(EventHandler<MediaRecorderEvent> value) {
        onStartProperty().set(value);
    }

    @Override
    public final ObjectProperty<EventHandler<MediaRecorderEvent>> onStartProperty() {
        if (onStart == null) {
            onStart = new SimpleObjectProperty<>(this, "onStart"){

                @Override
                protected void invalidated() {
                    eventHandlerManager.setEventHandler(MediaRecorderEvent.MEDIA_RECORDER_START, get());
                }
            };
        }
        return onStart;
    }

    // On pause event handler
    private ObjectProperty<EventHandler<MediaRecorderEvent>> onPause;

    @Override
    public final EventHandler<MediaRecorderEvent> getOnPause() {
        return (onPause == null) ? null : onPause.get();
    }

    @Override
    public final void setOnPause(EventHandler<MediaRecorderEvent> value) {
        onPauseProperty().set(value);
    }

    @Override
    public final ObjectProperty<EventHandler<MediaRecorderEvent>> onPauseProperty() {
        if (onPause == null) {
            onPause = new SimpleObjectProperty<>(this, "onPause") {

                @Override
                protected void invalidated() {
                    eventHandlerManager.setEventHandler(MediaRecorderEvent.MEDIA_RECORDER_PAUSE, get());
                }
            };
        }
        return onPause;
    }

    // On resume event handler
    private ObjectProperty<EventHandler<MediaRecorderEvent>> onResume;

    @Override
    public final EventHandler<MediaRecorderEvent> getOnResume() {
        return (onResume == null) ? null : onResume.get();
    }

    @Override
    public final void setOnResume(EventHandler<MediaRecorderEvent> value) {
        onResumeProperty().set(value);
    }

    @Override
    public final ObjectProperty<EventHandler<MediaRecorderEvent>> onResumeProperty() {
        if (onResume == null) {
            onResume = new SimpleObjectProperty<>(this, "onResume") {

                @Override
                protected void invalidated() {
                    eventHandlerManager.setEventHandler(MediaRecorderEvent.MEDIA_RECORDER_RESUME, get());
                }
            };
        }
        return onResume;
    }

    // On stopped event handler
    private ObjectProperty<EventHandler<MediaRecorderEvent>> onStopped;

    @Override
    public final EventHandler<MediaRecorderEvent> getOnStopped() {
        return (onStopped == null) ? null : onStopped.get();
    }

    @Override
    public final void setOnStopped(EventHandler<MediaRecorderEvent> value) {
        onStoppedProperty().set(value);
    }

    @Override
    public final ObjectProperty<EventHandler<MediaRecorderEvent>> onStoppedProperty() {
        if (onStopped == null) {
            onStopped = new SimpleObjectProperty<>(this, "onStopped") {

                @Override
                protected void invalidated() {
                    eventHandlerManager.setEventHandler(MediaRecorderEvent.MEDIA_RECORDER_STOP, get());
                }
            };
        }
        return onStopped;
    }

    // On error event handler
    private ObjectProperty<EventHandler<MediaRecorderEvent>> onError;

    @Override
    public final EventHandler<MediaRecorderEvent> getOnError() {
        return (onError == null) ? null : onError.get();
    }

    @Override
    public final void setOnError(EventHandler<MediaRecorderEvent> value) {
        onErrorProperty().set(value);
    }

    @Override
    public final ObjectProperty<EventHandler<MediaRecorderEvent>> onErrorProperty() {
        if (onError == null) {
            onError = new SimpleObjectProperty<>(this, "onError") {

                @Override
                protected void invalidated() {
                    eventHandlerManager.setEventHandler(MediaRecorderEvent.MEDIA_RECORDER_ERROR, get());
                }
            };
        }
        return onError;
    }

    // Error property
    private ReadOnlyObjectWrapper<MediaRecorderException> error;

    @Override
    public final MediaRecorderException getError() {
        return (error == null) ? null : error.get();
    }

    final void setError(MediaRecorderException error) {
        errorPropertyImpl().set(error);
    }

    @Override
    public final ReadOnlyObjectProperty<MediaRecorderException> errorProperty() {
        return errorPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<MediaRecorderException> errorPropertyImpl() {
        if (error == null) {
            error = new ReadOnlyObjectWrapper<>(this, "error") {

                @Override
                protected void invalidated() {
                    final MediaRecorderException exception = get();
                    log.error(exception.toString(), exception);
                }
            };
        }
        return error;
    }

    private final EventHandlerManager eventHandlerManager = new EventHandlerManager(this);

    @Override
    public final EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        return tail.prepend(eventHandlerManager);
    }
}
