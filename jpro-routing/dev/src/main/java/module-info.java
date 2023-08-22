/**
 * Module descriptor for the Routing Dev module.
 *
 * @author Besmir Beqiri
 */
open module one.jpro.routing.dev {
    requires transitive javafx.controls;
    requires one.jpro.routing.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.evaicons;
    requires org.kordamp.ikonli.ionicons4;
    requires org.scenicview.scenicview;

    exports one.jpro.routing.dev;
}