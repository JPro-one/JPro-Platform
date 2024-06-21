/**
 * Module descriptor for the example application.
 *
 * @author Besmir Beqiri
 */
module one.jpro.platform.mail.example {
    requires javafx.controls;
    requires jpro.webapi;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.material2;
    requires atlantafx.base;
    requires org.slf4j;
    requires one.jpro.platform.mail;

    exports one.jpro.platform.mail.example.compose;
}