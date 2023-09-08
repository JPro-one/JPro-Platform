package one.jpro.platform.image.encoder;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * An implementation of the ImageEncoder interface for PNG image format.
 *
 * @author Florian Kirmaier
 * @author Besmir Beqiri
 * @see ImageEncoder
 */
public class ImageEncoderPNG implements ImageEncoder {

    private static final Logger logger = LoggerFactory.getLogger(ImageEncoderPNG.class);

    /**
     * Saves the provided BufferedImage to a specified target file in PNG format.
     *
     * @param image  The BufferedImage to be saved.
     * @param target The target file where the image should be saved.
     * @throws ImageEncoderException If there's an error during the save process,
     *         or if the provided format is unsupported.
     */
    @Override
    public void saveImage(BufferedImage image, File target) {
        final String fileExtensionUpperCase = getFileExtension().toUpperCase();
        try {
            final File parentFile = target.getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                if (parentFile.mkdirs()) {
                    logger.info("Created directory: {}", parentFile.getAbsolutePath());
                } else {
                    throw new ImageEncoderException("Failed to create directory: " + parentFile.getAbsolutePath());
                }
            }
            boolean result = ImageIO.write(image, fileExtensionUpperCase, target);
            if (!result) {
                throw new ImageEncoderException("The given " + fileExtensionUpperCase + " format is not supported.");
            }
        } catch (IOException ex) {
            throw new ImageEncoderException("Error while saving the image to " + fileExtensionUpperCase + " format.", ex);
        }
    }

    /**
     * Gets the file extension for this encoder, which is "png".
     *
     * @return The file extension string.
     */
    @Override
    public String getFileExtension() {
        return "png";
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", getClass().getSimpleName());
        json.put("fileExtension", getFileExtension());
        return json;
    }
}