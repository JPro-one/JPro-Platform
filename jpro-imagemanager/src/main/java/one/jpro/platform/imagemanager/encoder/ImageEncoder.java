package one.jpro.platform.imagemanager.encoder;

import one.jpro.platform.imagemanager.JsonStringConvertible;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Interface for encoding and saving images.
 * Implementing classes must provide methods for saving a BufferedImage to a File,
 * and returning the file extension of the encoded image.
 *
 * @see JsonStringConvertible
 * @author Florian Kirmaier
 * @author Besmir Beqiri
 */
public interface ImageEncoder extends JsonStringConvertible {

    /**
     * Saves a {@link BufferedImage} to a specified {@link File}.
     *
     * @param image  the image to be saved
     * @param target the target file to save the image
     */
    void saveImage(BufferedImage image, File target);

    /**
     * Returns the file extension of the encoded image.
     *
     * @return The file extension as a string.
     */
    String fileExtension();
}