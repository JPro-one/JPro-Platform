package one.jpro.platform.imagemanager.transformer;

import one.jpro.platform.imagemanager.JsonConvertible;

import java.awt.image.BufferedImage;

public interface ImageTransformer extends JsonConvertible {
    BufferedImage transform(BufferedImage image);
}
