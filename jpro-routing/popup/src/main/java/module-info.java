/**
 * Module descriptor for the Popup module.
 *
 * @author Besmir Beqiri
 */
module one.jpro.platform.routing.popup {
    requires javafx.controls;
    requires simplefx.extended;
    requires scala.library;
    requires org.kordamp.ikonli.javafx;
    requires one.jpro.platform.routing.core;

    exports one.jpro.platform.routing.popup.simplepopup;
    exports one.jpro.platform.routing.popup;
}