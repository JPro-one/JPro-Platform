package one.jpro.platform.image.manager.encoder;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * An implementation of ImageEncoder for encoding images in JPG format.
 *
 * @author Florian Kirmaier
 * @author Besmir Beqiri
 * @see ImageEncoder
 */
public class ImageEncoderJPG implements ImageEncoder {

    private static final Logger logger = LoggerFactory.getLogger(ImageEncoderJPG.class);

    private final double quality;

    /**
     * Default constructor. Initializes with a default quality of 0.80.
     */
    public ImageEncoderJPG() {
        this(0.80);
    }

    /**
     * Constructor with specified quality.
     *
     * @param quality The quality factor for encoding, a value between 0.0 and 1.0.
     * @throws IllegalArgumentException If the provided quality is not between 0.0 and 1.0.
     */
    public ImageEncoderJPG(double quality) {
        if (quality < 0.0 || quality > 1.0) {
            throw new IllegalArgumentException("Quality should be between 0.0 and 1.0");
        }
        this.quality = quality;
    }

    /**
     * Save the provided image to the specified file in JPG format.
     * Note: The current implementation doesn't consider the quality settings due to Java's ImageIO limitations.
     *
     * @param image  The image to be saved.
     * @param target The target file where the image will be saved.
     * @throws ImageEncoderException If there's an error while saving the image.
     */
    @Override
    public void saveImage(BufferedImage image, File target) {
        final String fileExtensionUpperCase = getFileExtension().toUpperCase();
        try {
            // Java's ImageIO doesn't support JPG quality settings, so we would need another method
            // For simplicity, we'll just save it without specifying quality
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
     * Gets the file extension for this encoder, which is "jpg".
     *
     * @return The file extension string.
     */
    @Override
    public String getFileExtension() {
        return "jpg";
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", getClass().getSimpleName());
        json.put("quality", quality);
        json.put("fileExtension", getFileExtension());
        return json;
    }
}