package one.jpro.media.recorder.impl;

import com.jpro.webapi.HTMLView;
import com.jpro.webapi.WebAPI;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.Event;
import javafx.scene.layout.Region;
import one.jpro.media.MediaSource;
import one.jpro.media.event.MediaRecorderEvent;
import one.jpro.media.recorder.MediaRecorder;
import one.jpro.media.recorder.MediaRecorderException;
import org.json.JSONObject;

/**
 * {@link MediaRecorder} implementation for the web.
 *
 * @author Besmir Beqiri
 */
public final class WebMediaRecorder extends BaseMediaRecorder {

    private static final String DEFAULT_MIME_TYPE = "video/webm";

    private final WebAPI webAPI;
    private final HTMLView cameraView;

    private final String videoRecorderId;
    private final String mediaRecorderRef;
    private final String blobsRecordedRef;

    /**
     * Creates a new MediaRecorder object.
     *
     * @param webAPI JPro WebAPI
     */
    public WebMediaRecorder(WebAPI webAPI) {
        this.webAPI = webAPI;

        final String recorderId = webAPI.createUniqueJSName("recorder_");
        videoRecorderId = "video_" + recorderId;
        mediaRecorderRef = "media_" + recorderId;
        blobsRecordedRef = "blobs_" + recorderId;

        cameraView = new HTMLView("""
                <video id="%s" autoplay muted></video>
                """.formatted(videoRecorderId));

        cameraView.widthProperty().addListener(observable -> webAPI.executeScript("""
                        document.getElementById("%s").width = %s;
                        """.formatted(videoRecorderId, cameraView.getWidth())));
        cameraView.heightProperty().addListener(observable -> webAPI.executeScript("""
                        document.getElementById("%s").height = %s;
                        """.formatted(videoRecorderId, cameraView.getHeight())));

        webAPI.registerJavaFunction(mediaRecorderRef + "_onstart", result -> {
            // Set status
            Status.fromJS(result).ifPresent(this::setStatus);

            // Fire start event
            Event.fireEvent(WebMediaRecorder.this,
                    new MediaRecorderEvent(WebMediaRecorder.this,
                            MediaRecorderEvent.MEDIA_RECORDER_START));
        });

        webAPI.registerJavaFunction(mediaRecorderRef + "_onpause", result -> {
            // Set status
            Status.fromJS(result).ifPresent(this::setStatus);

            // Fire pause event
            Event.fireEvent(WebMediaRecorder.this,
                    new MediaRecorderEvent(WebMediaRecorder.this,
                            MediaRecorderEvent.MEDIA_RECORDER_PAUSE));
        });

        webAPI.registerJavaFunction(mediaRecorderRef + "_onresume", result -> {
            // Set status
            Status.fromJS(result).ifPresent(this::setStatus);

            // Fire resume event
            Event.fireEvent(WebMediaRecorder.this,
                    new MediaRecorderEvent(WebMediaRecorder.this,
                            MediaRecorderEvent.MEDIA_RECORDER_RESUME));
        });

        webAPI.registerJavaFunction(mediaRecorderRef + "_onstop", result -> {
            JSONObject json = new JSONObject(result
                    .substring(1, result.length() - 1)
                    .replace("\\", ""));
            final String objectUrl = json.getString("objectUrl");
            final long fileSize = json.getLong("fileSize");

            // Update ObjectURL value;
            setMediaSource(new MediaSource(webAPI.createJSFile(objectUrl, "RecordedVideo", fileSize)));

            // Set status to inactive
            setStatus(Status.INACTIVE);

            // Fire stop event
            Event.fireEvent(WebMediaRecorder.this,
                    new MediaRecorderEvent(WebMediaRecorder.this, MediaRecorderEvent.MEDIA_RECORDER_STOP));
        });

        webAPI.registerJavaFunction(mediaRecorderRef + "_ondataavailable", result -> {
            // Fire data available event
            Event.fireEvent(WebMediaRecorder.this,
                    new MediaRecorderEvent(WebMediaRecorder.this,
                            MediaRecorderEvent.MEDIA_RECORDER_DATA_AVAILABLE));
        });

        webAPI.registerJavaFunction(mediaRecorderRef + "_onerror", result -> {
            // Reset status to inactive
            setStatus(Status.INACTIVE);

            // Set error
            setError(MediaRecorderException.fromJSON(result));

            // Fire error event
            Event.fireEvent(WebMediaRecorder.this,
                    new MediaRecorderEvent(WebMediaRecorder.this,
                            MediaRecorderEvent.MEDIA_RECORDER_ERROR));
        });
    }

    public Region getCameraView() {
        return cameraView;
    }

    /**
     * Returns a boolean which is <code>true</code> if the MIME type specified is one that this media recorder should
     * be able to successfully record.
     *
     * @param mimeType the MIME media type to check
     * @return <code>true</code> if this MediaRecorder implementation is capable of recording Blob objects for the
     * specified MIME type. Recording may still fail if there are insufficient resources to support the recording
     * and encoding process. If the value is <code>false</code>, the user agent is incapable of recording the
     * specified format.
     * @throws Exception
     */
    public boolean isTypeSupported(String mimeType) throws Exception {
        return Boolean.getBoolean(webAPI.executeScriptWithReturn("""
                        MediaRecorder.isTypeSupported("%s")
                        """.formatted(mimeType)));
    }

