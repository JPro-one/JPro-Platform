/**
 * Module descriptor for the example application.
 *
 * @author Besmir Beqiri
 */
module one.jpro.platform.file.example {
    requires javafx.controls;
    requires jpro.webapi;
    requires one.jpro.platform.file;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.material2;
    requires org.apache.commons.io;
    requires atlantafx.base;
    requires org.slf4j;

    exports one.jpro.platform.file.example.editor;
    exports one.jpro.platform.file.example.upload;
}