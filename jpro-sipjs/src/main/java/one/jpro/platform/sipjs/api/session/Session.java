package one.jpro.platform.sipjs.api.session;

import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import one.jpro.platform.webrtc.MediaStream;
import one.jpro.platform.webrtc.VideoFrame;

/**
 * Represents a call.
 */
public class Session {

    WebAPI webapi;
    JSVariable session;

    StringProperty state;

    JSVariable jsFun;

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
        jsFun = webapi.registerJavaFunction(str -> {
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

    /**
     * Switches the stream of the call to the given stream.
     * @param stream
     */
    public void switchToStream(MediaStream stream) {
        stream.js.thenAccept(js -> {
            webapi.executeScript("var videoTrack = "+js.getName()+".getVideoTracks()[0];\n" +
                    "var sender = "+session.getName()+".sessionDescriptionHandler.peerConnection.getSenders().find(function(s) {\n" +
                    "  return s.track.kind == videoTrack.kind;\n" +
                    "});\n" +
                    "console.log('found sender:', sender);\n" +
                    "sender.replaceTrack(videoTrack);");
        });
    }

    public class State {
        public static final String Initial = "Initial";
        public static final String Establishing = "Establishing";
        public static final String Established = "Established";
        public static final String Terminating = "Terminating";
        public static final String Terminated = "Terminated";
    }

    public void bye() {
        webapi.executeScript(session.getName() + ".bye();");
    }

    public void endCall() {
        /* From the SIPjs documentation:
        function endCall() {
  switch(session.state) {
    case SessionState.Initial:
    case SessionState.Establishing:
      if (session instanceOf Inviter) {
        // An unestablished outgoing session
        session.cancel();
      } else {
        // An unestablished incoming session
        session.reject();
      }
      break;
    case SessionState.Established:
      // An established session
      session.bye();
      break;
    case SessionState.Terminating:
    case SessionState.Terminated:
      // Cannot terminate a session that is already terminated
      break;
  }
}
         */
        webapi.executeScript("switch("+session.getName()+".state) {\n" +
                "    case SessionState.Initial:\n" +
                "    case SessionState.Establishing:\n" +
                "      if ("+session.getName()+" instanceOf Inviter) {\n" +
                "        // An unestablished outgoing session\n" +
                "        "+session.getName()+".cancel();\n" +
                "      } else {\n" +
                "        // An unestablished incoming session\n" +
                "        "+session.getName()+".reject();\n" +
                "      }\n" +
                "      break;\n" +
                "    case SessionState.Established:\n" +
                "      // An established session\n" +
                "      "+session.getName()+".bye();\n" +
                "      break;\n" +
                "    case SessionState.Terminating:\n" +
                "    case SessionState.Terminated:\n" +
                "      // Cannot terminate a session that is already terminated\n" +
                "      break;\n" +
                "  }");
    }
}
