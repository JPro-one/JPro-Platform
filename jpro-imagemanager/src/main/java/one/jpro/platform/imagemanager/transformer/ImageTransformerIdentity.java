package one.jpro.platform.imagemanager.transformer;

import java.awt.image.BufferedImage;

/**
 * Useful for creating VirtualImages with JPro.
 */
public class ImageTransformerIdentity implements ImageTransformer {

    public BufferedImage transform(BufferedImage image){
        return image;
    }

    @Override
    public String toJSON() {
        return "{\"type\":\"ImageTransformerIdentity\"}";
    }

}
