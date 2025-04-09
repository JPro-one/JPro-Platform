/**
 * The module descriptor for the JPro Utils module.
 *
 * @author Besmir Beqiri
 */
module one.jpro.platform.utils {
    requires transitive javafx.controls;
    requires org.jetbrains.annotations;
    requires org.slf4j;
    requires jpro.webapi;
    requires javafx.graphics;

    exports one.jpro.platform.utils;
}