package one.jpro.platform.imagemanager;

import one.jpro.platform.imagemanager.encoder.ImageEncoder;
import one.jpro.platform.imagemanager.source.ImageSource;
import one.jpro.platform.imagemanager.transformer.ImageTransformer;
import org.json.JSONObject;

public class ImageDefinition implements JsonConvertible {

    private final ImageSource source;
    private final ImageTransformer transformer;
    private final ImageEncoder encoder;

    public ImageDefinition(ImageSource source, ImageTransformer transformer, ImageEncoder encoder) {
        this.source = source;
        this.transformer = transformer;
        this.encoder = encoder;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("source", source.toJSON());
        json.put("transformer", transformer.toJSON());
        json.put("encoder", encoder.toJSON());
        return json;
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