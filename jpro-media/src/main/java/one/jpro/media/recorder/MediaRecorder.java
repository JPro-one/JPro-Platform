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

import java.util.Optional;

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
        PAUSED;

        public static Optional<State> fromJS(String jsStr) {
            if (jsStr != null && !jsStr.isBlank()) {
                var str = jsStr.replace("\"", "").trim();
                for (State s : values()) {
                    if (s.name().equalsIgnoreCase(str)) {
                        return Optional.of(s);
                    }
                }
            }
            return Optional.empty();
        }
    }

    HTMLView getCameraView();

    String getMimeType();

    ReadOnlyStringProperty mimeTypeProperty();

    State getState();

    ReadOnlyObjectProperty<State> stateProperty();


    EventHandler<MediaRecorderEvent> getOnDataAvailable();

    void setOnDataAvailable(EventHandler<MediaRecorderEvent> value);

    ObjectProperty<EventHandler<MediaRecorderEvent>> onDataAvailableProperty();

    EventHandler<MediaRecorderEvent> getOnStart();

    void setOnStart(EventHandler<MediaRecorderEvent> value);

    ObjectProperty<EventHandler<MediaRecorderEvent>> onStartProperty();

    EventHandler<MediaRecorderEvent> getOnPause();

    void setOnPause(EventHandler<MediaRecorderEvent> value);

    ObjectProperty<EventHandler<MediaRecorderEvent>> onPauseProperty();

    EventHandler<MediaRecorderEvent> getOnResume();

    void setOnResume(EventHandler<MediaRecorderEvent> value);

    ObjectProperty<EventHandler<MediaRecorderEvent>> onResumeProperty();

    EventHandler<MediaRecorderEvent> getOnStopped();

    void setOnStopped(EventHandler<MediaRecorderEvent> value);

    ObjectProperty<EventHandler<MediaRecorderEvent>> onStoppedProperty();

    EventHandler<MediaRecorderEvent> getOnError();

    void setOnError(EventHandler<MediaRecorderEvent> value);

    ObjectProperty<EventHandler<MediaRecorderEvent>> onErrorProperty();

    MediaRecorderException getError();

    ReadOnlyObjectProperty<MediaRecorderException> errorProperty();

    // Recorder controller methods
    void enable();

    void start();

    void pause();

    void resume();

    void stop();

    void download();
}
