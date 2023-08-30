package one.jpro.platform.imagemanager.encoder;

import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageEncoderPNG implements ImageEncoder {

    @Override
    public void saveImage(BufferedImage image, File target) {
        try {
            target.getParentFile().mkdirs();
            boolean result = ImageIO.write(image, "PNG", target);
            if (!result) {
                throw new ImageEncoderException("The given PNG format is not supported.");
            }
        } catch (IOException e) {
            throw new ImageEncoderException("Error while saving the image to PNG format.", e);
        }
    }

    @Override
    public String fileExtension() {
        return "png";
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", getClass().getSimpleName());
        json.put("fileExtension", fileExtension());
        return json;
    }
}