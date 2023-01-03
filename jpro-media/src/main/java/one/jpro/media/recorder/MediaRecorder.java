package one.jpro.media.recorder;

import com.jpro.webapi.WebAPI;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.stage.Stage;
import one.jpro.media.MediaEngine;
import one.jpro.media.MediaSource;
import one.jpro.media.MediaView;
import one.jpro.media.event.MediaRecorderEvent;
import one.jpro.media.recorder.impl.FXMediaRecorder;
import one.jpro.media.recorder.impl.WebMediaRecorder;

import java.util.Optional;

/**
 * The MediaRecorder provides functionality to easily record media.
 *
 * @author Besmir Beqiri
 */
public interface MediaRecorder extends MediaEngine, EventTarget {

    /**
     * Creates a media recorder. If the application is running in a
     * browser via JPro server, then a web version of {@link MediaRecorder}
     * is returned. If the application is not running inside the browser
     * than a desktop/mobile version of the media recorder is returned.
     *
     * @param stage the application stage
     * @return a {@link MediaRecorder} object.
     */
    static MediaRecorder create(Stage stage) {
        if (WebAPI.isBrowser()) {
            final WebAPI webAPI = WebAPI.getWebAPI(stage);
            return new WebMediaRecorder(webAPI);
        }
        return new FXMediaRecorder();
    }

    /**
     * Media recorder status.
     */
    enum Status {

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
         * @return an {@link Optional} object containing the {@link Status} object.
         */
        public static Optional<Status> fromJS(String jsStr) {
            if (jsStr != null && !jsStr.isBlank()) {
                var str = jsStr.replace("\"", "").trim();
                for (Status s : values()) {
                    if (s.name().equalsIgnoreCase(str)) {
                        return Optional.of(s);
                    }
                }
            }
            return Optional.empty();
        }
    }

    /**
     * Retrieves the current media source.
     *
     * @return {@link MediaSource} object
     */
    MediaSource getMediaSource();

    /**
     * The current media source for this recorder.
     */
    ReadOnlyObjectProperty<MediaSource> mediaSourceProperty();

    /**
     * Retrieves the current recorder status.
     *
     * @return the recorder status
     */
    Status getStatus();

    /**
     * Recorder status hold the internal state for this recorder.
     */
    ReadOnlyObjectProperty<Status> statusProperty();

    /**
     * Retrieves the event handler when the recording has started
     * and media data is delivered to the application.
     *
     * @return the event handler or <code>null</code>.
     */
    EventHandler<MediaRecorderEvent> getOnDataAvailable();

    /**
     * Sets the event handler when the recording has started
     * and media data is delivered to the application.
     *
     * @param value the event handler or <code>null</code>.
     */
    void setOnDataAvailable(EventHandler<MediaRecorderEvent> value);

    /**
     * Event handler invoked when this recorder delivers media data
     * to the application for its use.
     */
    ObjectProperty<EventHandler<MediaRecorderEvent>> onDataAvailableProperty();

    /**
     * Retrieves the event handler when the recording has started
     * and status is set to {@link MediaRecorder.Status#RECORDING}.
     *
     * @return the event handler or <code>null</code>.
     */
    EventHandler<MediaRecorderEvent> getOnStart();

    /**
     * Sets the event handler when the recording has started
     * and status is set to {@link MediaRecorder.Status#RECORDING}.
     *
     * @param value the event handler or <code>null</code>.
     */
    void setOnStart(EventHandler<MediaRecorderEvent> value);

    /**
     * Event handler invoked when the recording started.
     */
    ObjectProperty<EventHandler<MediaRecorderEvent>> onStartProperty();

    /**
     * Retrieves the event handler when the recording has paused
     * and status is set to {@link MediaRecorder.Status#PAUSED}.
     *
     * @return the event handler or <code>null</code>.
     */
    EventHandler<MediaRecorderEvent> getOnPause();

    /**
     * Sets the event handler when the media recorder has paused
     * and status is set to {@link MediaRecorder.Status#PAUSED}.
     *
     * @param value the event handler or <code>null</code>.
     */
    void setOnPause(EventHandler<MediaRecorderEvent> value);

    /**
     * Event handler invoked when the recorder has paused,
     * and status is set to {@link MediaRecorder.Status#PAUSED}.
     */
    ObjectProperty<EventHandler<MediaRecorderEvent>> onPauseProperty();

    /**
     * Retrieves the event handler when recording has resumed
     * and status changes to {@link MediaRecorder.Status#RECORDING}
     * after previously was set to {@link MediaRecorder.Status#PAUSED}.
     *
     * @return the event handler or <code>null</code>.
     */
    EventHandler<MediaRecorderEvent> getOnResume();

    /**
     * Sets the event handler when recording has resumed
     * and status changes to {@link MediaRecorder.Status#RECORDING}
     * after previously was set to {@link MediaRecorder.Status#PAUSED}.
     *
     * @param value the event handler or <code>null</code>.
     */
    void setOnResume(EventHandler<MediaRecorderEvent> value);

    /**
     * Event handler invoked when recording has resumed and
     * the status changes to {@link MediaRecorder.Status#RECORDING}
     * after previously was set to {@link MediaRecorder.Status#PAUSED}.
     */
    ObjectProperty<EventHandler<MediaRecorderEvent>> onResumeProperty();

    /**
     * Retrieves the event handler when recording is stopped
     * and status changes to {@link MediaRecorder.Status#INACTIVE}.
     *
     * @return the event handler or <code>null</code>.
     */
    EventHandler<MediaRecorderEvent> getOnStopped();

    /**
     * Sets the event handler when recording is stopped
     * and status changes to {@link MediaRecorder.Status#INACTIVE}.
     *
     * @param value the event handler or <code>null</code>.
     */
    void setOnStopped(EventHandler<MediaRecorderEvent> value);

    /**
     * Event handler invoked when recording stopped and the
     * status changes to {@link MediaRecorder.Status#INACTIVE}.
     */
    ObjectProperty<EventHandler<MediaRecorderEvent>> onStoppedProperty();

    /**
     * Retrieves the event handler for errors.
     *
     * @return the event handler.
     */
    EventHandler<MediaRecorderEvent> getOnError();

    /**
     * Sets the event handler to be called when an error occurs.
     *
     * @param value the event handler or <code>null</code>.
     */
    void setOnError(EventHandler<MediaRecorderEvent> value);

    /**
     * Event handler invoked when an error occurs.
     */
    ObjectProperty<EventHandler<MediaRecorderEvent>> onErrorProperty();

    /**
     * Retrieve the value of the {@link #errorProperty error}
     * property or <code>null</code> if there is no error.
     *
     * @return a {@link MediaRecorderException} or <code>null</code>.
     */
    MediaRecorderException getError();

    /**
     * Observable property set to a {@link MediaRecorderException} if an error occurs.
     */
    ReadOnlyObjectProperty<MediaRecorderException> errorProperty();

    // Recorder controller methods
    /**
     * Enable camera stream from the device. If this process is successful,
     * then use the {@link MediaView#create(MediaRecorder)}
     * method to create the camera view and show it.
     */
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
}
