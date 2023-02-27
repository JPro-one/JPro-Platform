package one.jpro.media.player.impl;

import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;
import com.jpro.webapi.WebCallback;
import javafx.beans.property.*;
import javafx.event.Event;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import one.jpro.media.MediaSource;
import one.jpro.media.WebMediaEngine;
import one.jpro.media.event.MediaPlayerEvent;
import one.jpro.media.player.MediaPlayer;
import one.jpro.media.player.MediaPlayerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * {@link MediaPlayer} implementation for the web.
 *
 * @author Besmir Beqiri
 */
public final class WebMediaPlayer extends BaseMediaPlayer implements WebMediaEngine {

    private final Logger log = LoggerFactory.getLogger(WebMediaPlayer.class);

    private final WebAPI webAPI;
    private final String mediaPlayerId;
    private final JSVariable playerVideoElement;

    private boolean playerReady = false;
    private boolean startTimeChangeRequested = false;
    private boolean stopTimeChangeRequested = false;

    public WebMediaPlayer(WebAPI webAPI, MediaSource mediaSource) {
        this.webAPI = Objects.requireNonNull(webAPI, "WebAPI cannot be null.");
        mediaPlayerId = webAPI.createUniqueJSName("media_player_");
        playerVideoElement = createPlayerVideoElement("video_elem_" + mediaPlayerId);
        setMediaSource(Objects.requireNonNull(mediaSource, "Media source cannot be null."));

        // check if the media data is loaded
        handleWebEvent("loadeddata", """
                    console.log("$mediaPlayerId => ready state: " + elem.readyState);
                    java_fun(elem.readyState);
                """, readyState -> WebReadyState.fromCode(Integer.parseInt(readyState)).ifPresent(this::setReadyState));

        // handle current time change
        handleWebEvent("timeupdate", """
                console.log("$mediaPlayerId => current time: " + elem.currentTime);
                java_fun(elem.currentTime);
                """, currentTime -> {
            final Duration theCurrentTime = Duration.seconds(Double.parseDouble(currentTime));
            setCurrentTime(theCurrentTime);

            final Duration stopTime = getStopTime();
            if (stopTime != Duration.UNKNOWN && getCurrentTime().greaterThan(getStopTime())) {
                setCurrentCount(getCurrentCount() + 1);

                if ((getCurrentCount() < getCycleCount()) || (getCycleCount() == INDEFINITE)) {
                    // Fire end of media event
                    Event.fireEvent(WebMediaPlayer.this,
                            new MediaPlayerEvent(WebMediaPlayer.this,
                                    MediaPlayerEvent.MEDIA_PLAYER_END_OF_MEDIA));

                    // Loop playback
                    seek(getStartTime());

                    // Fire repeat media event
                    Event.fireEvent(WebMediaPlayer.this,
                            new MediaPlayerEvent(WebMediaPlayer.this,
                                    MediaPlayerEvent.MEDIA_PLAYER_REPEAT));
                } else {
                    // Set end of stream flag
                    isEOS = true;

                    // Pause playback to mimic the actual ended event
                    pause();
                }
            }
        });

        // handle duration change
        handleWebEvent("durationchange", """
                console.log("$mediaPlayerId => media duration: " + elem.duration + " seconds");
                java_fun(elem.duration);
                """, duration -> {
            if (duration != null && !duration.contains("null")) {
                setDuration(Duration.seconds(Double.parseDouble(duration)));
            } else {
                setDuration(Duration.UNKNOWN);
            }
        });

        // handle volume change
        handleWebEvent("volumechange", """
                console.log("$mediaPlayerId => volume change: " + elem.volume);
                java_fun(elem.volume);
                """, volume -> volumeProperty().set(Double.parseDouble(volume)));

        // handle play event
        handleWebEvent("play", """
                    console.log("$mediaPlayerId => playing...");
                    java_fun(elem.currentTime);
                """, currentTime -> {
            // Set status to playing
            setStatus(Status.PLAYING);

            // Fire play event
            Event.fireEvent(WebMediaPlayer.this,
                    new MediaPlayerEvent(WebMediaPlayer.this,
                            MediaPlayerEvent.MEDIA_PLAYER_PLAY));
        });

        // handle pause event
        handleWebEvent("pause", """
                    console.log("$mediaPlayerId => paused...");
                    java_fun(elem.paused);
                """, paused -> {
            if (Boolean.parseBoolean(paused)) {
                // Set status to paused
                setStatus(Status.PAUSED);

                // Fire pause event
                Event.fireEvent(WebMediaPlayer.this,
                        new MediaPlayerEvent(WebMediaPlayer.this,
                                MediaPlayerEvent.MEDIA_PLAYER_PAUSE));

                if (isEOS) {
                    // Fire end of media event
                    Event.fireEvent(WebMediaPlayer.this,
                            new MediaPlayerEvent(WebMediaPlayer.this,
                                    MediaPlayerEvent.MEDIA_PLAYER_END_OF_MEDIA));
                }
            }
        });

        // handle stalled event
        handleWebEvent("stalled", """
                    console.log("$mediaPlayerId => stalled...");
                    java_fun(elem.readyState);
                """, readyState -> {
            log.trace("Media player stalled: {}", readyState);

            // Set status to stalled
            setStatus(Status.STALLED);

            // Fire stalled event
            Event.fireEvent(WebMediaPlayer.this,
                    new MediaPlayerEvent(WebMediaPlayer.this,
                            MediaPlayerEvent.MEDIA_PLAYER_STALLED));
        });

        // handle ended event
        handleWebEvent("ended", """
                    console.log("$mediaPlayerId => ended...");
                    java_fun(elem.ended);
                """, ended -> {
            if (Boolean.parseBoolean(ended)) {
                // Set end of stream flag
                isEOS = true;

                // Fire end of media event
                Event.fireEvent(WebMediaPlayer.this,
                        new MediaPlayerEvent(WebMediaPlayer.this,
                                MediaPlayerEvent.MEDIA_PLAYER_END_OF_MEDIA));
            }
        });

        // handle error event
        handleWebEvent("error", """
                    console.log("$mediaPlayerId => error occurred with code: " + elem.error.code);
                    java_fun(elem.error.code);
                """, errorCode -> {
            // Set status to halted
            setStatus(Status.HALTED);

            // Set error
            WebMediaError.fromCode(Integer.parseInt(errorCode)).ifPresent(webErrorCode ->
                    setError(new MediaPlayerException(webErrorCode.getDescription())));
        });
    }

