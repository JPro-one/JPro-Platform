module one.jpro.utils.example {
    requires javafx.controls;
    requires javafx.media;
    requires jpro.webapi;
    requires jpro.utils.htmlscrollpane;
    requires one.jpro.media;
    requires one.jpro.sessionmanager;

    requires atlantafx.base;

//    requires org.bytedeco.ffmpeg.windows.x86_64;
//    requires org.bytedeco.opencv.windows.x86_64;
//    requires org.bytedeco.openblas.windows.x86_64;
//    requires org.bytedeco.videoinput.windows.x86_64;

//    requires org.bytedeco.ffmpeg.linux.x86_64;
//    requires org.bytedeco.opencv.linux.x86_64;
//    requires org.bytedeco.openblas.linux.x86_64;

//    requires org.bytedeco.ffmpeg.macosx.x86_64;
//    requires org.bytedeco.opencv.macosx.x86_64;
//    requires org.bytedeco.openblas.macosx.x86_64;

    requires org.bytedeco.ffmpeg.macosx.arm64;
    requires org.bytedeco.opencv.macosx.arm64;
    requires org.bytedeco.openblas.macosx.arm64;
    requires one.jpro.routing.core;

    exports one.jpro.platform.utils.example;
    exports one.jpro.platform.example.media;
}