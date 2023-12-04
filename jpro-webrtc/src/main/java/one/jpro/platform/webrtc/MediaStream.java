package one.jpro.platform.webrtc;

import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;

import java.util.concurrent.CompletableFuture;

public class MediaStream {

    public CompletableFuture<JSVariable> js;
    MediaStream(WebAPI webAPI, CompletableFuture<JSVariable> js) {
        this.js = js;
    }

    public static MediaStream getCameraStream(WebAPI webAPI) {
        var js = webAPI.executeJSAsync("return await navigator.mediaDevices.getUserMedia({video: true, audio: false});");
        return new MediaStream(webAPI, js);
    }

    public static MediaStream getScreenStream(WebAPI webAPI) {
        var js = webAPI.executeJSAsync("return await navigator.mediaDevices.getDisplayMedia({video: {\n" +
                "      displaySurface: \"window\",\n" +
                "    }, audio: false});");
        return new MediaStream(webAPI, js);
    }
}
