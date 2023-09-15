package one.jpro.platform.image.manager;

import com.jpro.webapi.WebAPI;
import javafx.scene.image.Image;

import java.io.File;

/**
 * Represents the result of an image operation, encapsulating details about
 * the image file and its dimensions (width and height).
 *
 * @author Florian Kirmaier
 */
public class ImageResult {

    /**
     * The image file.
     */
    private final File file;

    /**
     * The width of the image in pixels.
     */
    private final int width;

    /**
     * The height of the image in pixels.
     */
    private final int height;

    /**
     * Constructs a new instance.
     *
     * @param file   The image file.
     * @param width  The width of the image in pixels.
     * @param height The height of the image in pixels.
     */
    public ImageResult(File file, int width, int height) {
        this.file = file;
        this.width = width;
        this.height = height;
    }

    /**
     * Returns the image file associated with this result.
     *
     * @return The {@link File} instance representing the image file.
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns the width of the image in pixels.
     *
     * @return The width of the image.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height of the image in pixels.
     *
     * @return The height of the image.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Converts this image result into an {@link Image} suitable for use within JavaFX applications.
     * If the code is running in a browser context (as determined by the WebAPI), a virtual image is created instead.
     * Otherwise, a standard JavaFX Image object is constructed using the file's URI.
     *
     * @return A {@link Image} instance representing the image in a format suitable.
     */
    public Image toFXImage() {
        if (WebAPI.isBrowser()) {
            return WebAPI.createVirtualImage(file.toURI().toString(), width, height);
        } else {
            return new Image(file.toURI().toString(), width, height, false, true);
        }
    }
}