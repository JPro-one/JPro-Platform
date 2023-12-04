package one.jpro.platform.webrtc;

import com.jpro.webapi.HTMLView;
import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;

public class VideoFrame extends HTMLView {

    private WebAPI webAPI;
    JSVariable elem;
    public VideoFrame(WebAPI webAPI) {
        super("<video autoplay playsinline ></video>");
        this.webAPI = webAPI;
        elem = webAPI.getHTMLViewElement(this);

        setPrefSize(100,100);

        widthProperty().addListener((observable, oldValue, newValue) -> {
            webAPI.executeScript(elem.getName()+".firstElementChild.width = "+newValue.intValue()+";");
        });
        heightProperty().addListener((observable, oldValue, newValue) -> {
            webAPI.executeScript(elem.getName()+".firstElementChild.height = "+newValue.intValue()+";");
        });
    }

    public void setStream(MediaStream stream) {
        stream.js.thenAccept(s -> {
            webAPI.executeScript(elem.getName()+".firstElementChild.srcObject = "+s.getName()+";");
        });
    }

    public void setStream(JSVariable stream) {
        webAPI.executeScript(elem.getName()+".firstElementChild.srcObject = "+stream.getName()+";");
    }
}
