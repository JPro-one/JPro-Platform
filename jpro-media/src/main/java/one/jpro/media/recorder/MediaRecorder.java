package one.jpro.media.recorder;

import com.jpro.webapi.HTMLView;
import com.jpro.webapi.WebAPI;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import one.jpro.media.recorder.event.MediaRecorderEvent;
import one.jpro.media.recorder.impl.jpro.JProMediaRecorder;

/**
 * The MediaRecorder interface of the MediaStream Recording API
 * provides functionality to easily record media.
 *
 * @author Besmir Beqiri
 */
public interface MediaRecorder extends EventTarget {

    static MediaRecorder create(WebAPI webAPI) {
        return new JProMediaRecorder(webAPI);
    }

    /**
     * MediaR recorder state.
     */
    enum State {

        INACTIVE,
        RECORDING,
        PAUSED
    }

    public HTMLView getCameraView();

    String getMimeType();

    void setMimeType(String value);

    ReadOnlyStringProperty mimeTypeProperty();

    State getState();

    void setState(State value);

    ReadOnlyObjectProperty<State> stateProperty();


    EventHandler<MediaRecorderEvent> getOnStopped();

    void setOnStopped(EventHandler<MediaRecorderEvent> value);

    ObjectProperty<EventHandler<MediaRecorderEvent>> onStoppedProperty();

    // Recorder controller methods
    void enable();

    void start();

    void pause();

    void resume();

    void stop();

    void download();
}
