package one.jpro.platform.imagemanager.transformer;

import org.json.JSONObject;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageTransformerFitHeight implements ImageTransformer {

    private final int targetHeight;

    public ImageTransformerFitHeight(int targetHeight) {
        this.targetHeight = targetHeight;
    }

    public ImageTransformerFitHeight(int targetHeight, int devicePixelRatio) {
        this.targetHeight = targetHeight * devicePixelRatio;
    }

    @Override
    public BufferedImage transform(BufferedImage original) {
        double aspectRatio = (double) original.getWidth() / original.getHeight();
        int newWidth = (int) (targetHeight * aspectRatio);
        BufferedImage resized = new BufferedImage(newWidth, targetHeight, original.getType());
        Graphics2D g2d = resized.createGraphics();
        ImageTransformerHelpers.graphicsDefaultConfiguration(g2d);
        g2d.drawImage(original, 0, 0, newWidth, targetHeight, null);
        return resized;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", getClass().getSimpleName());
        json.put("targetHeight", targetHeight);
        return json;
    }
}