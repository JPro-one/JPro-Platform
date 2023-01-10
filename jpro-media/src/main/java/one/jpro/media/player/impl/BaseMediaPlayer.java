package one.jpro.media.player.impl;

import com.sun.javafx.event.EventHandlerManager;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import one.jpro.media.MediaSource;
import one.jpro.media.event.MediaPlayerEvent;
import one.jpro.media.player.MediaPlayer;
import one.jpro.media.player.MediaPlayerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link MediaPlayer} implementations.
 *
 * @author Besmir Beqiri
 */
abstract class BaseMediaPlayer implements MediaPlayer {

    private final Logger log = LoggerFactory.getLogger(BaseMediaPlayer.class);

    // source property
    ReadOnlyObjectWrapper<MediaSource> mediaSource;

    @Override
    public MediaSource getMediaSource() {
        return mediaSource == null ? null : mediaSource.get();
    }

    void setMediaSource(MediaSource value) {
        mediaSourcePropertyImpl().set(value);
    }

    @Override
    public ReadOnlyObjectProperty<MediaSource> mediaSourceProperty() {
        return mediaSourcePropertyImpl().getReadOnlyProperty();
    }

    abstract ReadOnlyObjectWrapper<MediaSource> mediaSourcePropertyImpl();

    // status property
    private ReadOnlyObjectWrapper<Status> status;

    @Override
    public Status getStatus() {
        return (status == null) ? Status.UNKNOWN : status.get();
    }

    void setStatus(Status value) {
        statusPropertyImpl().set(value);
    }

