/**
 * Module descriptor for the example module.
 */
module one.jpro.platform.auth.example {
    requires org.controlsfx.controls;
    requires one.jpro.platform.auth.routing;
    requires one.jpro.platform.routing.dev;
    requires one.jpro.platform.mdfx;
    requires one.jpro.platform.sessions;
    requires atlantafx.base;

    exports one.jpro.platform.auth.example.basic;
    exports one.jpro.platform.auth.example.login;
    exports one.jpro.platform.auth.example.oauth;
}