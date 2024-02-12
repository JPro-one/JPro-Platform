/**
 * The module descriptor for the OpenLink module.
 *
 * @author Besmir Beqiri
 */
module one.jpro.platform.internal.openlink {
    requires javafx.controls;
    requires org.jetbrains.annotations;
    requires org.slf4j;

    requires one.jpro.platform.internal.util;

    exports one.jpro.platform.internal.openlink;
}