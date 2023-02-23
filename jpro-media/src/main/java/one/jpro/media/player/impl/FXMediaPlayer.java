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
    private volatile boolean eom = false;

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
        });

        mediaPlayer.setOnPlaying(() -> {
            // set playing status
            setStatus(Status.PLAYING);

            // Fire playing event
            Event.fireEvent(FXMediaPlayer.this,
                    new MediaPlayerEvent(FXMediaPlayer.this,
                            MediaPlayerEvent.MEDIA_PLAYER_PLAY));
        });

        mediaPlayer.setOnPaused(() -> {
            // set paused status
            setStatus(Status.PAUSED);

            // Fire paused event
            Event.fireEvent(FXMediaPlayer.this,
                    new MediaPlayerEvent(FXMediaPlayer.this,
                            MediaPlayerEvent.MEDIA_PLAYER_PAUSE));
        });

        mediaPlayer.setOnStopped(() -> {
            // set stopped status
            setStatus(Status.STOPPED);

            // Fire stopped event
            Event.fireEvent(FXMediaPlayer.this,
                    new MediaPlayerEvent(FXMediaPlayer.this,
                            MediaPlayerEvent.MEDIA_PLAYER_STOP));
        });

        mediaPlayer.setOnEndOfMedia(() -> {
            // Set end of media flag
            eom = true;

            // While the internal media player reached the end of media,
            // it "pauses" even though no PAUSE event is fired and its
            // internal status is still PLAYING. So we need to set the
            // status to PAUSED here and fire the MEDIA_PLAYER_PAUSE event
            // to mimic the behavior of the Web media player implementation.
            setStatus(Status.PAUSED);

            // Fire pause event
            Event.fireEvent(FXMediaPlayer.this,
                    new MediaPlayerEvent(FXMediaPlayer.this,
                            MediaPlayerEvent.MEDIA_PLAYER_PAUSE));

            // Finally, fire end of media event
            Event.fireEvent(FXMediaPlayer.this,
                    new MediaPlayerEvent(FXMediaPlayer.this,
                            MediaPlayerEvent.MEDIA_PLAYER_END_OF_MEDIA));
        });

        mediaPlayer.setOnError(() -> {
            // set error status
            setStatus(Status.HALTED);
            setError(new MediaPlayerException(mediaPlayer.getError().getMessage(), mediaPlayer.getError()));
        });

        mediaPlayer.statusProperty().addListener(observable -> setStatus(mediaPlayer.getStatus()));

        mediaPlayer.currentTimeProperty().addListener(observable ->
                log.trace("Current time: {} seconds", mediaPlayer.getCurrentTime().toSeconds()));

        mediaPlayer.volumeProperty().addListener(observable ->
                log.trace("Volume: {}", mediaPlayer.getVolume()));
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
    public void play() {
        // If the end of media was reached, seek to the beginning
        // before playing, to mimic the behavior of the Web media
        // player implementation.
        if (eom) {
            // Seek to the beginning
            mediaPlayer.seek(Duration.ZERO);

            // The internal media player status is still PLAYING,
            // so we need to set the status to PLAYING and fire
            // again the MEDIA_PLAYER_PLAY event.
            if (mediaPlayer.getStatus() == Status.PLAYING) {
                // set playing status
                setStatus(Status.PLAYING);

                // Fire playing event since is not fired by the internal media player
                Event.fireEvent(FXMediaPlayer.this,
                        new MediaPlayerEvent(FXMediaPlayer.this,
                                MediaPlayerEvent.MEDIA_PLAYER_PLAY));
            }

            // Reset end of media flag
            eom = false;
        }

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
        if (seekTime == null) {
            setError(new MediaPlayerException("Seek time is null."));
        } else {
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

            // Check if seek time is greater than duration
            final Duration duration = getDuration();
            if (duration != null && seekTime.greaterThan(duration)) {
                setError(new MediaPlayerException("Seek time is greater than duration."));
                return;
            }

            mediaPlayer.seek(seekTime);

            // Check if end of media flag is set
            if (eom) {
                // If previously end of media was reached, we need to pause
                // the internal media player after the seek operation to mimic
                // the behaviour of the Web media player implementation.
                mediaPlayer.pause();

                // Reset end of media flag
                eom = false;
            }
        }
    }
}
