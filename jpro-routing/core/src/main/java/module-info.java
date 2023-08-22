module one.jpro.routing.core {
    requires transitive javafx.controls;

    requires transitive de.sandec.jnodes;
    requires transitive simplefx.core;
    requires transitive simplefx.extended;
    requires transitive simplefx.wrapping;
    requires transitive simplefx.utility;
    requires transitive scala.library;
    requires transitive jpro.webapi;
    requires java.desktop;

    exports one.jpro.routing;
    exports one.jpro.routing.crawl;
    exports one.jpro.routing.filter.container;
    exports one.jpro.routing.sessionmanager;
    exports one.jpro.routing.extensions.linkheader;
}