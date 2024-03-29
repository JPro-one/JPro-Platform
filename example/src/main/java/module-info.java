module one.jpro.platform.example {
    requires javafx.controls;
    requires javafx.media;
    requires atlantafx.base;
    requires org.slf4j;

    requires jpro.webapi;
    requires one.jpro.platform.routing.core;
    requires one.jpro.platform.mdfx.example;
    requires one.jpro.platform.media.example;
    requires one.jpro.platform.htmlscrollpane.example;
    requires one.jpro.platform.sessions.example;

    exports one.jpro.platform.example;
}