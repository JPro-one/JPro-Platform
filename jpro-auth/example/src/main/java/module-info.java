/**
 * Module descriptor for the example module.
 */
module one.jpro.platform.auth.example {
    requires org.controlsfx.controls;
    requires one.jpro.platform.auth.routing;
    requires one.jpro.platform.routing.core;
    requires one.jpro.platform.routing.dev;
    requires one.jpro.platform.mdfx;
    requires atlantafx.base;

    exports one.jpro.platform.auth.example.showcase;
}