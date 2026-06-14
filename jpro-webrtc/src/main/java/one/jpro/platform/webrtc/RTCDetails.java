package one.jpro.platform.webrtc;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * A small debug view that shows the live state of an {@link RTCPeerConnection} — its connection,
 * ICE and signaling states, and the number of ICE candidates and tracks.
 */
public class RTCDetails extends VBox {

    public RTCDetails(RTCPeerConnection rtc) {
        getChildren().add(createLabel("connectionState", rtc.connectionStateProperty()));
        getChildren().add(createLabel("iceConnectionState", rtc.iceConnectionStateProperty()));
        getChildren().add(createLabel("iceGatheringState", rtc.iceGatheringStateProperty()));
        getChildren().add(createLabel("signalingState", rtc.signalingStateProperty()));

        getChildren().add(createSizeLabel("iceCandidates", rtc.getIceCandidates()));
        getChildren().add(createSizeLabel("trackCount", rtc.getTracks()));
    }

    private Label createLabel(String name, ReadOnlyStringProperty prop) {
        var label = new Label();
        label.textProperty().bind(Bindings.concat(name, ": ", prop));
        return label;
    }

    private <T> Label createSizeLabel(String name, ObservableList<T> list) {
        var label = new Label(name + ": " + list.size());
        list.addListener((ListChangeListener<? super T>) change -> label.setText(name + ": " + list.size()));
        label.setWrapText(true);
        return label;
    }
}
