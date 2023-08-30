package one.jpro.platform.image.encoder;

import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageEncoderPNG implements ImageEncoder {

    @Override
    public void saveImage(BufferedImage image, File target) {
        final String fileExtensionUpperCase = getFileExtension().toUpperCase();
        try {
            target.getParentFile().mkdirs();
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