open module one.jpro.imagemanager {
    requires java.desktop;
    requires javafx.graphics;
    requires javafx.controls;
    requires org.slf4j;
    requires jpro.webapi;
    requires org.json;

    exports one.jpro.platform.imagemanager;
    exports one.jpro.platform.imagemanager.encoder;
    exports one.jpro.platform.imagemanager.source;
    exports one.jpro.platform.imagemanager.transformer;
}