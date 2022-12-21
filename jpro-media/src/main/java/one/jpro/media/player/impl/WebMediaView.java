package one.jpro.media.player.impl;

import com.jpro.webapi.HTMLView;
import com.jpro.webapi.WebAPI;
import javafx.beans.property.*;
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

    private WebAPI webAPI;

    public WebMediaView(WebMediaPlayer webMediaPlayer) {
        this.webAPI = webMediaPlayer.getWebAPI();
        getStyleClass().add(DEFAULT_STYLE_CLASS);

        setMediaPlayer(webMediaPlayer);

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
                    MediaPlayer newMediaPlayer = getMediaPlayer();
                    if (newMediaPlayer instanceof WebMediaPlayer webMediaPlayer) {
                        HTMLView htmlView = new HTMLView("""
                                <video id="%s" controls></video>
                                """.formatted(webMediaPlayer.getMediaPlayerId()));
                        getChildren().setAll(htmlView);
                    }
                }
            };
        }
        return mediaPlayer;
    }


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

    public final BooleanProperty preserveRatioProperty() {
        if (preserveRatio == null) {
            preserveRatio = new SimpleBooleanProperty(this, "preserveRatio", true) {

                @Override
                protected void invalidated() {
                    final boolean preserveRatio = get();
                    if (webAPI != null && getMediaPlayer() instanceof WebMediaPlayer webMediaPlayer) {
                        if (preserveRatio) {
                            webAPI.executeScript("""
                                    var video = document.getElementById('%s');
                                    video.style.objectFit = 'contain';
                                    """.formatted(webMediaPlayer.getMediaPlayerId()));
                        } else {
                            webAPI.executeScript("""
                                    var video = document.getElementById('%s');
                                    video.style.objectFit = 'fill';
                                    """.formatted(webMediaPlayer.getMediaPlayerId()));
                        }
                        log.debug("preserve ratio: " + preserveRatio);
                    }
                }
            };
        }
        return preserveRatio;
    }
}
