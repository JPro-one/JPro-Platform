package one.jpro.platform.webrtc;

import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * A JavaFX-friendly wrapper around the browser's
 * <a href="https://developer.mozilla.org/docs/Web/API/RTCPeerConnection">{@code RTCPeerConnection}</a>.
 *
 * <p>It exposes the connection state as observable properties, the received tracks and gathered ICE
 * candidates as observable lists, and the offer/answer/ICE operations as {@link CompletableFuture}s.
 * Runs in the browser only (it needs a JPro {@link WebAPI}). For a local two-peer setup, see
 * {@link #connectConnections(RTCPeerConnection, RTCPeerConnection)}.</p>
 */
public class RTCPeerConnection {

    private static final String defaultConf = "{ iceServers: [{ urls: 'stun:stun.l.google.com:19302' }] }";

    private final ReadOnlyStringWrapper connectionState = new ReadOnlyStringWrapper("new");
    private final ReadOnlyStringWrapper iceConnectionState = new ReadOnlyStringWrapper("new");
    private final ReadOnlyStringWrapper iceGatheringState = new ReadOnlyStringWrapper("new");
    private final ReadOnlyStringWrapper signalingState = new ReadOnlyStringWrapper("stable");

    private final ObservableList<JSVariable> tracks = FXCollections.observableArrayList();
    private final ObservableList<JSVariable> tracksView = FXCollections.unmodifiableObservableList(tracks);
    private final ObservableList<String> iceCandidates = FXCollections.observableArrayList();
    private final ObservableList<String> iceCandidatesView = FXCollections.unmodifiableObservableList(iceCandidates);

    private Consumer<String> onNewIceCandidate = str -> {};
    private Runnable onNegotiationNeeded = () -> {};

    private final WebAPI webAPI;
    private final Set<Object> hardReferences = new HashSet<>();
    private final JSVariable js;

    /** Creates a peer connection with a default STUN configuration. */
    public RTCPeerConnection(WebAPI webAPI) {
        this(webAPI, defaultConf);
    }

    /**
     * Creates a peer connection with a custom configuration.
     *
     * @param conf the JSON {@code RTCConfiguration} (e.g. {@code "{ iceServers: [...] }"})
     */
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

        listenToJS("icecandidate", iceCandidate ->
            webAPI.executeScriptWithFuture(iceCandidate.getName() + ".candidate").thenAccept(str -> {
                iceCandidates.add(str);
                onNewIceCandidate.accept(str);
            }));

        listenToJS("negotiationneeded", e -> onNegotiationNeeded.run());
    }

    /** Returns the {@link WebAPI} this connection runs on. */
    public WebAPI getWebAPI() {
        return webAPI;
    }

    /** The connection state ({@code new}, {@code connecting}, {@code connected}, …). */
    public ReadOnlyStringProperty connectionStateProperty() {
        return connectionState.getReadOnlyProperty();
    }

    public String getConnectionState() {
        return connectionState.get();
    }

    /** The ICE connection state. */
    public ReadOnlyStringProperty iceConnectionStateProperty() {
        return iceConnectionState.getReadOnlyProperty();
    }

    public String getIceConnectionState() {
        return iceConnectionState.get();
    }

    /** The ICE gathering state. */
    public ReadOnlyStringProperty iceGatheringStateProperty() {
        return iceGatheringState.getReadOnlyProperty();
    }

    public String getIceGatheringState() {
        return iceGatheringState.get();
    }

    /** The signaling state. */
    public ReadOnlyStringProperty signalingStateProperty() {
        return signalingState.getReadOnlyProperty();
    }

    public String getSignalingState() {
        return signalingState.get();
    }

    /** The remote media tracks received on this connection (read-only, observable). */
    public ObservableList<JSVariable> getTracks() {
        return tracksView;
    }

    /** The local ICE candidates gathered for this connection (read-only, observable). */
    public ObservableList<String> getIceCandidates() {
        return iceCandidatesView;
    }

    /** Sets the callback invoked for each new local ICE candidate (forward it to the remote peer). */
    public void setOnNewIceCandidate(Consumer<String> onNewIceCandidate) {
        this.onNewIceCandidate = onNewIceCandidate;
    }

    /** Sets the callback invoked when (re)negotiation is needed. */
    public void setOnNegotiationNeeded(Runnable onNegotiationNeeded) {
        this.onNegotiationNeeded = onNegotiationNeeded;
    }

    private void listenToJS(String propName, Consumer<JSVariable> setter) {
        var jsFun = webAPI.registerJavaFunctionWithVariable(setter);
        webAPI.executeScript(js.getName() + ".on" + propName + " = function(str){ " + jsFun.getName() + "(str);};");
        hardReferences.add(jsFun);
    }

    private void listenToProperty(String propName, Consumer<String> setter) {
        var jsFun = webAPI.registerJavaFunction(str -> {
            // strip the surrounding quotes added by the JS bridge
            str = str.substring(1, str.length() - 1);
            setter.accept(str);
        });
        webAPI.executeScript(js.getName() + ".on" + propName + "change = function(str){" + jsFun.getName() + "(str);};" +
          jsFun.getName() + "(" + js.getName() + "." + propName + ");");
        hardReferences.add(jsFun);
    }

    /** Creates an offer and sets it as the local description; resolves with the offer SDP. */
    public CompletableFuture<String> createOfferAndSetLocal() {
        return webAPI.executeJSAsync("const offer = await "+js.getName()+".createOffer();" +
          "await "+js.getName()+".setLocalDescription(offer);" +
          "return offer;").thenCompose(js -> webAPI.executeScriptWithFuture(js.getName()));
    }

    /** Creates an answer and sets it as the local description; resolves with the answer SDP. */
    public CompletableFuture<String> createAnswerAndSetLocal() {
        return webAPI.executeJSAsync("const answer = await "+js.getName()+".createAnswer();" +
          "await "+js.getName()+".setLocalDescription(answer);" +
          "return answer;").thenCompose(js -> webAPI.executeScriptWithFuture(js.getName()));
    }

    /** Sets the local session description from the given SDP. */
    public CompletableFuture<JSVariable> setLocalDescription(String sdp) {
        return webAPI.executeJSAsync("return await " + js.getName() + ".setLocalDescription(" + sdp + ");");
    }

    /** Sets the remote session description from the given SDP. */
    public CompletableFuture<JSVariable> setRemoteDescription(String sdp) {
        return webAPI.executeJSAsync("return await " + js.getName() + ".setRemoteDescription(new RTCSessionDescription(" + sdp + "));");
    }

    /** Adds all tracks of the given media stream to this connection. */
    public CompletableFuture<String> addStream(MediaStream stream) {
        return stream.js().thenCompose(this::addStream);
    }

    /** Adds all tracks of the given (already resolved) media stream to this connection. */
    public CompletableFuture<String> addStream(JSVariable stream) {
        return webAPI.executeScriptWithFuture(stream.getName() + ".getTracks().forEach(function(track) {" +
          js.getName() + ".addTrack(track, " + stream.getName() + ");" +
          "}); undefined;");
    }

    /** Stops and removes all senders' tracks for the given stream. */
    public CompletableFuture<JSVariable> removeStream(MediaStream stream) {
        return stream.js().thenApply(s ->
            webAPI.executeScriptWithVariable(js.getName() + ".getSenders().forEach(sender => {" +
              "if (sender.track) {" +
              "sender.track.stop();" +
              "}" +
              js.getName() + ".removeTrack(sender);" +
              "});"));
    }

    /** Adds a single track to this connection. */
    public void addTrack(JSVariable track) {
        webAPI.executeScript(js.getName() + ".addTrack(" + track.getName() + ");");
    }

    /** Removes a single track sender from this connection. */
    public void removeTrack(JSVariable track) {
        webAPI.executeScript(js.getName() + ".removeTrack(" + track.getName() + ");");
    }

    /**
     * Adds a remote ICE candidate received from the other peer.
     *
     * @param iceCandidate the candidate JSON; must not be {@code null} or the string {@code "null"}
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

    /**
     * Wires two local peer connections directly together: forwards ICE candidates both ways and
     * (re)negotiates whenever needed. Useful for demos and tests within a single page.
     */
    public static void connectConnections(RTCPeerConnection rtc1, RTCPeerConnection rtc2) {
        // Link ICE candidates both ways
        rtc1.iceCandidates.forEach(rtc2::addIceCandidate);
        rtc2.iceCandidates.forEach(rtc1::addIceCandidate);
        rtc1.onNewIceCandidate = rtc2::addIceCandidate;
        rtc2.onNewIceCandidate = rtc1::addIceCandidate;

        AtomicBoolean isNegotiating = new AtomicBoolean(false);
        rtc1.onNegotiationNeeded = () -> {
            if(!isNegotiating.get()) {
                isNegotiating.set(true);
                negotiate(rtc1, rtc2).thenRun(() -> isNegotiating.set(false));
            }
        };
        rtc2.onNegotiationNeeded = () -> {
            if(!isNegotiating.get()) {
                isNegotiating.set(true);
                negotiate(rtc2, rtc1).thenRun(() -> isNegotiating.set(false));
            }
        };
        isNegotiating.set(true);
        negotiate(rtc1, rtc2).thenRun(() -> isNegotiating.set(false));
    }

    /** Runs one offer/answer negotiation round from {@code rtc1} to {@code rtc2}. */
    public static CompletableFuture<JSVariable> negotiate(RTCPeerConnection rtc1, RTCPeerConnection rtc2) {
        return rtc1.createOfferAndSetLocal().thenCompose(sdp ->
            rtc2.setRemoteDescription(sdp).thenCompose(s ->
                rtc2.createAnswerAndSetLocal().thenCompose(sdp2 ->
                    rtc1.setRemoteDescription(sdp2))));
    }
}
