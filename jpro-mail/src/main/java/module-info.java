/**
 * Module descriptor for JPro Mail module.
 *
 * @author Besmir Beqiri
 */
module one.jpro.platform.mail {
    requires transitive org.slf4j;
    requires transitive org.eclipse.collections.api;

    requires jakarta.mail;
    requires org.eclipse.collections;
    requires org.jetbrains.annotations;

    exports one.jpro.platform.mail;
    exports one.jpro.platform.mail.config;
}