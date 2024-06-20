/**
 * Module descriptor for JPro Mail module.
 *
 * @author Besmir Beqiri
 */
module one.jpro.platform.mail {
    requires transitive org.slf4j;
    requires transitive org.eclipse.collections.api;

    requires jakarta.mail;
    requires org.jetbrains.annotations;
    requires org.eclipse.collections.impl;

    exports one.jpro.platform.mail;
}