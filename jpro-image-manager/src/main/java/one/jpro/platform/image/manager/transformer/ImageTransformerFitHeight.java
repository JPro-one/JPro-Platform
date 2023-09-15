package one.jpro.platform.image.manager.transformer;

import org.json.JSONObject;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * This class is responsible for transforming images such that they fit a specified height,
 * while maintaining their original aspect ratio.
 *
 * @author Florian Kirmaier
 * @see ImageTransformer
 */
public class ImageTransformerFitHeight implements ImageTransformer {

    private final int targetHeight;

    /**
     * Constructs an instance with a specified target height.
     *
     * @param targetHeight The target height in pixels for the resulting image.
     */
    public ImageTransformerFitHeight(int targetHeight) {
        this.targetHeight = targetHeight;
    }

    /**
     * Constructs an instance with a specified target height and device
     * pixel ratio. This constructor adjusts the target height based
     * on the provided device pixel ratio.
     *
     * @param targetHeight      The original target height in pixels.
     * @param devicePixelRatio  The device pixel ratio to adjust the height.
     */
    public ImageTransformerFitHeight(int targetHeight, int devicePixelRatio) {
        this.targetHeight = targetHeight * devicePixelRatio;
    }

    /**
     * Transforms (resizes) the given image to have the target height
     * while maintaining its original aspect ratio.
     *
     * @param original  The original BufferedImage to be transformed.
     * @return  A new BufferedImage that has been resized to the target height.
     */
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