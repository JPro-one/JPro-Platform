package one.jpro.media.player.impl;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
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
                    sceneProperty().removeListener(weakUpdateViewContainerListener);
                    updateViewContainer();
                    sceneProperty().addListener(weakUpdateViewContainerListener);
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
                    setInternalFitWidth(get());
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
                    setInternalFitHeight(get());
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

    private void setInternalFitWidth(double fitWidth) {
        if (fxMediaView != null) {
            if (fitWidth == 0) {
                getChildren().remove(fxMediaView);
            } else if (!getChildren().contains(fxMediaView)) {
                getChildren().add(fxMediaView);
            }
            fxMediaView.setFitWidth(fitWidth);
            log.trace("video width: {}", fitWidth);
        }
    }

    private void setInternalFitHeight(double fitHeight) {
        if (fxMediaView != null) {
            if (fitHeight == 0) {
                getChildren().remove(fxMediaView);
            } else if (!getChildren().contains(fxMediaView)) {
                getChildren().add(fxMediaView);
            }
            fxMediaView.setFitHeight(fitHeight);
            log.trace("video height: " + fitHeight);
        }
    }

    private final InvalidationListener updateViewContainerListener = observable -> updateViewContainer();
    private final WeakInvalidationListener weakUpdateViewContainerListener =
            new WeakInvalidationListener(updateViewContainerListener);

    private void updateViewContainer() {
        if (getScene() != null && getMediaEngine() instanceof FXMediaPlayer fxMediaPlayer) {
            fxMediaView = new javafx.scene.media.MediaView(fxMediaPlayer.getMediaPlayer());
            fxMediaView.setPreserveRatio(isPreserveRatio());
            setInternalFitWidth(getFitWidth());
            setInternalFitHeight(getFitHeight());
        }
    }
}
