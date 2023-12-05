package one.jpro.platform.mdfx.extensions;

import javafx.scene.Node;
import one.jpro.platform.mdfx.MarkdownView;

import java.util.function.BiFunction;

/**
 * An extension for the MarkdownView.
 * The scheme is used to identify the extension in the markdown string.
 *
 * An scheme of null is used to identify the default image extension.
 */
public class ImageExtension {
    String scheme;
    BiFunction<String, MarkdownView, Node> function;

    public ImageExtension(String scheme, BiFunction<String, MarkdownView, Node> function) {
        this.scheme = scheme;
        this.function = function;
    }

    public String getScheme() {
        return scheme;
    }

    public BiFunction<String, MarkdownView, Node> getFunction() {
        return function;
    }
}
