package one.jpro.platform.image.manager.transformer;

import org.json.JSONObject;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Implements the ImageTransformer interface to provide functionalities
 * for transforming images based on specified width and height.
 *
 * @author Florian Kirmaier
 * @author Besmir Beqiri
 * @see ImageTransformer
 */
public class ImageTransformerCover implements ImageTransformer {

    private final int targetWidth;
    private final int targetHeight;

    /**
     * Constructs an instance of ImageTransformerCover with specified target width and height.
     *
     * @param targetWidth  The desired width of the transformed image.
     * @param targetHeight The desired height of the transformed image.
     */
    public ImageTransformerCover(int targetWidth, int targetHeight) {
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
    }

    /**
     * Constructs an instance of ImageTransformerCover with a specified target width and height,
     * and scales them by the provided ratio.
     *
     * @param targetWidth  The initial width value before scaling.
     * @param targetHeight The initial height value before scaling.
     * @param ratio        The scaling factor for width and height.
     */
    public ImageTransformerCover(int targetWidth, int targetHeight, double ratio) {
        this.targetWidth = (int) (targetWidth * ratio);
        this.targetHeight = (int) (targetHeight * ratio);
    }

    /**
     * Transforms the provided original BufferedImage to match the target width and height.
     *
     * @param original The BufferedImage to be transformed.
     * @return The transformed BufferedImage.
     */
    @Override
    public BufferedImage transform(BufferedImage original) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, original.getType());
        Graphics2D g2d = resizedImage.createGraphics();
        ImageTransformerHelpers.graphicsDefaultConfiguration(g2d);
        double scale = Math.max((double) targetWidth / original.getWidth(), (double) targetHeight / original.getHeight());
        int newWidth = (int) Math.round(original.getWidth() * scale);
        int newHeight = (int) Math.round(original.getHeight() * scale);

// Center the image by calculating the top-left coordinates
        int x = (targetWidth - newWidth) / 2;
        int y = (targetHeight - newHeight) / 2;

        g2d.drawImage(original, x, y, newWidth, newHeight, null);
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