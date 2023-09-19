/**
 * Module descriptor for the example module.
 */
module one.jpro.routing.example {
    requires org.controlsfx.controls;
    requires one.jpro.platform.auth;
    requires one.jpro.platform.mdfx;
    requires one.jpro.platform.routing.core;
    requires one.jpro.platform.routing.dev;
    requires one.jpro.platform.routing.popup;
    requires org.json;
    requires scala.library;
    requires simplefx.extended;
    requires atlantafx.base;

    exports example.colors;
    exports example.filters;
    exports example.login;
    exports example.popup;
}