    public WebAPI getWebAPI() {
        return webAPI;
    }

    public JSVariable getVideoElement() {
        return playerVideoElement;
    }

    @Override
    ReadOnlyObjectWrapper<MediaSource> mediaSourcePropertyImpl() {
        if (mediaSource == null) {
            mediaSource = new ReadOnlyObjectWrapper<>(this, "source") {
                @Override
                protected void invalidated() {
                    if (getStatus() != Status.DISPOSED) {
                        webAPI.executeScript("""
                                %s.src = "$source";
                                """.formatted(playerVideoElement.getName())
                                .replace("$source", get().source())
                                .replace("\"\"", "\""));
                    }
                }
            };
        }
        return mediaSource;
    }

    // startTime property
    private ObjectProperty<Duration> startTime;

    @Override
    public Duration getStartTime() {
        return (startTime == null) ? Duration.ZERO : startTime.get();
    }

    @Override
    public void setStartTime(Duration value) {
        startTimeProperty().set(value);
    }

    @Override
    public ObjectProperty<Duration> startTimeProperty() {
        if (startTime == null) {
            startTime = new SimpleObjectProperty<>(this, "startTime", Duration.ZERO) {
                @Override
                protected void invalidated() {
                    if (getStatus() != Status.DISPOSED) {
                        if (playerReady) {
                            setStartStopTimes(getStartTime(), true, getStopTime(), false);
                        } else {
                            startTimeChangeRequested = true;
                        }
                        calculateCycleDuration();
                    }
                }
            };
        }
        return startTime;
    }

    // stopTime property
    private ObjectProperty<Duration> stopTime;

    @Override
    public Duration getStopTime() {
        return (stopTime == null) ? getDuration() : stopTime.get();
    }

    @Override
    public void setStopTime(Duration value) {
        stopTimeProperty().set(value);
    }

