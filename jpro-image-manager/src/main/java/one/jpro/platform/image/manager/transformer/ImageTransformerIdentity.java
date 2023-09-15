package one.jpro.platform.image.manager.transformer;

import org.json.JSONObject;

import java.awt.image.BufferedImage;

/**
 * This class implements an identity transformer for images. When applied,
 * it will return the original image without any modification.
 *
 * @author Florian Kirmaier
 * @author Besmir Beqiri
 * @see ImageTransformer
 */
public class ImageTransformerIdentity implements ImageTransformer {

    /**
     * Transforms the provided image. In the case of this identity transformer,
     * the original image is returned without any modifications.
     *
     * @param image The image to be transformed.
     * @return The same image that was passed as a parameter.
     */
    public BufferedImage transform(BufferedImage image) {
        return image;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", getClass().getSimpleName());
        return json;
    }

}
