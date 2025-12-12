/**
 * Module descriptor for the example application.
 *
 * @author Besmir Beqiri
 */
module one.jpro.platform.mdfx.example {
    requires javafx.controls;
    requires org.slf4j;
    requires org.apache.commons.io;

    requires jpro.webapi;
    requires one.jpro.platform.mdfx;
    requires org.scenicview.scenicview;

    exports one.jpro.platform.mdfx.example;
}