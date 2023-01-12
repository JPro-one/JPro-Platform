package one.jpro.media;

import com.jpro.webapi.HTMLView;
import com.jpro.webapi.WebAPI;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.*;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * {@link MediaView} base implementation for the web.
 *
 * @author Besmir Beqiri
 */
public abstract class WebMediaView extends MediaView {

    private final Logger log = LoggerFactory.getLogger(WebMediaView.class);

    private final WebAPI webAPI;
    private String mediaContainerId;

    public WebMediaView(WebAPI webAPI) {
        this.webAPI = Objects.requireNonNull(webAPI, "WebAPI must not be null.");
        initialize();
    }

    protected void initialize() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
        mediaContainerId = webAPI.createUniqueJSName("media_container_id_");
        final HTMLView viewContainer = new HTMLView("""
                <div id="%s"></div>
                """.formatted(mediaContainerId));
        getChildren().setAll(viewContainer);
    }

    @Override
    public final ObjectProperty<MediaEngine> mediaEngineProperty() {
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

    // disable controls property
    private BooleanProperty showControls;

    public final boolean isShowControls() {
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
                    if (getMediaEngine() instanceof WebMediaEngine webMediaEngine) {
                        webAPI.executeScript("""
                                %s.controls = $controls;
                                """.formatted(webMediaEngine.getVideoElement().getName())
                                .replace("$controls", String.valueOf(get())));
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
                    if (getMediaEngine() instanceof WebMediaEngine webMediaEngine) {
                        webAPI.executeScript("""
                                %s.width = "%s";
                                """.formatted(webMediaEngine.getVideoElement().getName(), getFitWidth()));
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
                    if (getMediaEngine() instanceof WebMediaEngine webMediaEngine) {
                        webAPI.executeScript("""
                                %s.height = "%s";
                                """.formatted(webMediaEngine.getVideoElement().getName(), getFitHeight()));
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
                    if (getMediaEngine() instanceof WebMediaEngine webMediaEngine) {
                        if (preserveRatio) {
                            webAPI.executeScript("""
                                    %s.style.objectFit = 'contain';
                                    """.formatted(webMediaEngine.getVideoElement().getName()));
                        } else {
                            webAPI.executeScript("""
                                    %s.style.objectFit = 'fill';
                                    """.formatted(webMediaEngine.getVideoElement().getName()));
                        }
                        log.debug("preserve ratio: " + preserveRatio);
                    }
                }
            };
        }
        return preserveRatio;
    }

    @Override
    protected void layoutChildren() {
        for (Node child : getManagedChildren()) {
            if (getMediaEngine() instanceof WebMediaEngine webMediaEngine) {
                if (getFitWidth() <= 0) {
                    webAPI.executeScript("""
                            %s.width = "%s";
                            """.formatted(webMediaEngine.getVideoElement().getName(), getWidth()));
                }
                if (getFitHeight() <= 0) {
                    webAPI.executeScript("""
                            %s.height = "%s";
                            """.formatted(webMediaEngine.getVideoElement().getName(), getHeight()));
                }
            }
            layoutInArea(child, 0.0, 0.0, getWidth(), getHeight(),
                    0.0, HPos.CENTER, VPos.CENTER);
        }
    }

    private final InvalidationListener updateViewContainerListener = observable -> updateViewContainer();
    private final WeakInvalidationListener weakUpdateViewContainerListener =
            new WeakInvalidationListener(updateViewContainerListener);

    private void updateViewContainer() {
        if (getScene() != null && getMediaEngine() instanceof WebMediaEngine webMediaEngine) {
            webAPI.executeScript("""
                    let elem = document.getElementById("%s");
                    // clear all elements
                    while (elem.firstChild) {
                        elem.removeChild(elem.firstChild);
                    }
                    // add new element
                    elem.appendChild(%s);
                    """.formatted(mediaContainerId, webMediaEngine.getVideoElement().getName()));
            webAPI.executeScript("""
                    %s.width = "%s";
                    """.formatted(webMediaEngine.getVideoElement().getName(), getFitWidth()));
            webAPI.executeScript("""
                    %s.height = "%s";
                    """.formatted(webMediaEngine.getVideoElement().getName(), getFitHeight()));
            webAPI.executeScript("""
                    %s.controls = $controls;
                    """.formatted(webMediaEngine.getVideoElement().getName())
                    .replace("$controls", String.valueOf(isShowControls())));
        }
    }
}
