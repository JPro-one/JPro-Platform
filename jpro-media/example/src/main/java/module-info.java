/**
 * Module descriptor for the example application.
 *
 * @author Besmir Beqiri
 */
module one.jpro.platform.media.example {
    requires javafx.controls;
    requires javafx.media;
    requires atlantafx.base;
    requires org.slf4j;

    requires jpro.webapi;
    requires one.jpro.platform.media;
    requires one.jpro.platform.file;

    requires org.bytedeco.ffmpeg.windows.x86_64;
    requires org.bytedeco.opencv.windows.x86_64;
    requires org.bytedeco.openblas.windows.x86_64;
    requires org.bytedeco.videoinput.windows.x86_64;

    requires org.bytedeco.ffmpeg.linux.x86_64;
    requires org.bytedeco.opencv.linux.x86_64;
    requires org.bytedeco.openblas.linux.x86_64;

    requires org.bytedeco.ffmpeg.macosx.x86_64;
    requires org.bytedeco.opencv.macosx.x86_64;
    requires org.bytedeco.openblas.macosx.x86_64;

    requires org.bytedeco.ffmpeg.macosx.arm64;
    requires org.bytedeco.opencv.macosx.arm64;
    requires org.bytedeco.openblas.macosx.arm64;

    exports one.jpro.platform.media.example;
}