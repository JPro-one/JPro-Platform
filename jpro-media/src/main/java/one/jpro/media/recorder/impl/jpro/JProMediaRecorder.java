package one.jpro.media.recorder.impl.jpro;

import com.jpro.webapi.HTMLView;
import com.jpro.webapi.JSVariable;
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

/**
 * {@link MediaRecorder} implementation for the web.
 *
 * @author Besmir Beqiri
 */
public final class JProMediaRecorder extends BaseMediaRecorder {

    private static final String DEFAULT_MIME_TYPE = "video/webm";

    private final WebAPI webAPI;
    private final HTMLView cameraView;

    private final String videoId;

    /**
     * Creates a new MediaRecorder object.
     *
     * @param webAPI JPro WebAPI
     */
    public JProMediaRecorder(WebAPI webAPI) {
        this.webAPI = webAPI;
        webAPI.loadJSFile(getClass().getResource("/one/jpro/media/recorder/js/jpro-recorder.js"));

        videoId = webAPI.createUniqueJSName("video_recorder_");
        cameraView = new HTMLView("<video id=\"" + videoId + "\" autoplay muted></video>");
        cameraView.widthProperty().addListener(observable -> webAPI.executeScript(
                "let preview = document.getElementById(\"" + videoId + "\");\n" +
                        "preview.width=" + cameraView.getWidth() + ";"));
        cameraView.heightProperty().addListener(observable -> webAPI.executeScript(
                "let preview = document.getElementById(\"" + videoId + "\");\n" +
                        "preview.height=" + cameraView.getHeight() + ";"));

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
            // Update ObjectURL value
            setObjectURL(new JSVariable(webAPI, result, "URL.revokeObjectURL(" + result + ")"));

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
        return Boolean.getBoolean(webAPI.executeScriptWithReturn(
                "MediaRecorder.isTypeSupported(\"" + mimeType + "\")"));
    }

    private ReadOnlyObjectWrapper<JSVariable> objectURL;

    public JSVariable getObjectURL() {
        return (objectURL == null) ? null : objectURL.get();
    }

    private void setObjectURL(JSVariable value) {
        objectUrlPropertyImpl().set(value);
    }

    public ReadOnlyObjectProperty<JSVariable> objectUrlProperty() {
        return objectUrlPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<JSVariable> objectUrlPropertyImpl() {
        if (objectURL == null) {
            objectURL = new ReadOnlyObjectWrapper<>(this, "objectUrl") {

                @Override
                protected void invalidated() {
                    System.out.println("MediaRecorder ObjectURL: " + get().getName());
                }
            };
        }
        return objectURL;
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
        webAPI.executeScript("enableCamera(\"" + videoId + "\"," + mediaRecorderOptions.toJSON() + ");");
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
    public void download() {
        if (getObjectURL() != null) {
            System.out.println("Downloading ObjectUrl: " + getObjectURL().getName());
            webAPI.executeScript(
                    "let download_link = document.createElement(\"a\");\n" +
                            "download_link.setAttribute(\"download\", \"RecordedVideo.webm\");\n" +
                            "download_link.href = " + getObjectURL().getName() + ";\n" +
                            "download_link.click();");
        }
    }
}
