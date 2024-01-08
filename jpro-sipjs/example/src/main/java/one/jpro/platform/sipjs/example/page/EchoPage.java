package one.jpro.platform.sipjs.example.page;

import com.jpro.webapi.WebAPI;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import one.jpro.platform.sipjs.api.UserAgent;
import one.jpro.platform.sipjs.api.options.InviterOptions;
import one.jpro.platform.sipjs.api.options.UserAgentOptions;
import one.jpro.platform.sipjs.api.session.Session;
import one.jpro.platform.webrtc.VideoFrame;

public class EchoPage extends VBox {

    private String server = "wss://edge.sip.onsip.com";
    private String target = "sip:echo@sipjs.onsip.com";
    private String displayName = "EchoUser";


    public EchoPage() {
        var title = new Label("Echo");
        title.getStyleClass().add("title");
        getChildren().add(title);

        WebAPI.getWebAPI(this, webapi -> {
            setup(webapi);
        });
        getStyleClass().add("jpro-sipjs-example-page");
    }

    public void setup(WebAPI webapi) {
        var options = new UserAgentOptions();
        options.addServer(server);
        options.addUri(target);
        options.addDisplayName(displayName);
        var userAgent = new UserAgent(options, webapi);
        userAgent.makeCall(target, InviterOptions.createAudioCall()).thenAccept(session -> {
            //session.state
            handleSession(webapi, session);
        });

        System.out.println("Call made");
    }

    public void handleSession(WebAPI webapi, Session session) {
        System.out.println("Session: " + session);

        // Session state

        var state = new Label();
        state.textProperty().bind(session.stateProperty());
        getChildren().add(state);

        // When state is "established"
        session.stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.equals(Session.State.Established)) {
                System.out.println("Session established");
                addVideo(webapi, session);
            }
        });
    }

    public void addVideo(WebAPI webapi, Session session) {
        // Add local video
      //  var localVideoStream = session.getLocalSteam();
        var videoElement = new VideoFrame(webapi);
     //   videoElement.setStream(localVideoStream);
        var description = new Label("Local Video");
        getChildren().add(description);
        getChildren().add(videoElement);

        // Add remote video
        var remoteVideoStream = session.getRemoteStream();
        var remoteVideoElement = new VideoFrame(webapi);
        //session.setupRemoteMedia(remoteVideoElement);
        remoteVideoElement.setStream(remoteVideoStream);
        var remoteDescription = new Label("Remote Video");
        getChildren().add(remoteDescription);
        getChildren().add(remoteVideoElement);
    }

}
