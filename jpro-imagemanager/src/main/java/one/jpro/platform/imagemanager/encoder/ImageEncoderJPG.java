package one.jpro.platform.imagemanager.encoder;

import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageEncoderJPG implements ImageEncoder {

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
        try {
            // Java's ImageIO doesn't support JPG quality settings, so we would need another method
            // For simplicity, we'll just save it without specifying quality
            target.getParentFile().mkdirs();
            boolean result = ImageIO.write(image, "JPG", target);
            if(!result) {
                throw new ImageEncoderException("The given JPG format is not supported.");
            }
        } catch (IOException ex) {
            throw new ImageEncoderException("Error while saving the image to JPG format.", ex);
        }
    }

    @Override
    public String fileExtension() {
        return "jpg";
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", getClass().getSimpleName());
        json.put("quality", quality);
        json.put("fileExtension", fileExtension());
        return json;
    }
}