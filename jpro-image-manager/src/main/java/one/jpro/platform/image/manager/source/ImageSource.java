package one.jpro.platform.image.manager.source;

import one.jpro.platform.image.manager.JsonConvertible;

import java.awt.image.BufferedImage;

/**
 * The ImageSource interface represents a source of images that can be loaded and manipulated.
 *
 * @author Florian Kirmaier
 * @see JsonConvertible
 */
public interface ImageSource extends JsonConvertible {

    /**
     * Loads an image and returns it as BufferedImage.
     *
     * @return The loaded image as a BufferedImage object.
     */
    BufferedImage loadImage();

    /**
     * Returns the identity hash code associated with this image source.
     *
     * @return The identity hash value corresponding to this object.
     */
    long identityHashValue();

    /**
     * Returns the file name associated with this image source.
     *
     * @return The file name as a string.
     */
    String getFileName();
}
