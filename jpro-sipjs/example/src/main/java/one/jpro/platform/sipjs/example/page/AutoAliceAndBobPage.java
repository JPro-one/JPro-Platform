package one.jpro.platform.sipjs.example.page;

import com.jpro.webapi.WebAPI;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import one.jpro.platform.sipjs.api.UserAgent;
import one.jpro.platform.sipjs.api.options.InvitationAcceptOptions;
import one.jpro.platform.sipjs.api.options.InviterOptions;
import one.jpro.platform.sipjs.api.options.UserAgentOptions;
import one.jpro.platform.sipjs.api.session.Session;
import one.jpro.platform.webrtc.VideoFrame;

public class AutoAliceAndBobPage extends VBox {


    private String server = "wss://edge.sip.onsip.com";
    String sipAlice = "sip:alice.swCqVznyordTFItTopErJxcn2qxtdxR1@sipjs.onsip.com";
    String sipBob = "sip:bob.swCqVznyordTFItTopErJxcn2qxtdxR1@sipjs.onsip.com";

    public AutoAliceAndBobPage() {
        // Add title
        var title = new Label("Alice and Bob");
        title.getStyleClass().add("title");
        getChildren().add(title);
        WebAPI.getWebAPI(this, webapi -> {
            setup(webapi);
        });
        getStyleClass().add("jpro-sipjs-example-page");
    }

    public void setup(WebAPI webapi) {
        var user1 = new User(webapi, sipAlice, "Alice", sipBob);
        var user2 = new User(webapi, sipBob, "Bob", null);

        var hbox = new HBox(user1, user2);
        hbox.getStyleClass().add("alice-and-bob-hbox");

        getChildren().addAll(hbox);
    }

    class User extends VBox {
        public User(WebAPI webapi, String sip, String displayName, String target) {
            var options = new UserAgentOptions();
            this.getStyleClass().add("user-container");
            this.getChildren().add(new Label(displayName+ ":"));
            options.addServer(server);
            options.addUri(sip);
            options.addDisplayName(displayName);
            var userAgent = new UserAgent(options, webapi);
            InviterOptions.createVideoCall();
            new Thread(() -> {
                try {
                    // The other client first has to register
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(target != null) {
                    userAgent.makeCall(target, InviterOptions.createVideoOnlyCall()).thenAccept(session -> {
                        //session.state
                        handleSession(webapi, session, this);
                    });
                }
            }).start();
            userAgent.setOnInvite(invitation -> {
                invitation.accept(InvitationAcceptOptions.createVideoOnlyCall());
                handleSession(webapi, invitation, this);
            });
        }
    }

    public void handleSession(WebAPI webapi, Session session, Pane container) {
        System.out.println("Session: " + session);

        // Session state

        var state = new Label();
        state.getStyleClass().add("session-state");
        state.textProperty().bind(session.stateProperty());
        container.getChildren().add(state);

        // When state is "established"
        session.stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.equals(Session.State.Established)) {
                System.out.println("Session established");
                addVideo(webapi, session, container);
            }
        });
    }

    public void addVideo(WebAPI webapi, Session session, Pane container) {
        // Add local video
        var localVideoStream = session.getLocalStream();
        webapi.executeScript("console.log('localVideoStream: ' + "+localVideoStream.getName()+");");
        var videoElement = new VideoFrame(webapi);
        videoElement.getStyleClass().add("video-local");
        videoElement.setStream(localVideoStream);
        var description = new Label("Local Video");
        container.getChildren().add(description);
        container.getChildren().add(videoElement);

        // Add remote video
        var remoteVideoStream = session.getRemoteStream();
        webapi.executeScript("console.log('remoteVideoStream: ' + "+remoteVideoStream.getName()+");");
        var remoteVideoElement = new VideoFrame(webapi);
        remoteVideoElement.getStyleClass().add("video-remote");
        remoteVideoElement.setStream(remoteVideoStream);
        var remoteDescription = new Label("Remote Video");
        container.getChildren().add(remoteDescription);
        container.getChildren().add(remoteVideoElement);
    }
}
