package one.jpro.platform.image.manager.source;

import one.jpro.platform.image.manager.ImageUtils;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.nio.file.Files;
import java.io.IOException;

/**
 * Represents an image source based on a file.
 * This class is used to load images from local files and perform various operations
 * related to the image file, such as computing a hash value and converting its details to JSON.
 *
 * @author Florian Kirmaier
 * @author Besmir Beqiri
 * @see ImageSource
 */
public class ImageSourceFile implements ImageSource {

    private final File file;

    /**
     * Constructs an ImageSourceFile using a given File object.
     *
     * @param file The File object pointing to the image.
     */
    public ImageSourceFile(File file) {
        this.file = file;
    }

    /**
     * Constructs an ImageSourceFile using a file path.
     *
     * @param path The file path pointing to the image.
     */
    public ImageSourceFile(String path) {
        this.file = new File(path);
    }

    @Override
    public BufferedImage loadImage() {
        try {
            return ImageIO.read(file);
        } catch (IOException ex) {
            throw new ImageSourceException("Failed to load image from file: " + file.getAbsolutePath(), ex);
        }
    }

    @Override
    public long identityHashValue() {
        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            return ImageUtils.computeHashValue(fileBytes);
        } catch (IOException ex) {
            throw new ImageSourceException("Failed to compute hash value for the file: " + file.getAbsolutePath(), ex);
        }
    }

    @Override
    public String getFileName() {
        return file.getName();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", getClass().getSimpleName());
        json.put("path", ImageUtils.escapeJson(file.getAbsolutePath()));
        json.put("modified", file.lastModified());
        return json;
    }
}