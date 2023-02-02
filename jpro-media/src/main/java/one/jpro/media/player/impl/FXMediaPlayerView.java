package one.jpro.media.player.impl;

import javafx.beans.property.*;
import one.jpro.media.MediaEngine;
import one.jpro.media.MediaView;
import one.jpro.media.player.MediaPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link MediaView} implementation for a desktop/mobile {@link MediaPlayer}.
 *
 * @author Besmir Beqiri
 */
public class FXMediaPlayerView extends MediaView {

    private final Logger log = LoggerFactory.getLogger(FXMediaPlayerView.class);

    private javafx.scene.media.MediaView fxMediaView;

    public FXMediaPlayerView() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
    }

    public FXMediaPlayerView(FXMediaPlayer mediaPlayer) {
        this();
        setMediaEngine(mediaPlayer);
    }

    @Override
    public ObjectProperty<MediaEngine> mediaEngineProperty() {
        if (mediaEngine == null) {
            mediaEngine = new SimpleObjectProperty<>(this, "mediaEngine") {

                @Override
                protected void invalidated() {
                    final MediaEngine mediaPlayer = getMediaEngine();
                    if (mediaPlayer instanceof FXMediaPlayer fxMediaPlayer) {
                        fxMediaView = new javafx.scene.media.MediaView(fxMediaPlayer.getMediaPlayer());
                        fxMediaView.setPreserveRatio(isPreserveRatio());
                        fxMediaView.setFitWidth(getFitWidth());
                        fxMediaView.setFitHeight(getFitHeight());
                        getChildren().setAll(fxMediaView);
                    }
                }
            };
        }
        return mediaEngine;
    }

    @Override
    public final DoubleProperty fitWidthProperty() {
        if (fitWidth == null) {
            fitWidth = new SimpleDoubleProperty(this, "fitWidth") {

                @Override
                protected void invalidated() {
                    if (fxMediaView != null) {
                        fxMediaView.setFitWidth(get());
                    }
                    log.trace("video width: " + getFitWidth());
                }
            };
        }
        return fitWidth;
    }

    @Override
    public DoubleProperty fitHeightProperty() {
        if (fitHeight == null) {
            fitHeight = new SimpleDoubleProperty(this, "fitHeight") {

                @Override
                protected void invalidated() {
                    if (fxMediaView != null) {
                        fxMediaView.setFitHeight(get());
                    }
                    log.trace("video height: " + getFitHeight());
                }
            };
        }
        return fitHeight;
    }

    @Override
    public final BooleanProperty preserveRatioProperty() {
        if (preserveRatio == null) {
            preserveRatio = new SimpleBooleanProperty(this, "preserveRatio", true) {

                @Override
                protected void invalidated() {
                    if (fxMediaView != null) {
                        fxMediaView.setPreserveRatio(get());
                    }
                }
            };
        }
        return preserveRatio;
    }
}
