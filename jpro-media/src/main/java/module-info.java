import one.jpro.platform.media.MediaSource;
import one.jpro.platform.media.MediaView;
import one.jpro.platform.media.player.MediaPlayer;
import one.jpro.platform.media.recorder.MediaRecorder;

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
module one.jpro.platform.media {
    requires javafx.controls;
    requires javafx.media;
    requires javafx.swing;
    requires jpro.webapi;
    requires org.json;
    requires org.slf4j;

    requires org.bytedeco.javacv;
    requires org.bytedeco.opencv;
    requires org.bytedeco.ffmpeg;

    opens one.jpro.platform.media;
    exports one.jpro.platform.media;
    exports one.jpro.platform.media.event;
    exports one.jpro.platform.media.player;
    exports one.jpro.platform.media.recorder;
    exports one.jpro.platform.media.util;
}