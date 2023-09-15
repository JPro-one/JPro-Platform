package one.jpro.platform.image.manager.transformer;

import org.json.JSONObject;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * This class implements an image transformer that scales images to a target area
 * while preserving the original aspect ratio.
 *
 * @author Florian Kirmaier
 * @author Besmir Beqiri
 * @see ImageTransformer
 */
public class ImageTransformerScaleToArea implements ImageTransformer {


    private final int targetArea;

    /**
     * Constructs an instance with the specified target area.
     *
     * @param targetArea The desired area (in pixels) of the scaled image.
     * @throws IllegalArgumentException if targetArea is not positive.
     */
    public ImageTransformerScaleToArea(int targetArea) {
        if (targetArea <= 0) {
            throw new IllegalArgumentException("Target area must be positive.");
        }
        this.targetArea = targetArea;
    }

    /**
     * Constructs an instance with a specified target width and height. The target area
     * will be calculated as the product of targetWidth and targetHeight.
     *
     * @param targetWidth  The desired width of the scaled image.
     * @param targetHeight The desired height of the scaled image.
     */
    public ImageTransformerScaleToArea(int targetWidth, int targetHeight) {
        this(targetWidth * targetHeight);
    }

    /**
     * Transforms the given image, scaling it to the desired area while maintaining
     * its original aspect ratio.
     *
     * @param image The original image to be transformed.
     * @return The scaled image with the target area.
     */
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