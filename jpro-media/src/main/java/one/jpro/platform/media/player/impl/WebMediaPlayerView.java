package one.jpro.platform.media.player.impl;

import com.jpro.webapi.WebAPI;
import one.jpro.platform.media.MediaView;
import one.jpro.platform.media.WebMediaView;
import one.jpro.platform.media.player.MediaPlayer;

/**
 * {@link MediaView} implementation for a web {@link MediaPlayer}.
 *
 * @author Besmir Beqiri
 */
public class WebMediaPlayerView extends WebMediaView {

    public WebMediaPlayerView(WebAPI webAPI) {
        super(webAPI);
    }

    public WebMediaPlayerView(WebMediaPlayer webMediaPlayer) {
        this(webMediaPlayer.getWebAPI());
        setMediaEngine(webMediaPlayer);
    }
}