    @Override
    ReadOnlyObjectWrapper<MediaSource> mediaResourcePropertyImpl() {
        if (mediaSource == null) {
            mediaSource = new ReadOnlyObjectWrapper<>(this, "mediaSource") {

                @Override
                protected void invalidated() {
                    final var mediaSource = get();
                    if (!mediaSource.isLocal()) {
                        final var objectUrl = mediaSource.jsFile().getObjectURL();
                        System.out.println("MediaRecorder ObjectURL: " + objectUrl.getName());
                    }
                }
            };
        }
        return mediaSource;
    }

    // mimeType property (read-only)
    private ReadOnlyStringWrapper mimeType;

    public String getMimeType() {
        return (mimeType == null) ? DEFAULT_MIME_TYPE : mimeType.get();
    }

    private void setMimeType(String value) {
        mimeTypePropertyImpl().set(value);
    }

    public ReadOnlyStringProperty mimeTypeProperty() {
        return mimeTypePropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyStringWrapper mimeTypePropertyImpl() {
        if (mimeType == null) {
            mimeType = new ReadOnlyStringWrapper(this, "mimeType", DEFAULT_MIME_TYPE);
        }
        return mimeType;
    }

    // Recorder controller methods
    @Override
    public void enable() {
        final var mediaRecorderOptions = new MediaRecorderOptions().mimeType(getMimeType());
        webAPI.executeScript("""
                $blobsRecorded = []; // stream buffer
                var elem = document.getElementById("$videoRecorderId");
                navigator.mediaDevices.getUserMedia({ video: true, audio: true })
                    .then((stream) => {
                        elem.srcObject = stream;
                        $mediaRecorder = new MediaRecorder(elem.srcObject, $videoRecorderOptions);
                        
                        // event : new recorded video blob available
                        $mediaRecorder.addEventListener('dataavailable', function(e) {
                            $blobsRecorded.push(e.data);
                            jpro.$mediaRecorder_ondataavailable(e.timecode);
                        });
                        
                        // event : recording stopped & all blobs sent
                        $mediaRecorder.onstop = (event) => {
                            // create local object URL from the recorded video blobs
                            let recordedBlob = new Blob($blobsRecorded, { type: "video/webm" });
                            let videoUrl = URL.createObjectURL(recordedBlob);
                            // pas object url and file size as json format
                            jpro.$mediaRecorder_onstop(JSON.stringify({
                                objectUrl: videoUrl,
                                fileSize: recordedBlob.size
                            }));
                        }
                    
                        $mediaRecorder.onerror = (event) => {
                            // pas error type and message as json format
                            jpro.$mediaRecorder_onerror(JSON.stringify({
                                type: event.error.code,
                                message: event.error.message
                            }));
                        }
                                                                               
                        $mediaRecorder.onstart = (event) => jpro.$mediaRecorder_onstart($mediaRecorder.state)
                        $mediaRecorder.onpause = (event) => jpro.$mediaRecorder_onpause($mediaRecorder.state)
                        $mediaRecorder.onresume = (event) => jpro.$mediaRecorder_onresume($mediaRecorder.state)
                    });
                """
                .replace("$videoRecorderId", videoRecorderId)
                .replace("$videoRecorderOptions", mediaRecorderOptions.toJSON().toString())
                .replace("$blobsRecorded", blobsRecordedRef)
                .replace("$mediaRecorder", mediaRecorderRef));
    }

    @Override
    public void start() {
        webAPI.executeScript("""
                $blobsRecorded = []; // clear recorded buffer
                $mediaRecorder.start(1000); // start recording with a timeslice of 1 second
                """
                .replace("$blobsRecorded", blobsRecordedRef)
                .replace("$mediaRecorder", mediaRecorderRef));
    }

    @Override
    public void pause() {
        webAPI.executeScript("""
                if ($mediaRecorder.state === "recording") {
                    $mediaRecorder.pause();
                }
                """.replace("$mediaRecorder", mediaRecorderRef));
    }

    @Override
    public void resume() {
        webAPI.executeScript("""
                if ($mediaRecorder.state === "paused") {
                    $mediaRecorder.resume();
                }
                """.replace("$mediaRecorder", mediaRecorderRef));
    }

    @Override
    public void stop() {
        webAPI.executeScript("""
                $mediaRecorder.stop();
                """.replace("$mediaRecorder", mediaRecorderRef));
    }

    @Override
    @Deprecated
    public void retrieve() {
        final var mediaSource = getMediaSource();
        if (!mediaSource.isLocal()) {
            final WebAPI.JSFile jsFile = mediaSource.jsFile();
            if (jsFile != null) {
                webAPI.executeScript("""
                    let download_link = document.createElement("a");
                    download_link.setAttribute("download", "RecordedVideo.webm");
                    download_link.href = %s;
                    download_link.click();
                    """.formatted(jsFile.getObjectURL().getName()));
            }
        }
    }
}
