/**
 * Module descriptor for the example module.
 */
module one.jpro.platform.auth.example {
    requires org.controlsfx.controls;
    requires one.jpro.platform.auth;
    requires one.jpro.platform.mdfx;
    requires one.jpro.platform.routing.core;
    requires one.jpro.platform.routing.dev;
    requires org.json;
    requires atlantafx.base;

    exports one.jpro.platform.auth.example.login;
}