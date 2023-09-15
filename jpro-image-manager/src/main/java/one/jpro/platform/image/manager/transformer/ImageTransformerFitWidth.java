package one.jpro.platform.image.manager.transformer;

import org.json.JSONObject;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * This class is responsible for transforming images such that they fit a specified width,
 * maintaining the original image's aspect ratio.
 *
 * @author Florian Kirmaier
 * @author Besmir Beqiri
 * @see ImageTransformer
 */
public class ImageTransformerFitWidth implements ImageTransformer {

    private final int targetWidth;

    /**
     * Constructs an instance that sets the target width for the transformation.
     *
     * @param targetWidth The desired width of the output image.
     */
    public ImageTransformerFitWidth(int targetWidth) {
        this.targetWidth = targetWidth;
    }

    /**
     * Constructs an instance that sets the target width based on a device pixel ratio.
     * This is useful for devices with different screen densities.
     *
     * @param targetWidth      The base width of the desired output image.
     * @param devicePixelRatio The pixel ratio of the target device.
     */
    public ImageTransformerFitWidth(int targetWidth, int devicePixelRatio) {
        this.targetWidth = targetWidth * devicePixelRatio;
    }

    /**
     * Transforms the input image to fit the specified target width while maintaining
     * its original aspect ratio.
     *
     * @param inputImage The input BufferedImage to be transformed.
     * @return The transformed BufferedImage that fits the specified target width.
     */
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