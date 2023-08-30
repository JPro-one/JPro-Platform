package one.jpro.platform.image.transformer;

import org.json.JSONObject;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageTransformerScaleToArea implements ImageTransformer {

    private final int targetArea;

    public ImageTransformerScaleToArea(int targetArea) {
        if(targetArea <= 0) {
            throw new IllegalArgumentException("Target area must be positive.");
        }
        this.targetArea = targetArea;
    }

    public ImageTransformerScaleToArea(int targetWidth, int targetHeight) {
        this(targetWidth * targetHeight);
    }

    @Override
    public BufferedImage transform(BufferedImage image) {
        double aspectRatio = (double) image.getWidth() / image.getHeight();
        double newWidth = Math.sqrt(targetArea * aspectRatio);
        double newHeight = newWidth / aspectRatio;

        BufferedImage newImage = new BufferedImage((int) newWidth, (int) newHeight, image.getType());
        Graphics2D g2d = (Graphics2D) newImage.getGraphics();
        ImageTransformerHelpers.graphicsDefaultConfiguration(g2d);
        g2d.drawImage(image, 0, 0, (int) newWidth, (int) newHeight, null);
        return newImage;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", getClass().getSimpleName());
        json.put("targetArea", targetArea);
        return json;
    }
}