package one.jpro.platform.webrtc;

import com.jpro.webapi.HTMLView;
import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;

public class VideoFrame extends HTMLView {

    private WebAPI webAPI;
    JSVariable elem;

    JSVariable videoElem;


    public VideoFrame(WebAPI webAPI) {
        super("<video autoplay playsinline ></video>");
        this.webAPI = webAPI;
        elem = webAPI.getHTMLViewElement(this);
        videoElem = webAPI.executeScriptWithVariable(elem.getName()+".firstElementChild");

        setPrefSize(100,100);

        widthProperty().addListener((observable, oldValue, newValue) -> {
            webAPI.executeScript(videoElem.getName()+".width = "+newValue.intValue()+";");
        });
        heightProperty().addListener((observable, oldValue, newValue) -> {
            webAPI.executeScript(videoElem.getName()+".height = "+newValue.intValue()+";");
        });
    }

    public JSVariable getVideoElem() {
        return videoElem;
    }

    public void setStream(MediaStream stream) {
        stream.js.thenAccept(s -> {
            webAPI.executeScript(videoElem.getName()+".srcObject = "+s.getName()+";");
        });
    }

    public void setStream(JSVariable stream) {
        webAPI.executeScript(videoElem.getName()+".srcObject = "+stream.getName()+";");
    }
}
