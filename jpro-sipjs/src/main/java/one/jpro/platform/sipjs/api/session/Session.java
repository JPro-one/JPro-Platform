package one.jpro.platform.sipjs.api.session;

import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import one.jpro.platform.webrtc.VideoFrame;

/**
 * Represents a call.
 */
public class Session {

    WebAPI webapi;
    JSVariable session;

    StringProperty state;

    Session(JSVariable session, WebAPI webapi) {
        session.isPromise().thenAccept((v) -> {
            if(v == true) {
                // this shouldn't be the case
                new RuntimeException("JSVariable for Session is a promise").printStackTrace();
            }
        });

        this.session = session;
        this.webapi = webapi;

        state = new SimpleStringProperty(State.Initial);
        var jsFun = webapi.registerJavaFunction(str -> {
            // remove first and last character
            str = str.substring(1, str.length() - 1);
            state.set(str);
        });
        webapi.executeScript(session.getName() + ".stateChange.addListener(" + jsFun.getName() + ");");
    }

    public StringProperty stateProperty() {
        return state;
    }


    public JSVariable getLocalStream() {
        // localMediaStream
        return webapi.executeScriptWithVariable(session.getName() + ".sessionDescriptionHandler.localMediaStream");
    }

    public JSVariable getRemoteStream() {
        // remoteMediaStream
        return webapi.executeScriptWithVariable(session.getName() + ".sessionDescriptionHandler.remoteMediaStream");
    }

    public void setupRemoteMedia(VideoFrame frame) {
        webapi.executeScript("const remoteStream = new MediaStream();\n" +
                session.getName()+".sessionDescriptionHandler.peerConnection.getReceivers().forEach((receiver) => {\n" +
                "  if (receiver.track) {\n" +
                "    remoteStream.addTrack(receiver.track);\n" +
                "  }\n" +
                "});\n" +
                frame.getVideoElem().getName()+".srcObject = remoteStream;\n" +
                frame.getVideoElem().getName()+".play();");
    }

    public class State {
        public static final String Initial = "Initial";
        public static final String Establishing = "Establishing";
        public static final String Established = "Established";
        public static final String Terminating = "Terminating";
        public static final String Terminated = "Terminated";
    }
}
