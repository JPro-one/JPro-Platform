/**
 * Module descriptor for the Popup module.
 *
 * @author Besmir Beqiri
 */
module one.jpro.routing.popup {
    requires javafx.controls;
    requires simplefx.extended;
    requires scala.library;
    requires org.kordamp.ikonli.javafx;

    exports one.jpro.routing.popup.simplepopup;
    exports one.jpro.routing.popup;
}