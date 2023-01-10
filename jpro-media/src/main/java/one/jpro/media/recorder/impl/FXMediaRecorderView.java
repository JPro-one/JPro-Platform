package one.jpro.media.recorder.impl;

import javafx.beans.property.*;
import javafx.scene.image.ImageView;
import one.jpro.media.MediaEngine;
import one.jpro.media.MediaView;
import one.jpro.media.recorder.MediaRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link MediaView} implementation for desktop/mobile {@link MediaRecorder}.
 *
 * @author Besmir Beqiri
 */
public class FXMediaRecorderView extends MediaView {

    private final Logger log = LoggerFactory.getLogger(FXMediaRecorderView.class);

    private ImageView fxFrameView;

    public FXMediaRecorderView() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
    }

    public FXMediaRecorderView(FXMediaRecorder mediaRecorder) {
        this();
        setMediaEngine(mediaRecorder);
    }

    @Override
    public ObjectProperty<MediaEngine> mediaEngineProperty() {
        if (mediaEngine == null) {
            mediaEngine = new SimpleObjectProperty<>(this, "mediaEngine") {

                @Override
                protected void invalidated() {
                    final MediaEngine mediaRecorder = getMediaEngine();
                    if (mediaRecorder instanceof FXMediaRecorder fxMediaRecorder) {
                        fxFrameView = fxMediaRecorder.getCameraView();
                        fxFrameView.setPreserveRatio(isPreserveRatio());
                        fxFrameView.setFitWidth(getFitWidth());
                        fxFrameView.setFitHeight(getFitHeight());
                        getChildren().setAll(fxFrameView);
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
                    if (fxFrameView != null) {
                        fxFrameView.setFitWidth(get());
                    }
                    log.debug("video width: {}", getFitWidth());
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
                    if (fxFrameView != null) {
                        fxFrameView.setFitHeight(get());
                    }
                    log.debug("video height: {}", getFitHeight());
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
                    if (fxFrameView != null) {
                        fxFrameView.setPreserveRatio(get());
                    }
                    log.debug("preserve ratio: {}", isPreserveRatio());
                }
            };
        }
        return preserveRatio;
    }
}
