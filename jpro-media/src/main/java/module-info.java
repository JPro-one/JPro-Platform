/**
 * JPro Media module descriptor.
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

    exports one.jpro.media.event;
    exports one.jpro.media.player;
    exports one.jpro.media.recorder;
    exports one.jpro.media.recorder.event;
}