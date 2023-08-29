package one.jpro.platform.imagemanager.transformer;

import one.jpro.platform.imagemanager.JsonStringConvertible;

import java.awt.image.BufferedImage;

public interface ImageTransformer extends JsonStringConvertible {
    public BufferedImage transform(BufferedImage image);
}
