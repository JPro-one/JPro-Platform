package one.jpro.platform.imagemanager.encoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageEncoderPNG implements ImageEncoder {

    @Override
    public void saveImage(BufferedImage image, File target) {
        try {
            target.getParentFile().mkdirs();
            boolean result = ImageIO.write(image, "JPG", target);
            if(!result) {
                throw new RuntimeException("Failed to save image");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error while saving the image to PNG format.", e);
        }
    }

    @Override
    public String fileExtension() {
        return "png";
    }

    @Override
    public String toJson() {
        return "{\"type\":\"ImageEncoderPNG\"}";
    }
}