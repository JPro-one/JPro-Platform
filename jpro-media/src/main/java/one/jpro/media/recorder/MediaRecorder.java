package one.jpro.media.recorder;

import com.jpro.webapi.WebAPI;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import one.jpro.media.recorder.event.MediaRecorderEvent;
import one.jpro.media.recorder.impl.javafx.JavaFXMediaRecorder;
import one.jpro.media.recorder.impl.jpro.JProMediaRecorder;

import java.util.Optional;

/**
 * The MediaRecorder interface of the MediaStream Recording API
 * provides functionality to easily record media.
 *
 * @author Besmir Beqiri
 */
public interface MediaRecorder extends EventTarget {

    /**
     * Creates a media recorder with the given JPro WebAPI.
     * If the application is running in a browser with JPro,
     * then a web version of {@link MediaRecorder} is returned.
     * If the application is not running inside the browser than
     * a desktop version of the media recorder is returned.
     *
     * @param stage the application stage
     * @return a {@link MediaRecorder} object.
     */
    static MediaRecorder create(Stage stage) {
        if (WebAPI.isBrowser()) {
            WebAPI webAPI = WebAPI.getWebAPI(stage);
            return new JProMediaRecorder(webAPI);
        }
        return new JavaFXMediaRecorder(stage);
    }

    /**
     * Media recorder state.
     */
    enum State {

        /**
         * Recording is not occurring — it has either not been started yet,
         * or it has been started and then stopped.
         */
        INACTIVE,

        /**
         * Recording has been started.
         */
        RECORDING,

        /**
         * Recording has been started, then paused, but not yet stopped or resumed.
         */
        PAUSED;

        /**
         * Parses a JavaScript string and if the content is equals to one of the states,
         * then return it wrapped in {@link Optional} object.
         *
         * @param jsStr a javascript string
         * @return an {@link Optional} object containing the {@link State} object.
         */
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

    Region getCameraView();

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

    /**
     * Begins recording media.
     */
    void start();

    /**
     * Pauses the recording of media.
     */
    void pause();

    /**
     * Resumes recording of media after having been paused.
     */
    void resume();

    /**
     * Stops the recording of media.
     */
    void stop();

    void retrieve();
}
