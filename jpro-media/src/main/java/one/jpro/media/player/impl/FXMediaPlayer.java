package one.jpro.media.player.impl;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.Event;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import one.jpro.media.MediaSource;
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

    public FXMediaPlayer(MediaSource mediaSource) {
        setMediaSource(mediaSource);
        final Media media = new Media(mediaSource.source());
        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.setOnReady(() -> {
            // set ready status
            setStatus(Status.READY);

            // Fire ready event
            Event.fireEvent(FXMediaPlayer.this,
                    new MediaPlayerEvent(FXMediaPlayer.this,
                            MediaPlayerEvent.MEDIA_PLAYER_READY));

            log.debug("Media player ready!");
        });

        mediaPlayer.setOnPlaying(() -> {
            // set playing status
            setStatus(Status.PLAYING);

            // Fire playing event
            Event.fireEvent(FXMediaPlayer.this,
                    new MediaPlayerEvent(FXMediaPlayer.this,
                            MediaPlayerEvent.MEDIA_PLAYER_PLAY));

            log.debug("Media player playing!");
        });

        mediaPlayer.setOnPaused(() -> {
            // set paused status
            setStatus(Status.PAUSED);

            // Fire paused event
            Event.fireEvent(FXMediaPlayer.this,
                    new MediaPlayerEvent(FXMediaPlayer.this,
                            MediaPlayerEvent.MEDIA_PLAYER_PAUSE));

            log.debug("Media player paused!");
        });

        mediaPlayer.setOnStopped(() -> {
            // set stopped status
            setStatus(Status.STOPPED);

            // Fire stopped event
            Event.fireEvent(FXMediaPlayer.this,
                    new MediaPlayerEvent(FXMediaPlayer.this,
                            MediaPlayerEvent.MEDIA_PLAYER_STOP));

            log.debug("Media player stopped!");
        });

        mediaPlayer.setOnEndOfMedia(() -> {
            mediaPlayer.seek(Duration.ZERO);
            mediaPlayer.stop();

            // Fire end of media event
            Event.fireEvent(FXMediaPlayer.this,
                    new MediaPlayerEvent(FXMediaPlayer.this,
                            MediaPlayerEvent.MEDIA_PLAYER_END_OF_MEDIA));

            log.debug("Media playback has ended!");
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

        mediaPlayer.currentTimeProperty().addListener(observable ->
                log.debug("Current time: {}", mediaPlayer.getCurrentTime()));

        mediaPlayer.volumeProperty().addListener(observable ->
                log.debug("Volume: {}", mediaPlayer.getVolume()));
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    ReadOnlyObjectWrapper<MediaSource> mediaSourcePropertyImpl() {
        if (mediaSource == null) {
            mediaSource = new ReadOnlyObjectWrapper<>(this, "source");
        }
        return mediaSource;
    }

    @Override
    public boolean isAutoPlay() {
        return mediaPlayer.isAutoPlay();
    }

    @Override
    public void setAutoPlay(boolean autoPlay) {
        mediaPlayer.setAutoPlay(autoPlay);
    }

    @Override
    public BooleanProperty autoPlayProperty() {
        return mediaPlayer.autoPlayProperty();
    }

    @Override
    public Duration getCurrentTime() {
        return mediaPlayer.getCurrentTime();
    }

    @Override
    public ReadOnlyObjectProperty<Duration> currentTimeProperty() {
        return mediaPlayer.currentTimeProperty();
    }

    @Override
    public Duration getDuration() {
        return mediaPlayer.getTotalDuration();
    }

    @Override
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
    public boolean isMute() {
        return mediaPlayer.isMute();
    }

    @Override
    public void setMute(boolean value) {
        mediaPlayer.setMute(value);
    }

    @Override
    public BooleanProperty muteProperty() {
        return mediaPlayer.muteProperty();
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
