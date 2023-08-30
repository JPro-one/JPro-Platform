package one.jpro.platform.image.transformer;

import one.jpro.platform.image.JsonConvertible;

import java.awt.image.BufferedImage;

public interface ImageTransformer extends JsonConvertible {
    BufferedImage transform(BufferedImage image);
}
