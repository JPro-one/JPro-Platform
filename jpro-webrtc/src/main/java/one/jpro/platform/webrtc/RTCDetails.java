package one.jpro.platform.webrtc;

import javafx.beans.binding.Bindings;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * This class shows various details of an RTCPeerConnection.
 */
public class RTCDetails extends VBox {

    private RTCPeerConnection rtc;
    public RTCDetails(RTCPeerConnection rtc) {
        this.rtc = rtc;


        getChildren().add(createLabel("connectionState", rtc.connectionState));
        getChildren().add(createLabel("iceConnectionState", rtc.iceConnectionState));
        getChildren().add(createLabel("iceGatheringState", rtc.iceGatheringState));
        getChildren().add(createLabel("signalingState", rtc.signalingState));

        getChildren().add(createSizeLabel("iceCandidates", rtc.iceCandidates));
        getChildren().add(createSizeLabel("trackCount", rtc.tracks));
    }

    private Label createLabel(String name, StringProperty prop) {
        var label = new Label();
        label.textProperty().bind(Bindings.concat(name, ": ", prop));
        return label;
    }

    private <T> Label createSizeLabel(String name, ObservableList<T> list) {
        var label = new Label();
        //label.textProperty().bind(Bindings.concat(name, ": ", Bindings.size(list)));
        // put in the whole list
        //label.textProperty().bind(Bindings.concat(name, ": ", list));
        // but add listener for List
        list.addListener((ListChangeListener<? super T>) change -> {
            while(change.next()) {
                if(change.wasAdded()) {
                    label.setText(name + ": " + list.size());
                }
            }
        });
        label.wrapTextProperty().setValue(true);
        return label;
    }

}
