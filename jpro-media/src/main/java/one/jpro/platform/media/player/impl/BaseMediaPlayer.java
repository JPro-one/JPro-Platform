package one.jpro.platform.media.player.impl;

import com.sun.javafx.event.EventHandlerManager;
import javafx.beans.property.*;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import one.jpro.platform.media.MediaSource;
import one.jpro.platform.media.event.MediaPlayerEvent;
import one.jpro.platform.media.player.MediaPlayer;
import one.jpro.platform.media.player.MediaPlayerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link MediaPlayer} implementations.
 *
 * @author Besmir Beqiri
 */
abstract class BaseMediaPlayer implements MediaPlayer {

    private final Logger log = LoggerFactory.getLogger(BaseMediaPlayer.class);
    boolean isEOS = false;

    // source property
    ReadOnlyObjectWrapper<MediaSource> mediaSource;

    @Override
    public final MediaSource getMediaSource() {
        return mediaSource == null ? null : mediaSource.get();
    }

    final void setMediaSource(MediaSource value) {
        mediaSourcePropertyImpl().set(value);
    }

    @Override
    public final ReadOnlyObjectProperty<MediaSource> mediaSourceProperty() {
        return mediaSourcePropertyImpl().getReadOnlyProperty();
    }

    abstract ReadOnlyObjectWrapper<MediaSource> mediaSourcePropertyImpl();

    // autoplay property
    BooleanProperty autoPlay;

    @Override
    public void setAutoPlay(boolean value) {
        autoPlayProperty().set(value);
    }

    @Override
    public boolean isAutoPlay() {
        return autoPlay != null && autoPlay.get();
    }

    @Override
    public abstract BooleanProperty autoPlayProperty();

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
                    log.trace("Status changed to: {}", get());
                }
            };
        }
        return status;
    }

    // current time property
    private ReadOnlyObjectWrapper<Duration> currentTime;

    @Override
    public Duration getCurrentTime() {
        if (getStatus() == Status.DISPOSED) {
            return Duration.ZERO;
        }

        if (getStatus() == Status.STOPPED) {
            return Duration.millis(getStartTime().toMillis());
        }

        if (isEOS) {
            final Duration duration = getDuration();
            final Duration stopTime = getStopTime();
            if (stopTime != Duration.UNKNOWN && duration != Duration.UNKNOWN) {
                if (stopTime.greaterThan(duration)) {
                    return Duration.millis(duration.toMillis());
                } else {
                    return Duration.millis(stopTime.toMillis());
                }
            }
        }

        return (currentTime == null) ? Duration.ZERO : currentTime.get();
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
            currentTime = new ReadOnlyObjectWrapper<>(this, "currentTime", Duration.ZERO) {

                @Override
                protected void invalidated() {
                    log.trace("Current time updated: {} s", get().toSeconds());
                }
            };
        }
        return currentTime;
    }

    // cycle count property
    private IntegerProperty cycleCount;

    @Override
    public int getCycleCount() {
        return cycleCount == null ? 1 : cycleCount.get();
    }

    @Override
    public void setCycleCount(int value) {
        cycleCountProperty().set(value);
    }

    @Override
    public IntegerProperty cycleCountProperty() {
        if (cycleCount == null) {
            cycleCount = new SimpleIntegerProperty(this, "cycleCount", 1);
        }
        return cycleCount;
    }

    // current count property
    private ReadOnlyIntegerWrapper currentCount;

    @Override
    public final int getCurrentCount() {
        return currentCount == null ? 0 : currentCount.get();
    }

    void setCurrentCount(int value) {
        currentCountPropertyImpl().set(value);
    }

    @Override
    public ReadOnlyIntegerProperty currentCountProperty() {
        return currentCountPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyIntegerWrapper currentCountPropertyImpl() {
        if (currentCount == null) {
            currentCount = new ReadOnlyIntegerWrapper(this, "currentCount");
        }
        return currentCount;
    }

    // cycleDuration property
    private ReadOnlyObjectWrapper<Duration> cycleDuration;

    @Override
    public Duration getCycleDuration() {
        return cycleDuration == null ? Duration.UNKNOWN : cycleDuration.get();
    }

    void setCycleDuration(Duration value) {
        cycleDurationPropertyImpl().set(value);
    }

    @Override
    public ReadOnlyObjectProperty<Duration> cycleDurationProperty() {
        return cycleDurationPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<Duration> cycleDurationPropertyImpl() {
        if (cycleDuration == null) {
            cycleDuration = new ReadOnlyObjectWrapper<>(this, "cycleDuration");
        }
        return cycleDuration;
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
                    log.trace("Media duration: {} s", get().toSeconds());
                }
            };
        }
        return duration;
    }

    // total duration property
    private ReadOnlyObjectWrapper<Duration> totalDuration;

    @Override
    public Duration getTotalDuration() {
        return totalDuration == null ? Duration.UNKNOWN : totalDuration.get();
    }

    void setTotalDuration(Duration value) {
        totalDurationPropertyImpl().set(value);
    }

    @Override
    public ReadOnlyObjectProperty<Duration> totalDurationProperty() {
        return totalDurationPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<Duration> totalDurationPropertyImpl() {
        if (totalDuration == null) {
            totalDuration = new ReadOnlyObjectWrapper<>(this, "totalDuration");
        }
        return totalDuration;
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
            onPlaying = new SimpleObjectProperty<>(this, "onPlaying") {

                @Override
                protected void invalidated() {
                    eventHandlerManager.setEventHandler(MediaPlayerEvent.MEDIA_PLAYER_PLAY, get());
                }
            };
        }
        return onPlaying;
    }

    // On pause event handler
    private ObjectProperty<EventHandler<MediaPlayerEvent>> onPaused;

    @Override
    public final EventHandler<MediaPlayerEvent> getOnPaused() {
        return (onPaused == null) ? null : onPaused.get();
    }

    @Override
    public final void setOnPaused(EventHandler<MediaPlayerEvent> value) {
        onPausedProperty().set(value);
    }

    @Override
    public final ObjectProperty<EventHandler<MediaPlayerEvent>> onPausedProperty() {
        if (onPaused == null) {
            onPaused = new SimpleObjectProperty<>(this, "onPaused") {

                @Override
                protected void invalidated() {
                    eventHandlerManager.setEventHandler(MediaPlayerEvent.MEDIA_PLAYER_PAUSE, get());
                }
            };
        }
        return onPaused;
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

    // On repeat event handler
    private ObjectProperty<EventHandler<MediaPlayerEvent>> onRepeat;

    @Override
    public final EventHandler<MediaPlayerEvent> getOnRepeat() {
        return onRepeat == null ? null : onRepeat.get();
    }

    @Override
    public final void setOnRepeat(EventHandler<MediaPlayerEvent> value) {
        onRepeatProperty().set(value);
    }

    @Override
    public ObjectProperty<EventHandler<MediaPlayerEvent>> onRepeatProperty() {
        if (onRepeat == null) {
            onRepeat = new SimpleObjectProperty<>(this, "onRepeat") {

                @Override
                protected void invalidated() {
                    eventHandlerManager.setEventHandler(MediaPlayerEvent.MEDIA_PLAYER_REPEAT, get());
                }
            };
        }
        return onRepeat;
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
                    if (exception != null) {
                        // Fire error event
                        Event.fireEvent(BaseMediaPlayer.this,
                                new MediaPlayerEvent(BaseMediaPlayer.this,
                                        MediaPlayerEvent.MEDIA_PLAYER_ERROR));
                    }
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
