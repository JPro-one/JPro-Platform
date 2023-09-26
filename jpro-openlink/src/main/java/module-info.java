/**
 * The module descriptor for the OpenLink module.
 *
 * @author Besmir Beqiri
 */
module one.jpro.platform.openlink {
    requires javafx.controls;
    requires jpro.webapi;
    requires org.jetbrains.annotations;
    requires org.slf4j;

    exports one.jpro.platform.openlink;
    exports one.jpro.platform.openlink.util;
}