module one.jpro.utils.example {
    requires javafx.controls;
    requires javafx.media;
    requires jpro.webapi;
    requires jpro.utils.htmlscrollpane;
    requires one.jpro.media;
    requires one.jpro.sound;
    requires one.jpro.sessionmanager;

    requires atlantafx.base;

//    requires org.bytedeco.opencv.windows.x86_64;
//    requires org.bytedeco.ffmpeg.windows.x86_64;
//    requires org.bytedeco.openblas.windows.x86_64;
//    requires org.bytedeco.opencv.linux.x86_64;
//    requires org.bytedeco.ffmpeg.linux.x86_64;
//    requires org.bytedeco.openblas.linux.x86_64;
//    requires org.bytedeco.opencv.macosx.x86_64;
//    requires org.bytedeco.ffmpeg.macosx.x86_64;
//    requires org.bytedeco.openblas.macosx.x86_64;
    requires org.bytedeco.opencv.macosx.arm64;
    requires org.bytedeco.ffmpeg.macosx.arm64;
    requires org.bytedeco.openblas.macosx.arm64;

    exports one.jpro.utils.example;
}