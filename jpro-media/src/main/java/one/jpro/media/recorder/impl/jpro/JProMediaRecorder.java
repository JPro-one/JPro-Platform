package one.jpro.media.recorder.impl.jpro;

import com.jpro.webapi.HTMLView;
import com.jpro.webapi.WebAPI;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.Event;
import javafx.scene.layout.Region;
import one.jpro.media.recorder.MediaRecorder;
import one.jpro.media.recorder.MediaRecorderException;
import one.jpro.media.recorder.MediaRecorderOptions;
import one.jpro.media.recorder.event.MediaRecorderEvent;
import one.jpro.media.recorder.impl.BaseMediaRecorder;
import org.json.JSONObject;

/**
 * {@link MediaRecorder} implementation for the web.
 *
 * @author Besmir Beqiri
 */
public final class JProMediaRecorder extends BaseMediaRecorder {

    private static final String DEFAULT_MIME_TYPE = "video/webm";

    private final WebAPI webAPI;
    private final HTMLView cameraView;

    private final String videoRecorderId;

    /**
     * Creates a new MediaRecorder object.
     *
     * @param webAPI JPro WebAPI
     */
    public JProMediaRecorder(WebAPI webAPI) {
        this.webAPI = webAPI;
        webAPI.loadJSFile(getClass().getResource("/one/jpro/media/recorder/jpro-recorder.js"));

        videoRecorderId = webAPI.createUniqueJSName("video_recorder_");
        cameraView = new HTMLView("""
                <video id="%s" autoplay muted></video>
                """.formatted(videoRecorderId));

        cameraView.widthProperty().addListener(observable -> webAPI.executeScript("""
                        let elem = document.getElementById("%s");
                        elem.width = %s;
                        """.formatted(videoRecorderId, cameraView.getWidth())));
        cameraView.heightProperty().addListener(observable -> webAPI.executeScript("""
                        let elem = document.getElementById("%s");
                        elem.height = %s;
                        """.formatted(videoRecorderId, cameraView.getHeight())));

        webAPI.registerJavaFunction("mediaRecorderOnStart", result -> {
            // Set state
            State.fromJS(result).ifPresent(this::setState);

            // Fire start event
            Event.fireEvent(JProMediaRecorder.this,
                    new MediaRecorderEvent(JProMediaRecorder.this,
                            MediaRecorderEvent.MEDIA_RECORDER_START));
        });

        webAPI.registerJavaFunction("mediaRecorderOnPause", result -> {
            // Set state
            State.fromJS(result).ifPresent(this::setState);

            // Fire pause event
            Event.fireEvent(JProMediaRecorder.this,
                    new MediaRecorderEvent(JProMediaRecorder.this,
                            MediaRecorderEvent.MEDIA_RECORDER_PAUSE));
        });

        webAPI.registerJavaFunction("mediaRecorderOnResume", result -> {
            // Set state
            State.fromJS(result).ifPresent(this::setState);

            // Fire resume event
            Event.fireEvent(JProMediaRecorder.this,
                    new MediaRecorderEvent(JProMediaRecorder.this,
                            MediaRecorderEvent.MEDIA_RECORDER_RESUME));
        });

        webAPI.registerJavaFunction("mediaRecorderOnStop", result -> {
            JSONObject json = new JSONObject(result
                    .substring(1, result.length() - 1)
                    .replace("\\", ""));
            final String objectUrl = json.getString("objectUrl");
            final long fileSize = json.getLong("fileSize");

            // Update ObjectURL value
            setJSFile(webAPI.createJSFile(objectUrl, "RecordedVideo", fileSize));

            // Set state to inactive
            setState(State.INACTIVE);

            // Fire stop event
            Event.fireEvent(JProMediaRecorder.this,
                    new MediaRecorderEvent(JProMediaRecorder.this, MediaRecorderEvent.MEDIA_RECORDER_STOP));
        });

        webAPI.registerJavaFunction("mediaRecorderOnDataavailable", result -> {
            // Fire data available event
            Event.fireEvent(JProMediaRecorder.this,
                    new MediaRecorderEvent(JProMediaRecorder.this,
                            MediaRecorderEvent.MEDIA_RECORDER_DATA_AVAILABLE));
        });

        webAPI.registerJavaFunction("mediaRecorderOnError", result -> {
            // Reset state to inactive
            setState(State.INACTIVE);

            // Set error
            setError(MediaRecorderException.fromJSON(result));

            // Fire error event
            Event.fireEvent(JProMediaRecorder.this,
                    new MediaRecorderEvent(JProMediaRecorder.this,
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

    private ReadOnlyObjectWrapper<WebAPI.JSFile> jsFile;

    public WebAPI.JSFile getJsFile() {
        return (jsFile == null) ? null : jsFile.get();
    }

    private void setJSFile(WebAPI.JSFile value) {
        objectUrlPropertyImpl().set(value);
    }

    public ReadOnlyObjectProperty<WebAPI.JSFile> jsFileProperty() {
        return objectUrlPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<WebAPI.JSFile> objectUrlPropertyImpl() {
        if (jsFile == null) {
            jsFile = new ReadOnlyObjectWrapper<>(this, "jsFile") {

                @Override
                protected void invalidated() {
                    System.out.println("MediaRecorder ObjectURL: " + get().getObjectURL().getName());
                }
            };
        }
        return jsFile;
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
                        enableCamera("%s", %s);
                        """.formatted(videoRecorderId, mediaRecorderOptions.toJSON()));
    }

    @Override
    public void start() {
        webAPI.executeScript("startRecording();");
    }

    @Override
    public void pause() {
        webAPI.executeScript("pauseRecording();");
    }

    @Override
    public void resume() {
        webAPI.executeScript("resumeRecording();");
    }

    @Override
    public void stop() {
        webAPI.executeScript("stopRecording();");
    }

    @Override
    public void retrieve() {
        if (getJsFile() != null) {
            webAPI.executeScript("""
                    let download_link = document.createElement("a");
                    download_link.setAttribute("download", "RecordedVideo.webm");
                    download_link.href = %s;
                    download_link.click();
                    """.formatted(getJsFile().getObjectURL().getName()));
        }
    }
}