    @Override
    public ObjectProperty<Duration> stopTimeProperty() {
        if (stopTime == null) {
            stopTime = new SimpleObjectProperty<>(this, "stopTime", getDuration()) {

                @Override
                protected void invalidated() {
                    if (getStatus() != Status.DISPOSED) {
                        if (playerReady) {
                            setStartStopTimes(getStartTime(), false, stopTime.get(), true);
                        } else {
                            stopTimeChangeRequested = true;
                        }
                        calculateCycleDuration();
                    }
                }
            };
        }
        return stopTime;
    }

    @Override
    public BooleanProperty autoPlayProperty() {
        if (autoPlay == null) {
            autoPlay = new SimpleBooleanProperty(this, "autoPlay") {
                @Override
                protected void invalidated() {
                    if (getStatus() != Status.DISPOSED) {
                        webAPI.executeScript("""
                                %s.autoplay = $autoplay;
                                """.formatted(playerVideoElement.getName())
                                .replace("$autoplay", String.valueOf(get())));
                    }
                }
            };
        }
        return autoPlay;
    }

    // ready state property
    private ReadOnlyObjectWrapper<WebReadyState> readyState;

    public WebReadyState getReadyState() {
        return readyState == null ? WebReadyState.HAVE_NOTHING : readyState.get();
    }

    private void setReadyState(WebReadyState value) {
        readyStatePropertyImpl().set(value);
    }

