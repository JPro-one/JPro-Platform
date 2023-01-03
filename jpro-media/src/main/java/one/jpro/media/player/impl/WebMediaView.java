package one.jpro.media.player.impl;

import com.jpro.webapi.HTMLView;
import com.jpro.webapi.WebAPI;
import javafx.beans.property.*;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import one.jpro.media.player.MediaPlayer;
import one.jpro.media.player.MediaView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A that provides a view of Media being played by a {@link MediaPlayer}.
 *
 * @author Besmir Beqiri
 */
public class WebMediaView extends MediaView {

    private final Logger log = LoggerFactory.getLogger(WebMediaView.class);

    private static final String DEFAULT_STYLE_CLASS = "media-view";

    private final WebAPI webAPI;

    public WebMediaView(WebAPI webAPI) {
        this.webAPI = webAPI;
        initialize();
    }

    public WebMediaView(WebMediaPlayer webMediaPlayer) {
        this.webAPI = webMediaPlayer.getWebAPI();
        initialize();
        setMediaPlayer(webMediaPlayer);
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
    public final ObjectProperty<MediaPlayer> mediaPlayerProperty() {
        if (mediaPlayer == null) {
            mediaPlayer = new SimpleObjectProperty<>(this, "mediaPlayer") {

                @Override
                protected void invalidated() {
                    final MediaPlayer mediaPlayer = getMediaPlayer();
                    if (mediaPlayer instanceof WebMediaPlayer webMediaPlayer) {
                        HTMLView htmlView = new HTMLView("""
                                <video id="%s" width="%spx" height="%spx"></video>
                                """.formatted(webMediaPlayer.getMediaPlayerId(), getFitWidth(), getFitHeight()));
                        getChildren().setAll(htmlView);
                    }
                }
            };
        }
        return mediaPlayer;
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
                    if (webAPI != null && getMediaPlayer() instanceof WebMediaPlayer webMediaPlayer) {
                        webAPI.executeScript("""
                                    let elem = document.getElementById('$mediaPlayerId');
                                    if ($showControls) {
                                        elem.setAttribute("controls","controls")
                                    } else if (elem.hasAttribute("controls")) {
                                        elem.removeAttribute("controls")
                                    }
                                    """.replace("$mediaPlayerId", webMediaPlayer.getMediaPlayerId())
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
                    if (webAPI != null && getMediaPlayer() instanceof WebMediaPlayer webMediaPlayer) {
                        webAPI.executeScript("""
                                let elem = document.getElementById("%s");
                                elem.style.width = "%spx";
                                """.formatted(webMediaPlayer.getMediaPlayerId(), getFitWidth()));
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
                    if (webAPI != null && getMediaPlayer() instanceof WebMediaPlayer webMediaPlayer) {
                        webAPI.executeScript("""
                                let elem = document.getElementById("%s");
                                elem.style.height = "%spx";
                                """.formatted(webMediaPlayer.getMediaPlayerId(), getFitHeight()));
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
                    if (webAPI != null && getMediaPlayer() instanceof WebMediaPlayer webMediaPlayer) {
                        if (preserveRatio) {
                            webAPI.executeScript("""
                                    let elem = document.getElementById('$mediaPlayerId');
                                    elem.style.objectFit = 'contain';
                                    console.log('$mediaPlayerId => preserve ratio: true');
                                    """.replace("$mediaPlayerId", webMediaPlayer.getMediaPlayerId()));
                        } else {
                            webAPI.executeScript("""
                                    let elem = document.getElementById('$mediaPlayerId');
                                    elem.style.objectFit = 'fill';
                                    console.log('$mediaPlayerId => preserve ratio: false');
                                    """.replace("$mediaPlayerId", webMediaPlayer.getMediaPlayerId()));
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
            layoutInArea(child, 0.0, 0.0, getFitWidth(), getFitHeight(),
                    0.0, HPos.CENTER, VPos.CENTER);
        }
    }
}
