package one.jpro.platform.webrtc;

import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;

import java.util.concurrent.CompletableFuture;

/**
 * A browser media stream (camera or screen capture), obtained asynchronously via the JavaScript
 * {@code navigator.mediaDevices} API. The underlying JS stream is available through {@link #js()}.
 */
public class MediaStream {

    private final CompletableFuture<JSVariable> js;

    MediaStream(WebAPI webAPI, CompletableFuture<JSVariable> js) {
        this.js = js;
    }

    /** The underlying JS {@code MediaStream}, available once the user has granted access. */
    public CompletableFuture<JSVariable> js() {
        return js;
    }

    /** Requests the user's camera (video only). */
    public static MediaStream getCameraStream(WebAPI webAPI) {
        var js = webAPI.executeJSAsync("return await navigator.mediaDevices.getUserMedia({video: true, audio: false});");
        return new MediaStream(webAPI, js);
    }

    /** Requests screen capture (a window, video only). */
    public static MediaStream getScreenStream(WebAPI webAPI) {
        var js = webAPI.executeJSAsync("return await navigator.mediaDevices.getDisplayMedia({video: {\n" +
                "      displaySurface: \"window\",\n" +
                "    }, audio: false});");
        return new MediaStream(webAPI, js);
    }
}
