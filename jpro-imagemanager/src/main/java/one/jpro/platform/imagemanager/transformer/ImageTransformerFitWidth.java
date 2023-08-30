package one.jpro.platform.imagemanager.transformer;

import org.json.JSONObject;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageTransformerFitWidth implements ImageTransformer {

    private final int targetWidth;

    public ImageTransformerFitWidth(int targetWidth) {
        this.targetWidth = targetWidth;
    }

    public ImageTransformerFitWidth(int targetWidth, int devicePixelRatio) {
        this.targetWidth = targetWidth * devicePixelRatio;
    }
    @Override
    public BufferedImage transform(BufferedImage inputImage) {
        int originalWidth = inputImage.getWidth();
        int originalHeight = inputImage.getHeight();

        double aspectRatio = (double) originalHeight / originalWidth;
        int targetHeight = (int) (targetWidth * aspectRatio);

        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, inputImage.getType());
        Graphics2D g2d = (Graphics2D) outputImage.getGraphics();
        ImageTransformerHelpers.graphicsDefaultConfiguration(g2d);
        g2d.drawImage(inputImage, 0, 0, targetWidth, targetHeight, null);
        return outputImage;
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = new JSONObject();
        json.put("type", getClass().getSimpleName());
        json.put("targetWidth", targetWidth);
        return json;
    }
}