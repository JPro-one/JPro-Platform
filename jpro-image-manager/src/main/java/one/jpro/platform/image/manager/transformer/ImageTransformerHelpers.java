package one.jpro.platform.image.manager.transformer;

import java.awt.*;

/**
 * Provides helper methods for image transformation tasks.
 *
 * @author Florian Kirmaier
 */
public class ImageTransformerHelpers {

    /**
     * Configures the provided Graphics2D object with default rendering settings.
     * These settings improve the quality of the rendered image with:
     * - Bilinear interpolation for smoother image scaling
     * - Anti-aliasing to smooth out jagged edges
     * - High-quality rendering hints for improved output quality
     *
     * @param g2d The Graphics2D object to be configured.
     */
    public static void graphicsDefaultConfiguration(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }
}
