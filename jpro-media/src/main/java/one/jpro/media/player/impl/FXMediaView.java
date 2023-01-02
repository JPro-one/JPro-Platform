package one.jpro.media.player.impl;

import javafx.beans.property.*;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import one.jpro.media.player.MediaPlayer;
import one.jpro.media.player.MediaView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link MediaView} implementation for desktop/mobile.
 *
 * @author Besmir Beqiri
 */
public class FXMediaView extends MediaView {

    private final Logger log = LoggerFactory.getLogger(FXMediaView.class);

    private static final String DEFAULT_STYLE_CLASS = "media-view";

    private javafx.scene.media.MediaView fxMediaView;

    public FXMediaView() {
        initialize();
    }

    public FXMediaView(FXMediaPlayer mediaPlayer) {
        this();
        setMediaPlayer(mediaPlayer);
    }

    private void initialize() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);

        // bind listeners
        minWidthProperty().bind(fitWidthProperty());
        minHeightProperty().bind(fitHeightProperty());
        prefWidthProperty().bind(fitWidthProperty());
        prefHeightProperty().bind(fitHeightProperty());
        maxWidthProperty().bind(fitWidthProperty());
        maxHeightProperty().bind(fitHeightProperty());
    }

    @Override
    public ObjectProperty<MediaPlayer> mediaPlayerProperty() {
        if (mediaPlayer == null) {
            mediaPlayer = new SimpleObjectProperty<>(this, "mediaPlayer") {

                @Override
                protected void invalidated() {
                    MediaPlayer newMediaPlayer = getMediaPlayer();
                    if (newMediaPlayer instanceof FXMediaPlayer fxMediaPlayer) {
                        fxMediaView = new javafx.scene.media.MediaView(fxMediaPlayer.getMediaPlayer());
                        fxMediaView.setPreserveRatio(isPreserveRatio());
                        fxMediaView.setFitWidth(getFitWidth());
                        fxMediaView.setFitHeight(getFitHeight());
                        getChildren().setAll(fxMediaView);
                    }
                }
            };
        }
        return mediaPlayer;
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
                    log.debug("video width: " + getFitWidth());
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
                    log.debug("video height: " + getFitHeight());
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

    @Override
    protected void layoutChildren() {
        for (Node child : getManagedChildren()) {
            layoutInArea(child, 0.0, 0.0, getFitWidth(), getFitHeight(),
                    0.0, HPos.CENTER, VPos.CENTER);
        }
    }
}
