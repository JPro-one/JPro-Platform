package one.jpro.media.player.impl;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.Event;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import one.jpro.media.event.MediaPlayerEvent;
import one.jpro.media.player.MediaPlayerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link MediaPlayer} implementation for the desktop/mobile.
 *
 * @author Besmir Beqiri
 */
public final class FXMediaPlayer extends BaseMediaPlayer {

    private final Logger log = LoggerFactory.getLogger(FXMediaPlayer.class);

    private final MediaPlayer mediaPlayer;

    public FXMediaPlayer(String source) {
        setSource(source);
        final Media media = new Media(source);
        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.setOnReady(() -> {
            // set ready status
            setStatus(Status.READY);

            // Fire ready event
            Event.fireEvent(FXMediaPlayer.this,
                    new MediaPlayerEvent(FXMediaPlayer.this,
                            MediaPlayerEvent.MEDIA_PLAYER_READY));

            log.info("Media player ready!");
        });

        mediaPlayer.setOnPlaying(() -> {
            // set playing status
            setStatus(Status.PLAYING);

            // Fire playing event
            Event.fireEvent(FXMediaPlayer.this,
                    new MediaPlayerEvent(FXMediaPlayer.this,
                            MediaPlayerEvent.MEDIA_PLAYER_PLAY));

            log.info("Media player playing!");
        });

        mediaPlayer.setOnPaused(() -> {
            // set paused status
            setStatus(Status.PAUSED);

            // Fire paused event
            Event.fireEvent(FXMediaPlayer.this,
                    new MediaPlayerEvent(FXMediaPlayer.this,
                            MediaPlayerEvent.MEDIA_PLAYER_PAUSE));

            log.info("Media player paused!");
        });

        mediaPlayer.setOnStopped(() -> {
            // set stopped status
            setStatus(Status.STOPPED);

            // Fire stopped event
            Event.fireEvent(FXMediaPlayer.this,
                    new MediaPlayerEvent(FXMediaPlayer.this,
                            MediaPlayerEvent.MEDIA_PLAYER_STOP));

            log.info("Media player stopped!");
        });

        mediaPlayer.setOnError(() -> {
            // set error status
            setStatus(Status.HALTED);
            setError(new MediaPlayerException(mediaPlayer.getError().getMessage()));

            // Fire error event
            Event.fireEvent(FXMediaPlayer.this,
                    new MediaPlayerEvent(FXMediaPlayer.this,
                            MediaPlayerEvent.MEDIA_PLAYER_ERROR));

            log.error("Media player error: {}", mediaPlayer.getError(), mediaPlayer.getError());
        });

        mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
            log.debug("Current time: {}", newValue);
        });
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
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
            source = new ReadOnlyStringWrapper(this, "source");
        }
        return source;
    }

    // current time property
    public Duration getCurrentTime() {
        return mediaPlayer.getCurrentTime();
    }

    public ReadOnlyObjectProperty<Duration> currentTimeProperty() {
        return mediaPlayer.currentTimeProperty();
    }

    // duration property
    public Duration getDuration() {
        return mediaPlayer.getTotalDuration();
    }

    public ReadOnlyObjectProperty<Duration> durationProperty() {
        return mediaPlayer.totalDurationProperty();
    }

    @Override
    public double getVolume() {
        return mediaPlayer.getVolume();
    }

    @Override
    public void setVolume(double value) {
        mediaPlayer.setVolume(value);
    }

    @Override
    public DoubleProperty volumeProperty() {
        return mediaPlayer.volumeProperty();
    }

    @Override
    public Status getStatus() {
        return mediaPlayer.getStatus();
    }
    @Override
    public ReadOnlyObjectProperty<Status> statusProperty() {
        return mediaPlayer.statusProperty();
    }

    @Override
    public void play() {
        mediaPlayer.play();
    }

    @Override
    public void pause() {
        mediaPlayer.pause();
    }

    @Override
    public void stop() {
        mediaPlayer.stop();
    }

    @Override
    public void seek(Duration seekTime) {
        mediaPlayer.seek(seekTime);
    }
}
