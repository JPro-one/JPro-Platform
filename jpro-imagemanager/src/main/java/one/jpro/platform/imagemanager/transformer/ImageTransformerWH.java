package one.jpro.platform.imagemanager.transformer;

import org.json.JSONObject;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;

public class ImageTransformerWH implements ImageTransformer {

    private final int targetWidth;
    private final int targetHeight;

    public ImageTransformerWH(int targetWidth, int targetHeight) {
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
    }

    public ImageTransformerWH(int targetWidth, int targetHeight, double ratio) {
        this.targetWidth = (int) (targetWidth * ratio);
        this.targetHeight = (int) (targetHeight * ratio);
    }

    @Override
    public BufferedImage transform(BufferedImage original) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, original.getType());
        Graphics2D g2d = resizedImage.createGraphics();
        ImageTransformerHelpers.graphicsDefaultConfiguration(g2d);
        g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return resizedImage;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", getClass().getSimpleName());
        json.put("targetWidth", targetWidth);
        json.put("targetHeight", targetHeight);
        return json;
    }
}