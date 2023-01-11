import one.jpro.media.MediaSource;
import one.jpro.media.MediaView;
import one.jpro.media.player.MediaPlayer;
import one.jpro.media.recorder.MediaRecorder;

/**
 * Defines APIs for playback and recording of video and audio content.
 * <p>
 * The APIs are much similar to the JavaFX APIs, but besides
 * being used in JavaFX applications running on desktop/mobile devices,
 * they can also be used in web applications running via JPro server,
 * without changing a single line of code.
 * <p>
 * The principal classes are {@link MediaSource}, {@link MediaView},
 * {@link MediaPlayer} and {@link MediaRecorder}.
 *
 * @author Besmir Beqiri
 */
module one.jpro.media {
    requires javafx.controls;
    requires javafx.media;
    requires javafx.swing;
    requires jpro.webapi;
    requires org.json;
    requires org.slf4j;

    requires org.bytedeco.javacv;
    requires org.bytedeco.opencv;
    requires org.bytedeco.ffmpeg;

    exports one.jpro.media;
    exports one.jpro.media.event;
    exports one.jpro.media.player;
    exports one.jpro.media.recorder;
    exports one.jpro.media.util;
}