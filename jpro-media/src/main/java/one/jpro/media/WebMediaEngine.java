package one.jpro.media;

import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;

/**
 * Media engine interface for the web.
 *
 * @author Besmir Beqiri
 */
public interface WebMediaEngine extends MediaEngine {

    /**
     * Returns the {@link WebAPI} instance.
     *
     * @return the {@link WebAPI} instance
     */
    WebAPI getWebAPI();

    /**
     * Returns the video element used to both play and record media on the browser.
     *
     * @return the video element as a {@link JSVariable}
     */
    JSVariable getVideoElement();
}
