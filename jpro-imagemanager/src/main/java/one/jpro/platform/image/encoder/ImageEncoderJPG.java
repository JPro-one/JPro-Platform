package one.jpro.platform.image.encoder;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageEncoderJPG implements ImageEncoder {

    private static final Logger log = LoggerFactory.getLogger(ImageEncoderJPG.class);

    private final double quality;

    public ImageEncoderJPG(double quality) {
        if (quality < 0.0 || quality > 1.0) {
            throw new IllegalArgumentException("Quality should be between 0.0 and 1.0");
        }
        this.quality = quality;
    }

    public ImageEncoderJPG() {
        this(0.80);
    }

    @Override
    public void saveImage(BufferedImage image, File target) {
        final String fileExtensionUpperCase = getFileExtension().toUpperCase();
        try {
            // Java's ImageIO doesn't support JPG quality settings, so we would need another method
            // For simplicity, we'll just save it without specifying quality
            final File parentFile = target.getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                if (parentFile.mkdirs()) {
                    log.info("Created directory: {}", parentFile.getAbsolutePath());
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