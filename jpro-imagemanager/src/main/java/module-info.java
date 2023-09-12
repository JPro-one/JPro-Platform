/**
 * Module descriptor for the JPro Image Manager module.
 */
module one.jpro.platform.imagemanager {
    requires java.desktop;
    requires javafx.graphics;
    requires org.slf4j;
    requires jpro.webapi;
    requires org.json;

    exports one.jpro.platform.image;
    exports one.jpro.platform.image.encoder;
    exports one.jpro.platform.image.source;
    exports one.jpro.platform.image.transformer;
}