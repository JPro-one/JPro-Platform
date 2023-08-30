package one.jpro.platform.image;

import one.jpro.platform.image.encoder.ImageEncoder;
import one.jpro.platform.image.source.ImageSource;
import one.jpro.platform.image.transformer.ImageTransformer;
import org.json.JSONObject;

/**
 * Represents a definition of an image that encompasses its source, transformation, and encoding details.
 * This class also provides the capability to convert its instance into a JSON representation.
 *
 * @author Florian Kirmaier
 * @author Besmir Beqiri
 * @see JsonConvertible
 */
public class ImageDefinition implements JsonConvertible {

    /**
     * The source from which the image is obtained or generated.
     */
    private final ImageSource source;

    /**
     * The transformer responsible for applying transformations to the image.
     */
    private final ImageTransformer transformer;

    /**
     * The encoder that defines how the image should be encoded.
     */
    private final ImageEncoder encoder;

    /**
     * Constructs a new instance of ImageDefinition.
     *
     * @param source      The source of the image.
     * @param transformer The transformer for the image.
     * @param encoder     The encoder for the image.
     */
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

    /**
     * Returns the image source associated with this definition.
     *
     * @return The {@link ImageSource} instance.
     */
    public ImageSource getSource() {
        return source;
    }

    /**
     * Returns the image transformer associated with this definition.
     *
     * @return The {@link ImageTransformer} instance.
     */
    public ImageTransformer getTransformer() {
        return transformer;
    }

    /**
     * Returns the image encoder associated with this definition.
     *
     * @return The {@link ImageEncoder} instance.
     */
    public ImageEncoder getEncoder() {
        return encoder;
    }

    /**
     * Computes and returns a hash string representation for this instance.
     * Note: This method currently returns the default hashCode as a string, which might not guarantee uniqueness.
     *
     * @return A string representing the hash of this instance.
     */
    String getHashString() {
        return Integer.toString(hashCode());
    }
}