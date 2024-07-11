/**
 * Module descriptor for the example module.
 */
module one.jpro.routing.example {
    requires org.controlsfx.controls;
    requires one.jpro.platform.routing.core;
    requires one.jpro.platform.routing.dev;
    requires one.jpro.platform.routing.popup;
    requires simplefx.extended;

    exports example.colors;
    exports example.filters;
    exports example.popup;
    exports example.scala;
}