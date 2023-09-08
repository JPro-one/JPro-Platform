package one.jpro.platform.image.transformer;

import org.json.JSONObject;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;

/**
 * Implements the ImageTransformer interface to provide functionalities
 * for transforming images based on specified width and height.
 *
 * @author Florian Kirmaier
 * @author Besmir Beqiri
 * @see ImageTransformer
 */
public class ImageTransformerWH implements ImageTransformer {

    private final int targetWidth;
    private final int targetHeight;

    /**
     * Constructs an instance of ImageTransformerWH with specified target width and height.
     *
     * @param targetWidth  The desired width of the transformed image.
     * @param targetHeight The desired height of the transformed image.
     */
    public ImageTransformerWH(int targetWidth, int targetHeight) {
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
    }

    /**
     * Constructs an instance of ImageTransformerWH with a specified target width and height,
     * and scales them by the provided ratio.
     *
     * @param targetWidth  The initial width value before scaling.
     * @param targetHeight The initial height value before scaling.
     * @param ratio        The scaling factor for width and height.
     */
    public ImageTransformerWH(int targetWidth, int targetHeight, double ratio) {
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