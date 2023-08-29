package one.jpro.platform.imagemanager.transformer;

import java.awt.image.BufferedImage;

import java.awt.Graphics2D;

public class ImageTransformerWH implements ImageTransformer {
    private int targetWidth;
    private int targetHeight;

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
    public String toJSON() {
        return "{\"type\":\"ImageTransformerWH\",\"targetWidth\":" + targetWidth + ",\"targetHeight\":" + targetHeight + "}";
    }
}