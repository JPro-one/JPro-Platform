module one.jpro.platform.routing.core {
    requires transitive javafx.controls;
    requires transitive org.slf4j;

    requires transitive de.sandec.jnodes;
    requires transitive de.sandec.jmemorybuddy;
    requires transitive jpro.webapi;
    requires transitive simplefx.core;
    requires transitive simplefx.utility;
    requires transitive simplefx.wrapping;
    requires transitive simplefx.extended;
    requires transitive scala.library;
    requires transitive one.jpro.platform.internal.openlink;

    exports one.jpro.platform.routing;
    exports one.jpro.platform.routing.crawl;
    exports one.jpro.platform.routing.performance;
    exports one.jpro.platform.routing.filter.container;
    exports one.jpro.platform.routing.sessionmanager;
    exports one.jpro.platform.routing.extensions.linkheader;
}