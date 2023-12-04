package one.jpro.platform.webrtc.example.videoroom.model;

import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import one.jpro.platform.webrtc.MediaStream;
import one.jpro.platform.webrtc.RTCPeerConnection;
import one.jpro.platform.webrtc.VideoFrame;

public class VideoRoom {
    static ObservableList<VideoRoom> rooms = FXCollections.observableArrayList();

    public String id;
    ObservableList<User> users = FXCollections.observableArrayList();

    public VideoRoom(String id) {
        this.id = id;
    }


    public static VideoRoom getOrCreateRoom(String id) {
        for(VideoRoom room : rooms) {
            if(room.id.equals(id)) {
                return room;
            }
        }
        VideoRoom room = new VideoRoom(id);
        rooms.add(room);
        return room;
    }

    public void addUserAndCreateConnections(User user) {
        for(User otherUser : users) {
            createWebRTCUserVideo(user, otherUser);
        }
        users.add(user);
    }

    public static void createWebRTCUserVideo(User user1, User user2) {
        var webAPI1 = user1.webAPI;
        var webAPI2 = user2.webAPI;

        var rtc1 = new RTCPeerConnection(webAPI1);
        var rtc2 = new RTCPeerConnection(webAPI2);

        var video1 = new VideoFrame(webAPI1);
        var video2 = new VideoFrame(webAPI2);

        rtc1.tracks.addListener((ListChangeListener<? super JSVariable>) change -> {
            System.out.println("tracks size for user 1: " + rtc1.tracks.size());
            while(change.next()) {
                if(change.wasAdded()) {
                    video1.setStream(change.getAddedSubList().get(0));
                }
            }
        });
        rtc2.tracks.addListener((ListChangeListener<? super JSVariable>)  change -> {
            System.out.println("tracks size for user 2: " + rtc2.tracks.size());
            while(change.next()) {
                if(change.wasAdded()) {
                    video2.setStream(change.getAddedSubList().get(0));
                }
            }
        });

        user1.mediaStream.addListener((observable, oldValue, newValue) -> {
            try {
                //rtc1.removeAllTracks();
                rtc1.removeStream(oldValue);
                newValue.js.thenAccept(stream -> {
                    rtc1.addStream(stream);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        user2.mediaStream.addListener((observable, oldValue, newValue) -> {
            try {
                //rtc2.removeAllTracks();
                rtc2.removeStream(oldValue);
                newValue.js.thenAccept(stream -> {
                    rtc2.addStream(stream);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        var f1 = user1.mediaStream.get().js.thenAccept(stream -> {
            rtc1.addStream(stream);
        });
        var f2 = user2.mediaStream.get().js.thenAccept(stream -> {
            rtc2.addStream(stream);
        });

        (f1.thenCompose(a -> f2)).thenAccept( r ->
            RTCPeerConnection.connectConnections(rtc1, rtc2)
        );

        user1.userVideos.add(new UserVideo(user2, video1, rtc1));

        user2.userVideos.add(new UserVideo(user1, video2, rtc2));
    }

    public static ObservableList<VideoRoom> getRooms() {
        return rooms;
    }


    /**
     * A user in a room
     */
    public static class User {

        public User(String name, MediaStream mediaStream, WebAPI webAPI) {
            this.name.set(name);
            this.mediaStream = new SimpleObjectProperty<>(mediaStream);
            this.webAPI = webAPI;
        }

        public WebAPI webAPI;
        public StringProperty name = new SimpleStringProperty("User");

        public SimpleObjectProperty<MediaStream> mediaStream;

        public ObservableList<UserVideo> userVideos = FXCollections.observableArrayList();
    }

    /**
     * The video of a user in a room accessed by a specific user
     */
    public static class UserVideo {

        UserVideo (User user, VideoFrame videoFrame, RTCPeerConnection connection) {
            this.user = user;
            this.videoFrame = videoFrame;
            this.connection = connection;
        }

        public User user;
        public VideoFrame videoFrame;
        public RTCPeerConnection connection;
    }
}
