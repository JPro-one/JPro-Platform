package one.jpro.media.player.impl;

import com.jpro.webapi.WebAPI;
import com.jpro.webapi.WebCallback;
import javafx.beans.property.*;
import javafx.event.Event;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
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
public final class WebMediaPlayer extends BaseMediaPlayer {

    private final Logger log = LoggerFactory.getLogger(WebMediaPlayer.class);

    private final WebAPI webAPI;
    private final String mediaPlayerId;

    public WebMediaPlayer(WebAPI webAPI, String source) {
        this.webAPI = Objects.requireNonNull(webAPI, "WebAPI cannot be null.");
        mediaPlayerId = webAPI.createUniqueJSName("media_player_");
        setSource(Objects.requireNonNull(source, "Media source cannot be null."));

        // check if the media can be played
        handleWebEvent("canplay", """
                    console.log("$mediaPlayerId => ready state: " + elem.readyState);
                    java_fun(elem.readyState);
                """, str -> WebReadyState.fromCode(Integer.parseInt(str)).ifPresent(this::setReadyState));

        // handle current time change
        handleWebEvent("timeupdate", """
                console.log("$mediaPlayerId => current time: " + elem.currentTime);
                java_fun(elem.currentTime);
                """, str -> setCurrentTime(Duration.seconds(Double.parseDouble(str))));

        // handle duration change
        handleWebEvent("durationchange", """
                console.log("$mediaPlayerId => media duration: " + elem.duration + " seconds");
                java_fun(elem.duration);
                """, str -> setDuration(Duration.seconds(Double.parseDouble(str))));

        // handle volume change
        handleWebEvent("volumechange", """
                console.log("$mediaPlayerId => volume change: " + elem.volume);
                java_fun(elem.volume);
                """, str -> volumeProperty().set(Double.parseDouble(str)));

        // handle play event
        handleWebEvent("play", """
                    console.log("$mediaPlayerId => playing...");
                    java_fun(elem.currentTime);
                """, str -> {
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
                """, str -> {
            if (Boolean.parseBoolean(str)) {
                // Set status to paused
                setStatus(Status.PAUSED);

                // Fire pause event
                Event.fireEvent(WebMediaPlayer.this,
                        new MediaPlayerEvent(WebMediaPlayer.this,
                                MediaPlayerEvent.MEDIA_PLAYER_PAUSE));
            }
        });

        // handle stalled event
        handleWebEvent("stalled", """
                    console.log("$mediaPlayerId => stalled...");
                    java_fun(elem.readyState);
                """, str -> {
            // Set status to stalled
            setStatus(Status.STALLED);

            // Fire stalled event
            Event.fireEvent(WebMediaPlayer.this,
                    new MediaPlayerEvent(WebMediaPlayer.this,
                            MediaPlayerEvent.MEDIA_PLAYER_STALLED));
        });

        // handle error event
        handleWebEvent("error", """
                    console.log("$mediaPlayerId => error occurred with code: " + elem.error.code);
                    java_fun(elem.error.code);
                """, str -> {
            // Set error
            WebMediaError.fromCode(Integer.parseInt(str)).ifPresent(webErrorCode ->
                    setError(new MediaPlayerException(webErrorCode.getDescription())));

            // Set status to halted
            setStatus(Status.HALTED);

            // Fire error event
            Event.fireEvent(WebMediaPlayer.this,
                    new MediaPlayerEvent(WebMediaPlayer.this,
                            MediaPlayerEvent.MEDIA_PLAYER_ERROR));
        });
    }

    public WebAPI getWebAPI() {
        return webAPI;
    }

    public String getMediaPlayerId() {
        return mediaPlayerId;
    }

    // source property
    private ReadOnlyStringWrapper source;

    @Override
    public String getSource() {
        return source == null ? null : source.get();
    }

    private void setSource(String value) {
        sourcePropertyImpl().set(value);
    }

    @Override
    public ReadOnlyStringProperty sourceProperty() {
        return sourcePropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyStringWrapper sourcePropertyImpl() {
        if (source == null) {
            source = new ReadOnlyStringWrapper(this, "source") {
                @Override
                protected void invalidated() {
                    webAPI.executeScript("""
                            var elem = document.getElementById("$mediaPlayerId");
                            elem.src = "$source";
                            """.replace("$mediaPlayerId", mediaPlayerId)
                            .replace("$source", get()));
                }
            };
        }
        return source;
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
                    if (get() == WebReadyState.HAVE_ENOUGH_DATA) {
                        // Set state to ready
                        setStatus(Status.READY);

                        // Fire ready event
                        Event.fireEvent(WebMediaPlayer.this,
                                new MediaPlayerEvent(WebMediaPlayer.this,
                                        MediaPlayerEvent.MEDIA_PLAYER_READY));
                    }
                    log.info("Ready state changed: {}", get());
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
        value = clamp(volume.get(), 0.0, 1.0);
        if (getStatus() != Status.DISPOSED) {
            webAPI.executeScript("""
                    var elem = document.getElementById("$mediaPlayerId");
                    elem.volume = %s;
                    """.replace("$mediaPlayerId", mediaPlayerId)
                    .formatted(value));
        }
        volumeProperty().set(value);
    }

    @Override
    public DoubleProperty volumeProperty() {
        if (volume == null) {
            volume = new SimpleDoubleProperty(this, "volume", 1.0) {

                @Override
                protected void invalidated() {
                    log.info("Volume changed: {}", get());
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
                    webAPI.executeScript("""
                            var elem = document.getElementById("$mediaPlayerId");
                            elem.muted = $muted;
                            """.replace("$mediaPlayerId", mediaPlayerId)
                            .replace("$muted", String.valueOf(get())));
                }
            };
        }
        return muted;
    }

    @Override
    public void play() {
        if (getStatus() != Status.DISPOSED) {
            webAPI.executeScript("""
                let elem = document.getElementById("$mediaPlayerId");
                elem.play();
                """.replace("$mediaPlayerId", mediaPlayerId));
        }
    }

    @Override
    public void pause() {
        if (getStatus() != Status.DISPOSED) {
            webAPI.executeScript("""
                let elem = document.getElementById("$mediaPlayerId");
                elem.pause();
                """.replace("$mediaPlayerId", mediaPlayerId));
        }
    }

    @Override
    public void stop() {
        if (getStatus() != Status.DISPOSED) {
            webAPI.executeScript("""
                let elem = document.getElementById("$mediaPlayerId");
                elem.pause();
                elem.currentTime = 0;
                """.replace("$mediaPlayerId", mediaPlayerId));
        }

        setStatus(Status.STOPPED);
    }

    @Override
    public void seek(Duration seekTime) {
        webAPI.executeScript("""
                let elem = document.getElementById("$mediaPlayerId");
                elem.currentTime=%s;
                """.formatted(seekTime.toSeconds())
                .replace("$mediaPlayerId", mediaPlayerId));
    }

    private void handleWebEvent(String eventName, String eventHandler, WebCallback webCallback) {
        webAPI.registerJavaFunction(mediaPlayerId + "_" + eventName, webCallback);
        webAPI.executeScript("""
                let elem = document.getElementById("$mediaPlayerId");
                elem.on$eventName = (event) => {
                     $eventHandler
                };
                """.replace("$eventHandler", eventHandler)
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
}
