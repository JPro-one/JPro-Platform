package one.jpro.platform.image.manager.transformer;

import one.jpro.platform.image.manager.JsonConvertible;

import java.awt.image.BufferedImage;

/**
 * An interface for transforming images. Implementers of this interface should provide
 * functionality to apply specific transformations to BufferedImage objects.
 *
 * @author Florian Kirmaier
 */
public interface ImageTransformer extends JsonConvertible {

    /**
     * Transforms the given image according to some specific criteria or logic.
     *
     * @param image The BufferedImage to be transformed.
     * @return A new BufferedImage resulting from the applied transformation.
     */
    BufferedImage transform(BufferedImage image);
}
