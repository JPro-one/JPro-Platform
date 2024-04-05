package one.jpro.platform.sipjs.example.component;

import com.jpro.webapi.WebAPI;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import one.jpro.platform.sipjs.api.UserAgent;
import one.jpro.platform.sipjs.api.options.InvitationAcceptOptions;
import one.jpro.platform.sipjs.api.options.InviterOptions;
import one.jpro.platform.sipjs.api.options.UserAgentOptions;
import one.jpro.platform.sipjs.api.session.Invitation;
import one.jpro.platform.sipjs.api.session.Session;
import one.jpro.platform.webrtc.MediaStream;
import one.jpro.platform.webrtc.VideoFrame;

public class User extends VBox {
    WebAPI webapi;
    UserAgent userAgent;
    String target;
    ObjectProperty<Session> session = new SimpleObjectProperty(null);

    VideoFrame localVideoElement;
    VideoFrame remoteVideoElement;
    public User(WebAPI webapi, String server, String sip, String displayName, String target) {
        this.target = target;
        this.webapi = webapi;
        var options = new UserAgentOptions();
        this.getStyleClass().add("user-container");
        this.getChildren().add(new Label(displayName+ ":"));
        options.addServer(server);
        options.addUri(sip);
        options.addDisplayName(displayName);
        userAgent = new UserAgent(options, webapi);
        InviterOptions.createVideoCall();
        userAgent.setOnInvite(invitation -> {
            //invitation.accept(InvitationAcceptOptions.createVideoOnlyCall());
            //handleSession(webapi, invitation, this);
            session.set(invitation);
        });
        makeCallButton();
        makeAcceptButton();
        makeRejectButton();
        makeHangupButton();
        makeScreenShareButton();
    }

    public void makeCall() {
        userAgent.makeCall(target, InviterOptions.createVideoOnlyCall()).thenAccept(session -> {
            this.session.set(session);
            handleSession(webapi, session, this);
        });
    }

    public void makeCallButton() {
        // Active when session is null
        var button = new Button("Call");
        button.getStyleClass().add("call-button");
        button.setOnAction(event -> {
            makeCall();
        });
        session.addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                button.setDisable(false);
            } else {
                button.setDisable(true);
            }
        });
        getChildren().add(button);
    }
    public void makeAcceptButton() {
        // Active when session is a Inventation
        var button = new Button("Accept");
        button.getStyleClass().add("call-button");
        button.setOnAction(event -> {
            ((Invitation) session.get()).accept(InvitationAcceptOptions.createVideoOnlyCall());
            handleSession(webapi, session.get(), this);
        });
        session.addListener((observable, oldValue, newValue) -> {
            if (newValue instanceof Invitation) {
                button.setDisable(false);
            } else {
                button.setDisable(true);
            }
        });
        button.setDisable(true);
        getChildren().add(button);
    }

    public void makeRejectButton() {
        // Active when session is a Inventation
        var button = new Button("Reject");
        button.getStyleClass().add("call-button");
        button.setOnAction(event -> {
            ((Invitation) session.get()).reject();
            session.set(null);
        });
        session.addListener((observable, oldValue, newValue) -> {
            if (newValue instanceof Invitation) {
                button.setDisable(false);
            } else {
                button.setDisable(true);
            }
        });
        button.setDisable(true);
        getChildren().add(button);
    }

    public void makeHangupButton() {
        // Active when session is not null
        var button = new Button("Hangup");
        button.getStyleClass().add("call-button");
        button.setOnAction(event -> {
            session.get().bye();
            session.set(null);
        });
        session.addListener((observable, oldValue, newValue) -> {
            button.setDisable(session.get() == null);
        });
        button.setDisable(session.get() == null);
        getChildren().add(button);
    }

    public void makeScreenShareButton() {
        // Active when session is not null
        var button = new Button("Screen Share");
        button.getStyleClass().add("call-button");
        button.setOnAction(event -> {
            var stream = MediaStream.getScreenStream(webapi);
            session.get().switchToStream(stream);
            localVideoElement.setStream(stream);
        });
        session.addListener((observable, oldValue, newValue) -> {
            button.setDisable(session.get() == null);
        });
        button.setDisable(session.get() == null);
        getChildren().add(button);


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
        localVideoElement = new VideoFrame(webapi);
        localVideoElement.getStyleClass().add("video-local");
        localVideoElement.setStream(localVideoStream);
        var description = new Label("Local Video");
        container.getChildren().add(description);
        container.getChildren().add(localVideoElement);

        // Add remote video
        var remoteVideoStream = session.getRemoteStream();
        webapi.executeScript("console.log('remoteVideoStream: ' + "+remoteVideoStream.getName()+");");
        remoteVideoElement = new VideoFrame(webapi);
        remoteVideoElement.getStyleClass().add("video-remote");
        remoteVideoElement.setStream(remoteVideoStream);
        var remoteDescription = new Label("Remote Video");
        container.getChildren().add(remoteDescription);
        container.getChildren().add(remoteVideoElement);
    }
}
