package one.jpro.platform.mdfx.extensions;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import one.jpro.platform.youtube.YoutubeNode;

public class YoutubeExtension {

    public static ImageExtension create() {
        return new ImageExtension(
                "youtube", (url, view) -> {
            var uri = java.net.URI.create(url);
            var schemeSpecificPart = uri.getSchemeSpecificPart();
            var node = new VBox(new Label(""), new YoutubeNode(schemeSpecificPart));
            node.prefWidthProperty().bind(view.widthProperty());
            node.maxWidthProperty().bind(view.widthProperty());
            return node;
        });
    }
}
