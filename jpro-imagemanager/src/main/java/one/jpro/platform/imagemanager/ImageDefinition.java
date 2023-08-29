package one.jpro.platform.imagemanager;

import one.jpro.platform.imagemanager.encoder.ImageEncoder;
import one.jpro.platform.imagemanager.source.ImageSource;
import one.jpro.platform.imagemanager.transformer.ImageTransformer;

public class ImageDefinition implements JsonStringConvertible {

    private final ImageSource source;
    private final ImageTransformer transformer;
    private final ImageEncoder encoder;

    public ImageDefinition(ImageSource source, ImageTransformer transformer, ImageEncoder encoder) {
        this.source = source;
        this.transformer = transformer;
        this.encoder = encoder;
    }

    @Override
    public String toJSON() {
        return "{" +
                "\"source\":" + source.toJSON() + "," +
                "\"transformer\":" + transformer.toJSON() + "," +
                "\"encoder\":" + encoder.toJSON() +
                "}";
    }

    public ImageSource getSource() {
        return source;
    }

    public ImageTransformer getTransformer() {
        return transformer;
    }

    public ImageEncoder getEncoder() {
        return encoder;
    }

    String getHashString() {
        return Integer.toString(hashCode());
    }
}