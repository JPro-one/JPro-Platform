/**
 * Module descriptor for the example application.
 *
 * @author Besmir Beqiri
 */
module one.jpro.platform.sessions.example {
    requires javafx.controls;
    requires jpro.webapi;
    requires atlantafx.base;
    requires one.jpro.platform.session;
    requires org.slf4j;
    requires org.apache.commons.io;

    exports one.jpro.platform.session.example;
}