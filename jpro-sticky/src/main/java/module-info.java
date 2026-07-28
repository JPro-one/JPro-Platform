/**
 * Module descriptor for the JPro Sticky module.
 *
 * @author Tobias Horak
 */
module one.jpro.platform.sticky {
    requires transitive javafx.graphics;
    requires transitive one.jpro.platform.utils;
    requires jpro.webapi;
    requires one.jpro.jmemorybuddy;
    requires org.slf4j;

    exports one.jpro.platform.sticky;
}
