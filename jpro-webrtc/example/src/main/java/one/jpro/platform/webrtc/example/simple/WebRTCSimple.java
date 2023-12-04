package one.jpro.platform.webrtc.example.simple;

import com.jpro.webapi.JProApplication;
import com.jpro.webapi.JSVariable;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import one.jpro.platform.webrtc.MediaStream;
import one.jpro.platform.webrtc.RTCDetails;
import one.jpro.platform.webrtc.RTCPeerConnection;
import one.jpro.platform.webrtc.VideoFrame;

public class WebRTCSimple extends JProApplication {

    @Override
    public void start(Stage primaryStage) throws Exception {

        var pin = new VBox();

        //var rtc = new RTCPeerConnection(getWebAPI());
        var rtc1 = new RTCPeerConnection(getWebAPI());
        var rtc2 = new RTCPeerConnection(getWebAPI());

        var video1 = new VideoFrame(getWebAPI());
        var video2 = new VideoFrame(getWebAPI());

        rtc1.tracks.addListener((ListChangeListener<? super JSVariable>)  change -> {
            while(change.next()) {
                if(change.wasAdded()) {
                    video1.setStream(change.getAddedSubList().get(0));
                }
            }
        });
        rtc2.tracks.addListener((ListChangeListener<? super JSVariable>)  change -> {
            while(change.next()) {
                if(change.wasAdded()) {
                    video2.setStream(change.getAddedSubList().get(0));
                }
            }
        });

        var f1 = MediaStream.getCameraStream(rtc1.getWebAPI()).js.thenAccept(stream -> {
            rtc1.addStream(stream);
        });
        var f2 = MediaStream.getCameraStream(rtc2.getWebAPI()).js.thenAccept(stream -> {
            rtc2.addStream(stream);
        });

        (f1.thenCompose(a -> f2)).thenAccept( r ->
            RTCPeerConnection.connectConnections(rtc1, rtc2)
        );

        var WebRTCLabel = new Label("WebRTC");
        WebRTCLabel.setStyle("-fx-font-size: 20px;");
        pin.getChildren().add(WebRTCLabel);
        pin.getChildren().add(new RTCDetails(rtc1));
        pin.getChildren().add(video1);
        pin.getChildren().add(new Label(" ---------------- "));

        pin.getChildren().add(new RTCDetails(rtc2));
        pin.getChildren().add(video2);

        //pin.getChildren().add(createLabel("signalingState", rtc.tracks));

        primaryStage.setScene(new Scene(pin));

        primaryStage.show();
    }



}
