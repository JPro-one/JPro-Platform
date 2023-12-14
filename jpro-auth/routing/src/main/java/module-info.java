/**
 * Module descriptor for the Auth Routing module.
 *
 * @author Besmir Beqiri
 */
module one.jpro.platform.auth.routing {
    requires transitive one.jpro.platform.auth.core;
    requires transitive one.jpro.platform.routing.core;

    exports one.jpro.platform.auth.routing;
}