package one.jpro.platform.webrtc.example.videoroom.page;

import com.jpro.webapi.WebAPI;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import one.jpro.platform.webrtc.MediaStream;
import one.jpro.platform.webrtc.RTCPeerConnection;
import one.jpro.platform.webrtc.VideoFrame;
import one.jpro.platform.webrtc.example.videoroom.model.VideoRoom;

public class VideoRoomPage extends VBox {

    VideoRoom videoRoom;

    static int i = 0;

    public VideoRoomPage(String id, WebAPI webAPI) {
        getStyleClass().add("page");
        getStyleClass().add("video-room-page");

        videoRoom = VideoRoom.getOrCreateRoom(id);

        i += 1;

        var me = new VideoRoom.User("Me_"+i, MediaStream.getCameraStream(webAPI), webAPI);

        var roomName = new Label("Room: " + id);
        roomName.getStyleClass().add("room-name");

        var usrNameLabel = new Label("Name: ");
        usrNameLabel.getStyleClass().add("user-name-label");
        var usrName = new TextField();
        usrName.getStyleClass().add("user-name-field");
        usrName.setMaxWidth(100);
        usrName.textProperty().bindBidirectional(me.name);
        var usrNameBox = new HBox();
        usrNameBox.getStyleClass().add("user-name-box");
        usrNameBox.getChildren().addAll(usrNameLabel, usrName);

        var otherViews = new HBox();

        otherViews.getStyleClass().add("other-views");

        me.userVideos.addListener((ListChangeListener<? super VideoRoom.UserVideo>) change -> {
            while(change.next()) {
                if(change.wasAdded()) {
                    var frames = change.getAddedSubList().stream()
                            .map(userVideo -> videoFrameContainer(userVideo.videoFrame, userVideo.user, userVideo.connection))
                            .toArray(Node[]::new);
                    otherViews.getChildren().addAll(frames);
                }
            }
        });

        videoRoom.addUserAndCreateConnections(me);

        var userVideo = new VideoFrame(webAPI);
        me.mediaStream.addListener((observable, oldValue, newValue) -> {
            userVideo.setStream(newValue);
        });
        userVideo.setStream(me.mediaStream.get());

        var buttons = new HBox();
        buttons.getStyleClass().add("buttons");
        var shareScreen = new Button("Share Screen");
        buttons.getChildren().add(shareScreen);

        shareScreen.setOnAction(e -> {
            me.mediaStream.set(MediaStream.getScreenStream(webAPI));
        });

        getChildren().add(roomName);
        getChildren().add(usrNameBox);
        getChildren().add(otherViews);
        getChildren().add(videoFrameContainer(userVideo, me, null));
        getChildren().add(buttons);
    }

    public Node videoFrameContainer(VideoFrame frame, VideoRoom.User user, RTCPeerConnection connection) {
        StackPane wrapper = new StackPane();
        var clip = new Rectangle();
        clip.widthProperty().bind(wrapper.widthProperty());
        clip.heightProperty().bind(wrapper.heightProperty());
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        wrapper.setClip(clip);
        wrapper.getStyleClass().add("video-frame-wrapper");
        wrapper.getChildren().add(frame);

        VBox box = new VBox();
        box.getStyleClass().add("video-frame-box");
        var name = new Label();
        name.getStyleClass().add("video-frame-name");
        name.textProperty().bind(user.name);
        box.getChildren().add(name);
        // For debugging
        //if(connection != null) {
        //    box.getChildren().add(new RTCDetails(connection));
        //}
        box.getChildren().add(wrapper);
        return box;
    }
}
