package one.jpro.media.player;

import javafx.beans.property.*;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import one.jpro.media.player.impl.FXMediaPlayer;
import one.jpro.media.player.impl.FXMediaView;
import one.jpro.media.player.impl.WebMediaPlayer;
import one.jpro.media.player.impl.WebMediaView;

/**
 * A {@link Node} that provides a view of Media being played by a {@link MediaPlayer}.
 *
 * @author Besmir Beqiri
 */
public abstract class MediaView extends Region {

    public static MediaView create(MediaPlayer mediaPlayer) {
        if (mediaPlayer instanceof FXMediaPlayer fxMediaPlayer) {
            return new FXMediaView(fxMediaPlayer);
        } else if (mediaPlayer instanceof WebMediaPlayer webMediaPlayer) {
            return new WebMediaView(webMediaPlayer);
        } else {
            throw new IllegalArgumentException("Unsupported MediaPlayer implementation: " + mediaPlayer.getClass().getName());
        }
    }

    // media player property
    protected ObjectProperty<MediaPlayer> mediaPlayer;

    public final MediaPlayer getMediaPlayer() {
        return mediaPlayer == null ? null : mediaPlayer.get();
    }

    public final void setMediaPlayer(MediaPlayer value) {
        mediaPlayerProperty().set(value);
    }

    public abstract ObjectProperty<MediaPlayer> mediaPlayerProperty();

    // preserve ratio property
    protected BooleanProperty preserveRatio;

    public final boolean isPreserveRatio() {
        return preserveRatio != null && preserveRatio.get();
    }

    public final void setPreserveRatio(boolean value) {
        preserveRatioProperty().set(value);
    }

    public abstract BooleanProperty preserveRatioProperty();

    // fitWidth property
    protected DoubleProperty fitWidth;

    public final double getFitWidth() {
        return fitWidth == null ? 0.0 : fitWidth.get();
    }

    public final void setFitWidth(double value) {
        fitWidthProperty().set(value);
    }

    public abstract DoubleProperty fitWidthProperty();

    // fitHeight property
    protected DoubleProperty fitHeight;

    public final double getFitHeight() {
        return fitHeight == null ? 0.0 : fitHeight.get();
    }

    public final void setFitHeight(double value) {
        fitHeightProperty().set(value);
    }

    public abstract DoubleProperty fitHeightProperty();
}
