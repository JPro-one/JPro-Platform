/**
 * The module descriptor for the JPro Utils module.
 *
 * @author Besmir Beqiri
 */
module one.jpro.platform.utils {
    requires transitive javafx.controls;
    requires org.jetbrains.annotations;
    requires org.slf4j;

    exports one.jpro.platform.utils;
}