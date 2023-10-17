/**
 * Module descriptor for JPro File module.
 *
 * @author Besmir Beqiri
 */
module one.jpro.platform.file {
    requires javafx.controls;
    requires jpro.webapi;
    requires org.jetbrains.annotations;

    exports one.jpro.platform.file;
    exports one.jpro.platform.file.picker;
    exports one.jpro.platform.file.dropper;
}