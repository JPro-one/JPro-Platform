package one.jpro.media.recorder.impl;

import com.jpro.webapi.HTMLView;
import com.jpro.webapi.WebAPI;
import javafx.beans.property.*;
import one.jpro.media.MediaEngine;
import one.jpro.media.MediaView;
import one.jpro.media.recorder.MediaRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link MediaView} implementation for a web {@link MediaRecorder}.
 *
 * @author Besmir Beqiri
 */
public class WebMediaRecorderView extends MediaView {

    private final Logger log = LoggerFactory.getLogger(WebMediaRecorderView.class);

    private final WebAPI webAPI;

    public WebMediaRecorderView(WebAPI webAPI) {
        this.webAPI = webAPI;
        initialize();
    }

    public WebMediaRecorderView(WebMediaRecorder webMediaRecorder) {
        this.webAPI = webMediaRecorder.getWebAPI();
        initialize();
        setMediaEngine(webMediaRecorder);
    }

    private void initialize() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);

        sceneProperty().addListener(observable -> {
            if (getScene() != null && webAPI != null && getMediaEngine() instanceof WebMediaRecorder webMediaRecorder) {
                webAPI.executeScript("""
                                let elem = document.getElementById("%s");
                                elem.width = "%s";
                                """.formatted(webMediaRecorder.getVideoRecorderId(), getFitWidth()));
                webAPI.executeScript("""
                                let elem = document.getElementById("%s");
                                elem.height = "%s";
                                """.formatted(webMediaRecorder.getVideoRecorderId(), getFitHeight()));
            }
        });
    }

    @Override
    public final ObjectProperty<MediaEngine> mediaEngineProperty() {
        if (mediaEngine == null) {
            mediaEngine = new SimpleObjectProperty<>(this, "mediaEngine") {

                @Override
                protected void invalidated() {
                    final MediaEngine mediaPlayer = getMediaEngine();
                    if (mediaPlayer instanceof WebMediaRecorder webMediaRecorder) {
                        HTMLView cameraView = new HTMLView("""
                                <video id="%s" width="%s" height="%s" autoplay muted></video>
                                """.formatted(webMediaRecorder.getVideoRecorderId(), getFitWidth(), getFitHeight()));
                        getChildren().setAll(cameraView);
                    }
                }
            };
        }
        return mediaEngine;
    }

    // disable controls property
    private BooleanProperty showControls;

    public final boolean getShowControls() {
        return showControls != null && showControls.get();
    }

    public final void setShowControls(boolean showControls) {
        showControlsProperty().set(showControls);
    }

    public final BooleanProperty showControlsProperty() {
        if (showControls == null) {
            showControls = new SimpleBooleanProperty(this, "showControls") {
                @Override
                protected void invalidated() {
                    if (webAPI != null && getMediaEngine() instanceof WebMediaRecorder webMediaRecorder) {
                        webAPI.executeScript("""
                                    let elem = document.getElementById('$mediaRecorderId');
                                    if ($showControls) {
                                        elem.setAttribute("controls","controls")
                                    } else if (elem.hasAttribute("controls")) {
                                        elem.removeAttribute("controls")
                                    }
                                    """.replace("$mediaRecorderId", webMediaRecorder.getVideoRecorderId())
                                .replace("$showControls", String.valueOf(get())));
                    }
                }
            };
        }
        return showControls;
    }

    @Override
    public final DoubleProperty fitWidthProperty() {
        if (fitWidth == null) {
            fitWidth = new SimpleDoubleProperty(this, "fitWidth") {

                @Override
                protected void invalidated() {
                    if (webAPI != null && getMediaEngine() instanceof WebMediaRecorder webMediaRecorder) {
                        webAPI.executeScript("""
                                let elem = document.getElementById("%s");
                                if (elem != null) {
                                    elem.width = "%s";
                                }
                                """.formatted(webMediaRecorder.getVideoRecorderId(), getFitWidth()));
                        log.debug("video width: " + getFitWidth());
                    }
                }
            };
        }
        return fitWidth;
    }

    @Override
    public final DoubleProperty fitHeightProperty() {
        if (fitHeight == null) {
            fitHeight = new SimpleDoubleProperty(this, "fitHeight") {
                @Override
                protected void invalidated() {
                    if (webAPI != null && getMediaEngine() instanceof WebMediaRecorder webMediaRecorder) {
                        webAPI.executeScript("""
                                let elem = document.getElementById("%s");
                                if (elem != null) {
                                    elem.height = "%s";
                                }
                                """.formatted(webMediaRecorder.getVideoRecorderId(), getFitHeight()));
                        log.debug("video height: " + get());
                    }
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
                    final boolean preserveRatio = get();
                    if (webAPI != null && getMediaEngine() instanceof WebMediaRecorder webMediaRecorder) {
                        if (preserveRatio) {
                            webAPI.executeScript("""
                                    let elem = document.getElementById('$mediaRecorderId');
                                    elem.style.objectFit = 'contain';
                                    console.log('$mediaPlayerId => preserve ratio: true');
                                    """.replace("$mediaRecorderId", webMediaRecorder.getVideoRecorderId()));
                        } else {
                            webAPI.executeScript("""
                                    let elem = document.getElementById('$mediaRecorderId');
                                    elem.style.objectFit = 'fill';
                                    console.log('$mediaPlayerId => preserve ratio: false');
                                    """.replace("$mediaRecorderId", webMediaRecorder.getVideoRecorderId()));
                        }
                        log.debug("preserve ratio: " + preserveRatio);
                    }
                }
            };
        }
        return preserveRatio;
    }
}
