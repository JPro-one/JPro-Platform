module one.jpro.platform.routing.core {
    requires transitive javafx.controls;

    requires transitive de.sandec.jnodes;
    requires transitive simplefx.core;
    requires transitive simplefx.extended;
    requires transitive simplefx.wrapping;
    requires transitive simplefx.utility;
    requires transitive scala.library;
    requires transitive jpro.webapi;

    exports one.jpro.platform.routing;
    exports one.jpro.platform.routing.crawl;
    exports one.jpro.platform.routing.filter.container;
    exports one.jpro.platform.routing.sessionmanager;
    exports one.jpro.platform.routing.extensions.linkheader;
}