    /**
     * The current status of this media player.
     */
    @Override
    public ReadOnlyObjectProperty<Status> statusProperty() {
        return statusPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<Status> statusPropertyImpl() {
        if (status == null) {
            status = new ReadOnlyObjectWrapper<>(this, "status", Status.UNKNOWN) {
                @Override
                protected void invalidated() {
                    log.debug("Status changed to: {}", get());
                }
            };
        }
        return status;
    }

    // current time property
    private ReadOnlyObjectWrapper<Duration> currentTime;

    @Override
    public Duration getCurrentTime() {
        return (currentTime == null) ? Duration.UNKNOWN : currentTime.get();
    }

    void setCurrentTime(Duration value) {
        currentTimePropertyImpl().set(value);
    }

    @Override
    public ReadOnlyObjectProperty<Duration> currentTimeProperty() {
        return currentTimePropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<Duration> currentTimePropertyImpl() {
        if (currentTime == null) {
            currentTime = new ReadOnlyObjectWrapper<>(this, "currentTime", Duration.UNKNOWN) {

                @Override
                protected void invalidated() {
                    log.info("Current time updated: {} s", get().toSeconds());
                }
            };
        }
        return currentTime;
    }

    // duration property
    private ReadOnlyObjectWrapper<Duration> duration;

    @Override
    public Duration getDuration() {
        return (duration == null) ? Duration.UNKNOWN : duration.get();
    }

    void setDuration(Duration value) {
        durationPropertyImpl().set(value);
    }

    @Override
    public ReadOnlyObjectProperty<Duration> durationProperty() {
        return durationPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<Duration> durationPropertyImpl() {
        if (duration == null) {
            duration = new ReadOnlyObjectWrapper<>(this, "duration", Duration.UNKNOWN) {

                @Override
                protected void invalidated() {
                    log.info("Duration updated: {} s", get().toSeconds());
                }
            };
        }
        return duration;
    }

    // On ready event handler
    private ObjectProperty<EventHandler<MediaPlayerEvent>> onReady;

    @Override
    public final EventHandler<MediaPlayerEvent> getOnReady() {
        return onReady == null ? null : onReady.get();
    }

    @Override
    public final void setOnReady(EventHandler<MediaPlayerEvent> value) {
        onReadyProperty().set(value);
    }

    @Override
    public final ObjectProperty<EventHandler<MediaPlayerEvent>> onReadyProperty() {
        if (onReady == null) {
            onReady = new SimpleObjectProperty<>(this, "onReady") {
                @Override
                protected void invalidated() {
                    eventHandlerManager.setEventHandler(MediaPlayerEvent.MEDIA_PLAYER_READY, get());
                }
            };
        }
        return onReady;
    }

    // On play event handler
    private ObjectProperty<EventHandler<MediaPlayerEvent>> onPlaying;

    @Override
    public final EventHandler<MediaPlayerEvent> getOnPlaying() {
        return (onPlaying == null) ? null : onPlaying.get();
    }

    @Override
    public final void setOnPlaying(EventHandler<MediaPlayerEvent> value) {
        onPlayingProperty().set(value);
    }

    @Override
    public final ObjectProperty<EventHandler<MediaPlayerEvent>> onPlayingProperty() {
        if (onPlaying == null) {
            onPlaying = new SimpleObjectProperty<>(this, "onPlaying"){

                @Override
                protected void invalidated() {
                    eventHandlerManager.setEventHandler(MediaPlayerEvent.MEDIA_PLAYER_PLAY, get());
                }
            };
        }
        return onPlaying;
    }

    // On pause event handler
    private ObjectProperty<EventHandler<MediaPlayerEvent>> onPause;

    @Override
    public final EventHandler<MediaPlayerEvent> getOnPause() {
        return (onPause == null) ? null : onPause.get();
    }

    @Override
    public final void setOnPause(EventHandler<MediaPlayerEvent> value) {
        onPauseProperty().set(value);
    }

    @Override
    public final ObjectProperty<EventHandler<MediaPlayerEvent>> onPauseProperty() {
        if (onPause == null) {
            onPause = new SimpleObjectProperty<>(this, "onPause") {

                @Override
                protected void invalidated() {
                    eventHandlerManager.setEventHandler(MediaPlayerEvent.MEDIA_PLAYER_PAUSE, get());
                }
            };
        }
        return onPause;
    }

    // On stalled event handler
    private ObjectProperty<EventHandler<MediaPlayerEvent>> onStalled;

    @Override
    public final EventHandler<MediaPlayerEvent> getOnStalled() {
        return (onStalled == null) ? null : onStalled.get();
    }

    @Override
    public final void setOnStalled(EventHandler<MediaPlayerEvent> value) {
        onStalledProperty().set(value);
    }

    @Override
    public final ObjectProperty<EventHandler<MediaPlayerEvent>> onStalledProperty() {
        if (onStalled == null) {
            onStalled = new SimpleObjectProperty<>(this, "onStalled") {

                @Override
                protected void invalidated() {
                    eventHandlerManager.setEventHandler(MediaPlayerEvent.MEDIA_PLAYER_STALLED, get());
                }
            };
        }
        return onStalled;
    }

    // On stopped event handler
    private ObjectProperty<EventHandler<MediaPlayerEvent>> onStopped;

    @Override
    public final EventHandler<MediaPlayerEvent> getOnStopped() {
        return (onStopped == null) ? null : onStopped.get();
    }

    @Override
    public final void setOnStopped(EventHandler<MediaPlayerEvent> value) {
        onStoppedProperty().set(value);
    }

    @Override
    public final ObjectProperty<EventHandler<MediaPlayerEvent>> onStoppedProperty() {
        if (onStopped == null) {
            onStopped = new SimpleObjectProperty<>(this, "onStopped") {

                @Override
                protected void invalidated() {
                    eventHandlerManager.setEventHandler(MediaPlayerEvent.MEDIA_PLAYER_STOP, get());
                }
            };
        }
        return onStopped;
    }

    // On end of media event handler
    private ObjectProperty<EventHandler<MediaPlayerEvent>> onEndOfMedia;

    @Override
    public final EventHandler<MediaPlayerEvent> getOnEndOfMedia() {
        return (onEndOfMedia == null) ? null : onEndOfMedia.get();
    }

    @Override
    public final void setOnEndOfMedia(EventHandler<MediaPlayerEvent> value) {
        onEndOfMediaProperty().set(value);
    }

    @Override
    public final ObjectProperty<EventHandler<MediaPlayerEvent>> onEndOfMediaProperty() {
        if (onEndOfMedia == null) {
            onEndOfMedia = new SimpleObjectProperty<>(this, "onEndOfMedia") {

                @Override
                protected void invalidated() {
                    eventHandlerManager.setEventHandler(MediaPlayerEvent.MEDIA_PLAYER_END_OF_MEDIA, get());
                }
            };
        }
        return onEndOfMedia;
    }

    // On error event handler
    private ObjectProperty<EventHandler<MediaPlayerEvent>> onError;

    @Override
    public final EventHandler<MediaPlayerEvent> getOnError() {
        return (onError == null) ? null : onError.get();
    }

    @Override
    public final void setOnError(EventHandler<MediaPlayerEvent> value) {
        onErrorProperty().set(value);
    }

    @Override
    public final ObjectProperty<EventHandler<MediaPlayerEvent>> onErrorProperty() {
        if (onError == null) {
            onError = new SimpleObjectProperty<>(this, "onError") {

                @Override
                protected void invalidated() {
                    eventHandlerManager.setEventHandler(MediaPlayerEvent.MEDIA_PLAYER_ERROR, get());
                }
            };
        }
        return onError;
    }

    // Error property
    private ReadOnlyObjectWrapper<MediaPlayerException> error;

    @Override
    public final MediaPlayerException getError() {
        return (error == null) ? null : error.get();
    }

    final void setError(MediaPlayerException error) {
        errorPropertyImpl().set(error);
    }

    @Override
    public final ReadOnlyObjectProperty<MediaPlayerException> errorProperty() {
        return errorPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<MediaPlayerException> errorPropertyImpl() {
        if (error == null) {
            error = new ReadOnlyObjectWrapper<>(this, "error") {

                @Override
                protected void invalidated() {
                    final MediaPlayerException exception = get();
                    log.error(exception.toString(), exception);
                }
            };
        }
        return error;
    }

    private final EventHandlerManager eventHandlerManager = new EventHandlerManager(this);

    @Override
    public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        return tail.prepend(eventHandlerManager);
    }
}
