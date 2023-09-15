/**
 * Module descriptor for the JPro Image Manager module.
 */
module one.jpro.platform.image.manager {
    requires java.desktop;
    requires javafx.graphics;
    requires org.slf4j;
    requires jpro.webapi;
    requires org.json;

    exports one.jpro.platform.image.manager;
    exports one.jpro.platform.image.manager.encoder;
    exports one.jpro.platform.image.manager.source;
    exports one.jpro.platform.image.manager.transformer;
}