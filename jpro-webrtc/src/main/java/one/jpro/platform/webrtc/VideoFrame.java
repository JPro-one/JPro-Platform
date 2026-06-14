package one.jpro.platform.webrtc;

import com.jpro.webapi.HTMLView;
import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;

/**
 * A JavaFX node that renders a live media stream, backed by an HTML {@code <video>} element.
 * Set the stream to display with {@link #setStream(MediaStream)}.
 */
public class VideoFrame extends HTMLView {

    private final WebAPI webAPI;
    private final JSVariable elem;
    private final JSVariable videoElem;

    public VideoFrame(WebAPI webAPI) {
        super("<video autoplay playsinline ></video>");
        this.webAPI = webAPI;
        elem = webAPI.getHTMLViewElement(this);
        videoElem = webAPI.executeScriptWithVariable(elem.getName()+".firstElementChild");

        setPrefSize(100,100);

        widthProperty().addListener((observable, oldValue, newValue) ->
            webAPI.executeScript(videoElem.getName()+".width = "+newValue.intValue()+";"));
        heightProperty().addListener((observable, oldValue, newValue) ->
            webAPI.executeScript(videoElem.getName()+".height = "+newValue.intValue()+";"));
    }

    /** The underlying HTML {@code <video>} element (low-level). */
    public JSVariable getVideoElem() {
        return videoElem;
    }

    /** Displays the given media stream once it is available. */
    public void setStream(MediaStream stream) {
        stream.js().thenAccept(s ->
            webAPI.executeScript(videoElem.getName()+".srcObject = "+s.getName()+";"));
    }

    /** Displays the given (already resolved) media stream. */
    public void setStream(JSVariable stream) {
        webAPI.executeScript(videoElem.getName()+".srcObject = "+stream.getName()+";");
    }
}
