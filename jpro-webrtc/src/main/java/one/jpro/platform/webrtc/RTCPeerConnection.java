package one.jpro.platform.webrtc;

import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class RTCPeerConnection {

    private static String defaultConf = "{ iceServers: [{ urls: 'stun:stun.l.google.com:19302' }] }";

    public StringProperty connectionState = new SimpleStringProperty("new");

    public StringProperty iceConnectionState = new SimpleStringProperty("new");

    public StringProperty iceGatheringState = new SimpleStringProperty("new");

    public StringProperty signalingState = new SimpleStringProperty("stable");

    private WebAPI webAPI;
    private Set<Object> hardReferences = new HashSet<>();

    public ObservableList<JSVariable> tracks = FXCollections.observableArrayList();
    public ObservableList<String> iceCandidates = FXCollections.observableArrayList();

    public Consumer<String> onNewIceCandidate = str -> {};

    public Runnable onnegotiationneeded = () -> {};

    JSVariable js;

    public RTCPeerConnection(WebAPI webAPI) {
        this(webAPI, defaultConf);
    }

    public RTCPeerConnection(WebAPI webAPI, String conf) {
        this.webAPI = webAPI;
        js = webAPI.executeScriptWithVariable("new RTCPeerConnection(" + conf + ");");

        listenToProperty("connectionState", connectionState::set);
        listenToProperty("iceConnectionState", iceConnectionState::set);
        listenToProperty("iceGatheringState", iceGatheringState::set);
        listenToProperty("signalingState", signalingState::set);

        listenToJS("track", event -> {
            JSVariable track = webAPI.executeScriptWithVariable(event.getName() + ".streams[0];");
            tracks.add(track);
        });

        listenToJS("icecandidate", iceCandidate -> {
            //System.out.println("TO EXECUTE: " + "" + iceCandidate.getName() + ".candidate");
            webAPI.executeScriptWithFuture("" + iceCandidate.getName() + ".candidate").thenAccept(str -> {
                iceCandidates.add(str);
                onNewIceCandidate.accept(str);
            });
        });

        listenToJS("negotiationneeded", e -> {
            System.out.println("negotiationneeded: " + "" + e.getName());
            onnegotiationneeded.run();
        });
    }

    public WebAPI getWebAPI() {
        return webAPI;
    }

    public void listenToJS(String propName, Consumer<JSVariable> setter) {
        var jsFun = webAPI.registerJavaFunctionWithVariable(setter);
        webAPI.executeScript(js.getName() + ".on" + propName + " = function(str){ console.log('str: ' + str); " + jsFun.getName() + "(str);};");
        hardReferences.add(jsFun);
    }

    public void listenToProperty(String propName, Consumer<String> setter) {
        var jsFun = webAPI.registerJavaFunction(str -> {
            // remove first and last character
            str = str.substring(1, str.length() - 1);
            setter.accept(str);
        });
        webAPI.executeScript(js.getName() + ".on" + propName + "change = function(str){" + jsFun.getName() + "(str);};" +
          jsFun.getName() + "(" + js.getName() + "." + propName + ");");
        hardReferences.add(jsFun);
    }

    public CompletableFuture<String> createOfferAndSetLocal() {
        return webAPI.executeJSAsync("const offer = await "+js.getName()+".createOffer();" +
          "await "+js.getName()+".setLocalDescription(offer);" +
          "return offer;").thenCompose(js -> {
            return webAPI.executeScriptWithFuture(js.getName());});
    }

    public CompletableFuture<String> createAnswerAndSetLocal() {
        return webAPI.executeJSAsync("const answer = await "+js.getName()+".createAnswer();" +
          "await "+js.getName()+".setLocalDescription(answer);" +
          "return answer;").thenCompose(js -> {
            return webAPI.executeScriptWithFuture(js.getName());});
    }

    public CompletableFuture<JSVariable> setLocalDescription(String sdp) {
        return webAPI.executeJSAsync("return await " + js.getName() + ".setLocalDescription(" + sdp + ");");
    }

    public CompletableFuture<JSVariable> setRemoteDescription(String sdp) {
        return webAPI.executeJSAsync("return await " + js.getName() + ".setRemoteDescription(new RTCSessionDescription(" + sdp + "));");
    }

    public CompletableFuture<String> addStream(JSVariable stream) {
        return webAPI.executeScriptWithFuture(stream.getName() + ".getTracks().forEach(function(track) {" +
          js.getName() + ".addTrack(track, " + stream.getName() + ");" +
          "}); undefined;");
    }

    public CompletableFuture<JSVariable> removeStream(MediaStream stream) {
        return stream.js.thenApply(s -> {
            return webAPI.executeScriptWithVariable(js.getName() + ".getSenders().forEach(sender => {" +
              "if (sender.track) {" +
              "sender.track.stop();" +
              "}" +
              js.getName() + ".removeTrack(sender);" +
              "});");
        });
    }

    //public void removeAllTracks() {
    //    webAPI.executeScript(js.getName() + ".getSenders().forEach(sender => {" +
    //      "if (sender.track) {" +
    //      "sender.track.stop();" +
    //      "}" +
    //      js.getName() + ".removeTrack(sender);" +
    //      "});");
    //}

    public void addTrack(JSVariable track) {
        webAPI.executeScript(js.getName() + ".addTrack(" + track.getName() + ");");
    }

    public void removeTracks(JSVariable stream) {
        webAPI.executeScript(js.getName() + ".removeTrack(" + stream.getName() + ");");
    }

    /**
     * Sets the remote ICE candidates.
     */
    public void addIceCandidate(String iceCandidate) {
        if(iceCandidate == null) {
            throw new IllegalArgumentException("iceCandidate must not be null");
        }
        if(iceCandidate.equals("null")) {
            throw new IllegalArgumentException("iceCandidate was String 'null'");
        }
        webAPI.executeScript(js.getName() + ".addIceCandidate(new RTCIceCandidate(" + iceCandidate + "));");
    }

    public static void connectConnections(RTCPeerConnection rtc1, RTCPeerConnection rtc2) {

        // Link ice candidates
        rtc1.iceCandidates.stream().forEach(rtc2::addIceCandidate);
        rtc2.iceCandidates.stream().forEach(rtc1::addIceCandidate);
        rtc1.onNewIceCandidate = (rtc2::addIceCandidate);
        rtc2.onNewIceCandidate = (rtc1::addIceCandidate);

        AtomicBoolean isNegotiating = new AtomicBoolean(false);

        //rtc1.onnegotiationneeded = () -> {
        //    negotiate(rtc1, rtc2);
        //};
        //rtc2.onnegotiationneeded = () -> {
        //    negotiate(rtc2, rtc1);
        //};

        rtc1.onnegotiationneeded = () -> {
            if(!isNegotiating.get()) {
                isNegotiating.set(true);
                negotiate(rtc1, rtc2).thenRun(() -> {
                    isNegotiating.set(false);
                });
            }
        };
        rtc2.onnegotiationneeded = () -> {
            if(!isNegotiating.get()) {
                isNegotiating.set(true);
                negotiate(rtc2, rtc1).thenRun(() -> {
                    isNegotiating.set(false);
                });
            }
        };
        isNegotiating.set(true);
        negotiate(rtc1, rtc2).thenRun(() -> {
            isNegotiating.set(false);
        });
    }

    public static CompletableFuture<JSVariable> negotiate(RTCPeerConnection rtc1, RTCPeerConnection rtc2) {

        return rtc1.createOfferAndSetLocal().thenCompose(sdp -> {
            //System.out.println("OFFER: " + sdp);
            return rtc2.setRemoteDescription(sdp).thenCompose(s -> {
                return rtc2.createAnswerAndSetLocal().thenCompose(sdp2 -> {
                    //System.out.println("ANSWER: " + sdp2);
                    return rtc1.setRemoteDescription(sdp2);
                });
            });
        });
    }
}
