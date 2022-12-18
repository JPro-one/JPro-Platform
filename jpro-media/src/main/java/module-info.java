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

    requires org.bytedeco.opencv.windows.x86_64;
    requires org.bytedeco.ffmpeg.windows.x86_64;
    requires org.bytedeco.openblas.windows.x86_64;
    requires org.bytedeco.opencv.linux.x86_64;
    requires org.bytedeco.ffmpeg.linux.x86_64;
    requires org.bytedeco.openblas.linux.x86_64;
    requires org.bytedeco.opencv.macosx.x86_64;
    requires org.bytedeco.ffmpeg.macosx.x86_64;
    requires org.bytedeco.openblas.macosx.x86_64;
    requires org.bytedeco.opencv.macosx.arm64;
    requires org.bytedeco.ffmpeg.macosx.arm64;
    requires org.bytedeco.openblas.macosx.arm64;

    exports one.jpro.media.recorder;
    exports one.jpro.media.recorder.event;
}