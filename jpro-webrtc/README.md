# JPro WebRTC

`jpro-webrtc` adds real-time audio/video communication to JPro applications by wrapping the
browser's [WebRTC](https://developer.mozilla.org/docs/Web/API/WebRTC_API) API in JavaFX-friendly
classes. It runs **in the browser only** (every class needs a JPro `WebAPI`).

## Dependency

```groovy
dependencies {
    implementation("one.jpro.platform:jpro-webrtc:0.7.2")
}
```

## What's inside

| Class | Purpose |
|---|---|
| `RTCPeerConnection` | A peer connection — offer/answer/ICE, observable state, received tracks. |
| `MediaStream` | The user's camera (`getCameraStream`) or screen (`getScreenStream`). |
| `VideoFrame` | A JavaFX node that displays a media stream (`setStream(...)`). |
| `RTCDetails` | A small debug view of a connection's live state. |

## Two peers on one page

The quickest way to see it work — connect two local peer connections directly (no signaling server),
each showing the other's camera:

```java
var rtc1 = new RTCPeerConnection(getWebAPI());
var rtc2 = new RTCPeerConnection(getWebAPI());

var video1 = new VideoFrame(getWebAPI());
var video2 = new VideoFrame(getWebAPI());

// show each peer's incoming track in the matching video frame
rtc1.getTracks().addListener((ListChangeListener<JSVariable>) c ->
        { while (c.next()) c.getAddedSubList().forEach(video1::setStream); });
rtc2.getTracks().addListener((ListChangeListener<JSVariable>) c ->
        { while (c.next()) c.getAddedSubList().forEach(video2::setStream); });

// add each camera, then connect the two peers
var f1 = rtc1.addStream(MediaStream.getCameraStream(getWebAPI()));
var f2 = rtc2.addStream(MediaStream.getCameraStream(getWebAPI()));
f1.thenCompose(a -> f2).thenRun(() -> RTCPeerConnection.connectConnections(rtc1, rtc2));
```

`connectConnections(...)` forwards ICE candidates both ways and renegotiates automatically — it's
for demos and tests within a single page.

## Real applications: signaling

Connecting two *different* browsers requires a **signaling channel** (a WebSocket, the routing
session, a backend, …) to exchange the offer/answer SDP and ICE candidates. `jpro-webrtc` gives you
the pieces; the transport is yours:

```java
// caller
String offer = rtc.createOfferAndSetLocal().get();   // send to the other peer via your channel
rtc.setOnNewIceCandidate(candidate -> /* send candidate over your channel */);

// on the other peer
rtc.setRemoteDescription(offer);
String answer = rtc.createAnswerAndSetLocal().get(); // send back
rtc.addIceCandidate(candidateFromPeer);              // for each received candidate
```

Observe `connectionStateProperty()`, `iceConnectionStateProperty()`, `iceGatheringStateProperty()`
and `signalingStateProperty()` to track progress (or drop an `RTCDetails` node into your scene).

## Running the example

```bash
./gradlew jpro-webrtc:example:jproRun
```

A multi-user video room (`VideoRoomApp`); `WebRTCSimple` shows the two-peers-on-one-page setup above.
