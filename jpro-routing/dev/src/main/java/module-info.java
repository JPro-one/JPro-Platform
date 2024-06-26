/**
 * Module descriptor for the Routing Dev module.
 *
 * @author Besmir Beqiri
 */
open module one.jpro.platform.routing.dev {
    requires transitive javafx.controls;
    requires transitive one.jpro.platform.routing.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.evaicons;
    requires org.kordamp.ikonli.ionicons4;
    requires fr.brouillard.oss.cssfx;
    requires org.scenicview.scenicview;

    exports one.jpro.platform.routing.dev;
}