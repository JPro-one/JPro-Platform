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
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"source\":").append(source.toJson()).append(",");
        sb.append("\"transformer\":").append(transformer.toJson()).append(",");
        sb.append("\"encoder\":").append(encoder.toJson());
        sb.append("}");
        return sb.toString();
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