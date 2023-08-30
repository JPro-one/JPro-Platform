package one.jpro.platform.image.transformer;

import org.json.JSONObject;

import java.awt.image.BufferedImage;

/**
 * Useful for creating VirtualImages with JPro.
 */
public class ImageTransformerIdentity implements ImageTransformer {

    public BufferedImage transform(BufferedImage image){
        return image;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", getClass().getSimpleName());
        return json;
    }

}