    public ReadOnlyObjectProperty<WebReadyState> readyStateProperty() {
        return readyStatePropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<WebReadyState> readyStatePropertyImpl() {
        if (readyState == null) {
            readyState = new ReadOnlyObjectWrapper<>(this, "readyState") {

                @Override
                protected void invalidated() {
                    if (get().getCode() >= WebReadyState.HAVE_METADATA.getCode()) {
                        playerReady = true;

                        // Set state to ready
                        setStatus(Status.READY);

                        // Handle requested changes once player is ready
                        handleRequestedChanges();

                        // Fire ready event
                        Event.fireEvent(WebMediaPlayer.this,
                                new MediaPlayerEvent(WebMediaPlayer.this,
                                        MediaPlayerEvent.MEDIA_PLAYER_READY));
                    }
                    log.trace("Ready state changed: {}", get());
                }
            };
        }
        return readyState;
    }

    // volume property
    private DoubleProperty volume;

    @Override
    public double getVolume() {
        return (volume == null) ? 1.0 : volume.get();
    }

    @Override
    public void setVolume(double value) {
        value = clamp(value, 0.0, 1.0);
        if (getStatus() != Status.DISPOSED) {
            webAPI.executeScript("""
                    %s.volume = %s;
                    """.formatted(playerVideoElement.getName(), value));
        }
        volumeProperty().set(value);
    }

    @Override
    public DoubleProperty volumeProperty() {
        if (volume == null) {
            volume = new SimpleDoubleProperty(this, "volume", 1.0) {

                @Override
                protected void invalidated() {
                    log.trace("Volume changed: {}", get());
                }
            };
        }
        return volume;
    }

    // muted property
    private BooleanProperty muted;

    @Override
    public boolean isMute() {
        return muted != null && muted.get();
    }

    @Override
    public void setMute(boolean value) {
        muteProperty().set(value);
    }

    @Override
    public BooleanProperty muteProperty() {
        if (muted == null) {
            muted = new SimpleBooleanProperty(this, "muted") {
                @Override
                protected void invalidated() {
                    if (getStatus() != Status.DISPOSED) {
                        webAPI.executeScript("""
                                %s.muted = $muted;
                                """.formatted(playerVideoElement.getName())
                                .replace("$muted", String.valueOf(get())));
                    }
                }
            };
        }
        return muted;
    }

    @Override
    public void play() {
        if (playerReady && getStatus() != Status.DISPOSED) {
            if (isEOS) {
                seek(getStartTime());
            }

            webAPI.executeScript("""
                    %s.play();
                    """.formatted(playerVideoElement.getName()));
        }
    }

    @Override
    public void pause() {
        if (playerReady && getStatus() != Status.DISPOSED) {
            webAPI.executeScript("""
                    %s.pause();
                    """.formatted(playerVideoElement.getName()));
        }
    }

    @Override
    public void stop() {
        if (playerReady && getStatus() != Status.DISPOSED) {
            webAPI.executeScript("""
                    %s.pause();
                    """.formatted(playerVideoElement.getName()));

            seek(getStartTime());
            setCurrentCount(0);
        }

        setStatus(Status.STOPPED);
    }

    @Override
    public void seek(Duration seekTime) {
        // Check if parameter is null
        if (seekTime == null) {
            setError(new MediaPlayerException("Seek time is null."));
        } else if (playerReady) {
            if (getStatus() == Status.DISPOSED) {
                return;
            }
            // Check if seek time is unknown
            if (seekTime.isUnknown()) {
                setError(new MediaPlayerException("Seek time is unknown."));
                return;
            }

            // Check if seek time is negative
            if (seekTime.lessThan(Duration.ZERO)) {
                setError(new MediaPlayerException("Seek time is negative. The value will be clamp to zero."));
                seekTime = Duration.ZERO;
            }

            // Determine the seek position in seconds.
            double seekSeconds;
            if (seekTime.isIndefinite()) {
                // Determine the effective duration.
                Duration duration = getDuration();
                if (duration == null || duration.isUnknown() || duration.isIndefinite()) {
                    duration = Duration.millis(Double.MAX_VALUE);
                }

                // Convert duration to seconds.
                seekSeconds = duration.toMillis() / 1000.0;
            } else {
                // Convert the parameter to seconds.
                seekSeconds = seekTime.toMillis() / 1000.0;

                // Clamp the seconds if needed.
                double[] startStop = calculateStartStopTimes(getStartTime(), getStopTime());
                if (seekSeconds < startStop[0]) {
                    seekSeconds = startStop[0];
                } else if (seekSeconds > startStop[1]) {
                    seekSeconds = startStop[1];
                }
            }

            if ((getStatus() == Status.PLAYING || getStatus() == Status.PAUSED)
                    && getStartTime().toSeconds() <= seekSeconds
                    && seekSeconds <= getStopTime().toSeconds()) {
                isEOS = false;
            }

            webAPI.executeScript("""
                    %s.currentTime = %s;
                    """.formatted(playerVideoElement.getName(), seekSeconds));
        }
    }

    private void handleWebEvent(String eventName, String eventHandler, WebCallback webCallback) {
        webAPI.registerJavaFunction(mediaPlayerId + "_" + eventName, webCallback);
        webAPI.executeScript("""
                let elem = $playerVideoElem;
                elem.on$eventName = (event) => {
                     $eventHandler
                };
                """
                .replace("$playerVideoElem", playerVideoElement.getName())
                .replace("$eventHandler", eventHandler)
                .replace("java_fun", "jpro.$mediaPlayerId_$eventName")
                .replace("$mediaPlayerId", mediaPlayerId)
                .replace("$eventName", eventName));
    }

    /**
     * Simple utility function which clamps the given value to be strictly
     * between the min and max values.
     */
    private double clamp(double min, double value, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    private JSVariable createPlayerVideoElement(String videoElement) {
        webAPI.executeScript("""
                $playerVideoElem = document.createElement("video");
                $playerVideoElem.controls = false;
                $playerVideoElem.muted = false;
                $playerVideoElem.setAttribute("webkit-playsinline", 'webkit-playsinline');
                $playerVideoElem.setAttribute("playsinline", 'playsinline');
                """.replace("$playerVideoElem", videoElement));
        return new JSVariable(webAPI, videoElement);
    }

    /**
     * Set the effective start and stop times on the underlying player,
     * clamping as needed.
     *
     * @param startValue      the new start time
     * @param isStartValueSet if the start time should be set
     * @param stopValue       the new stop time
     * @param isStopValueSet  if the stop time should be set
     */
    private void setStartStopTimes(Duration startValue, boolean isStartValueSet, Duration stopValue, boolean isStopValueSet) {
        if (getDuration().isIndefinite()) {
            return;
        }

        // Clamp the start and stop times to values in seconds.
        double[] startStopTimes = calculateStartStopTimes(startValue, stopValue);
        if (isStartValueSet) {
            final Duration startTime = Duration.seconds(startStopTimes[0]);
            if (getStatus() == Status.READY || getStatus() == Status.PAUSED) {
                if (startTime.greaterThan(getCurrentTime())) {
                    setCurrentTime(startTime);
                    seek(startTime);
                }
            }
        }
        if (isStopValueSet) {
            final Duration stopTime = Duration.seconds(startStopTimes[1]);
            if (getStatus() == Status.READY || getStatus() == Status.PAUSED) {
                if (stopTime.lessThan(getCurrentTime())) {
                    setCurrentTime(stopTime);
                    seek(stopTime);
                }
            }
        }
    }

    /**
     * Clamp the start and stop times. The parameters are clamped
     * to the range <code>[0.0,&nbsp;duration]</code>. If the duration
     * is unknown, {@link Double#MAX_VALUE} is used instead. Furthermore,
     * if the separately clamped values satisfy <code>startTime&nbsp;&gt;&nbsp;stopTime</code>
     * then <code>stopTime</code> is clamped as <code>stopTime&nbsp;&ge;&nbsp;startTime</code>.
     *
     * @param startValue the new start time
     * @param stopValue  the new stop time
     * @return the clamped times in seconds as <code>{actualStart,&nbsp;actualStop}</code>.
     */
    private double[] calculateStartStopTimes(Duration startValue, Duration stopValue) {
        // Derive start time in seconds.
        double newStart;
        if (startValue == null || startValue.lessThan(Duration.ZERO)
                || startValue.equals(Duration.UNKNOWN)) {
            newStart = 0.0;
        } else if (startValue.equals(Duration.INDEFINITE)) {
            newStart = Double.MAX_VALUE;
        } else {
            newStart = startValue.toMillis() / 1000.0;
        }

        // Derive stop time in seconds.
        double newStop;
        if (stopValue == null || stopValue.equals(Duration.UNKNOWN)
                || stopValue.equals(Duration.INDEFINITE)) {
            newStop = Double.MAX_VALUE;
        } else if (stopValue.lessThan(Duration.ZERO)) {
            newStop = 0.0;
        } else {
            newStop = stopValue.toMillis() / 1000.0;
        }

        // Derive the duration in seconds.
        final Duration mediaDuration = getDuration();
        double duration = mediaDuration == Duration.UNKNOWN ?
                Double.MAX_VALUE : mediaDuration.toMillis() / 1000.0;

        // Clamp the start and stop times to [0,duration].
        double actualStart = clamp(newStart, 0.0, duration);
        double actualStop = clamp(newStop, 0.0, duration);

        // Restrict actual stop time to [startTime,duration].
        if (actualStart > actualStop) {
            actualStop = actualStart;
        }

        return new double[]{actualStart, actualStop};
    }

    private void calculateCycleDuration() {
        Duration endTime;
        Duration mediaDuration = getDuration();

        if (!getStopTime().isUnknown()) {
            endTime = getStopTime();
        } else {
            endTime = mediaDuration;
        }
        if (endTime.greaterThan(mediaDuration)) {
            endTime = mediaDuration;
        }

        // filter bad values
        if (endTime.isUnknown() || getStartTime().isUnknown() || getStartTime().isIndefinite()) {
            if (!getCycleDuration().isUnknown())
                setCycleDuration(Duration.UNKNOWN);
        }

        setCycleDuration(endTime.subtract(getStartTime()));
        calculateTotalDuration(); // since it's dependent on cycle duration
    }

    private void calculateTotalDuration() {
        if (getCycleCount() == INDEFINITE) {
            setTotalDuration(Duration.INDEFINITE);
        } else if (getCycleDuration().isUnknown()) {
            setTotalDuration(Duration.UNKNOWN);
        } else {
            setTotalDuration(getCycleDuration().multiply(getCycleCount()));
        }
    }

    private void handleRequestedChanges() {
        if (startTimeChangeRequested || stopTimeChangeRequested) {
            setStartStopTimes(getStartTime(), startTimeChangeRequested, getStopTime(), stopTimeChangeRequested);
            startTimeChangeRequested = stopTimeChangeRequested = false;
        }
    }
}
