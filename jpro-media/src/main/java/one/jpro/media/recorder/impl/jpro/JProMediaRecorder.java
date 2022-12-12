package one.jpro.media.recorder.impl.jpro;

import com.jpro.webapi.HTMLView;
import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;
import com.sun.javafx.event.EventHandlerManager;
import javafx.beans.property.*;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import one.jpro.media.recorder.MediaRecorder;
import one.jpro.media.recorder.MediaRecorderException;
import one.jpro.media.recorder.MediaRecorderOptions;
import one.jpro.media.recorder.event.MediaRecorderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Media recorder interface implementation.
 *
 * @author Besmir Beqiri
 */
public final class JProMediaRecorder implements MediaRecorder {

    private final Logger log = LoggerFactory.getLogger(JProMediaRecorder.class);

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
            // Set state to recording
            setState(State.RECORDING);

            // Fire start event
            Event.fireEvent(JProMediaRecorder.this,
                    new MediaRecorderEvent(JProMediaRecorder.this,
                            MediaRecorderEvent.MEDIA_RECORDER_START));
        });

        webAPI.registerJavaFunction("mediaRecorderOnPause", result -> {
            // Set state to paused
            setState(State.PAUSED);

            // Fire start event
            Event.fireEvent(JProMediaRecorder.this,
                    new MediaRecorderEvent(JProMediaRecorder.this,
                            MediaRecorderEvent.MEDIA_RECORDER_PAUSE));
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

    public HTMLView getCameraView() {
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

    @Override
    public String getMimeType() {
        return (mimeType == null) ? DEFAULT_MIME_TYPE : mimeType.get();
    }

    private void setMimeType(String value) {
        mimeTypePropertyImpl().set(value);
    }

    @Override
    public ReadOnlyStringProperty mimeTypeProperty() {
        return mimeTypePropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyStringWrapper mimeTypePropertyImpl() {
        if (mimeType == null) {
            mimeType = new ReadOnlyStringWrapper(this, "mimeType", DEFAULT_MIME_TYPE);
        }
        return mimeType;
    }

    // state property
    private ReadOnlyObjectWrapper<State> state;

    @Override
    public State getState() {
        return (state == null) ? State.INACTIVE : state.get();
    }

    private void setState(State value) {
        statePropertyImpl().set(value);
    }

    /**
     * The current state of the MediaRecorder object (inactive, recording, or paused.)
     */
    @Override
    public ReadOnlyObjectProperty<State> stateProperty() {
        return statePropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<State> statePropertyImpl() {
        if (state == null) {
            state = new ReadOnlyObjectWrapper<>(this, "state", State.INACTIVE);
        }
        return state;
    }

    // On data available event handler
    private ObjectProperty<EventHandler<MediaRecorderEvent>> onDataAvailable;

    @Override
    public EventHandler<MediaRecorderEvent> getOnDataAvailable() {
        return (onDataAvailable == null) ? null : onDataAvailable.get();
    }

    @Override
    public void setOnDataAvailable(EventHandler<MediaRecorderEvent> value) {
        onDataAvailableProperty().set(value);
    }

    @Override
    public ObjectProperty<EventHandler<MediaRecorderEvent>> onDataAvailableProperty() {
        if (onDataAvailable == null) {
            onDataAvailable = new SimpleObjectProperty<>(this, "onDataAvailable") {

                @Override
                protected void invalidated() {
                    eventHandlerManager.setEventHandler(MediaRecorderEvent.MEDIA_RECORDER_DATA_AVAILABLE, get());
                }
            };
        }
        return onDataAvailable;
    }

    // On start event handler
    private ObjectProperty<EventHandler<MediaRecorderEvent>> onStart;

    @Override
    public EventHandler<MediaRecorderEvent> getOnStart() {
        return (onStart == null) ? null : onStart.get();
    }

    @Override
    public void setOnStart(EventHandler<MediaRecorderEvent> value) {
        onStartProperty().set(value);
    }

    @Override
    public ObjectProperty<EventHandler<MediaRecorderEvent>> onStartProperty() {
        if (onStart == null) {
            onStart = new SimpleObjectProperty<>(this, "onStart"){

                @Override
                protected void invalidated() {
                    eventHandlerManager.setEventHandler(MediaRecorderEvent.MEDIA_RECORDER_START, get());
                }
            };
        }
        return onStart;
    }

    // On pause event handler
    private ObjectProperty<EventHandler<MediaRecorderEvent>> onPause;

    @Override
    public EventHandler<MediaRecorderEvent> getOnPause() {
        return (onPause == null) ? null : onPause.get();
    }

    @Override
    public void setOnPause(EventHandler<MediaRecorderEvent> value) {
        onPauseProperty().set(value);
    }

    @Override
    public ObjectProperty<EventHandler<MediaRecorderEvent>> onPauseProperty() {
        if (onPause == null) {
            onPause = new SimpleObjectProperty<>(this, "onPause") {

                @Override
                protected void invalidated() {
                    eventHandlerManager.setEventHandler(MediaRecorderEvent.MEDIA_RECORDER_PAUSE, get());
                }
            };
        }
        return onPause;
    }

    // On stopped event handler
    private ObjectProperty<EventHandler<MediaRecorderEvent>> onStopped;

    @Override
    public EventHandler<MediaRecorderEvent> getOnStopped() {
        return (onStopped == null) ? null : onStopped.get();
    }

    @Override
    public void setOnStopped(EventHandler<MediaRecorderEvent> value) {
        onStoppedProperty().set(value);
    }

    @Override
    public ObjectProperty<EventHandler<MediaRecorderEvent>> onStoppedProperty() {
        if (onStopped == null) {
            onStopped = new SimpleObjectProperty<>(this, "onStopped") {

                @Override
                protected void invalidated() {
                    eventHandlerManager.setEventHandler(MediaRecorderEvent.MEDIA_RECORDER_STOP, get());
                }
            };
        }
        return onStopped;
    }

    // On error event handler
    private ObjectProperty<EventHandler<MediaRecorderEvent>> onError;

    @Override
    public EventHandler<MediaRecorderEvent> getOnError() {
        return (onError == null) ? null : onError.get();
    }

    @Override
    public void setOnError(EventHandler<MediaRecorderEvent> value) {
        onErrorProperty().set(value);
    }

    @Override
    public ObjectProperty<EventHandler<MediaRecorderEvent>> onErrorProperty() {
        if (onError == null) {
            onError = new SimpleObjectProperty<>(this, "onError") {

                @Override
                protected void invalidated() {
                    eventHandlerManager.setEventHandler(MediaRecorderEvent.MEDIA_RECORDER_ERROR, get());
                }
            };
        }
        return onError;
    }

    // Error property
    private ReadOnlyObjectWrapper<MediaRecorderException> error;

    @Override
    public MediaRecorderException getError() {
        return (error == null) ? null : error.get();
    }

    private void setError(MediaRecorderException error) {
        errorPropertyImpl().set(error);
    }

    @Override
    public ReadOnlyObjectProperty<MediaRecorderException> errorProperty() {
        return errorPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<MediaRecorderException> errorPropertyImpl() {
        if (error == null) {
            error = new ReadOnlyObjectWrapper<>(this, "error") {

                @Override
                protected void invalidated() {
                    final MediaRecorderException exception = get();
                    log.error(exception.toString(), exception);
                }
            };
        }
        return error;
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
        setState(State.RECORDING);
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

    private final EventHandlerManager eventHandlerManager = new EventHandlerManager(this);

    @Override
    public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        return tail.prepend(eventHandlerManager);
    }
}
