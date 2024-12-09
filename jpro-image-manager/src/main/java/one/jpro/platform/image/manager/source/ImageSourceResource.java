package one.jpro.platform.image.manager.source;

import one.jpro.platform.image.manager.ImageUtils;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * This class provides an implementation of the ImageSource interface
 * for image resources available on the classpath.
 *
 * @author Florian Kirmaier
 * @author Besmir Beqiri
 * @see ImageSource
 */
public class ImageSourceResource implements ImageSource {

    private final String resourcePath;

    /**
     * Creates a new instance with the given resource path.
     *
     * @param resourcePath the path to the image resource.
     */
    public ImageSourceResource(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @Override
    public BufferedImage loadImage() {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new ImageSourceException("Resource not found: " + resourcePath);
            }
            return ImageIO.read(is);
        } catch (Exception e) {
            throw new ImageSourceException("Error loading resource: " + resourcePath, e);
        }
    }

    @Override
    public long identityHashValue() {
        try {
            URL resourceUrl = getClass().getResource(resourcePath);
            if (resourceUrl == null) {
                throw new ImageSourceException("Resource not found: " + resourcePath);
            }
            URLConnection conn = resourceUrl.openConnection();
            long lastModified = conn.getLastModified();
            if (lastModified == 0) { // fallback to binary data hash
                byte[] resourceData = Files.readAllBytes(Paths.get(resourceUrl.toURI()));
                return ImageUtils.computeHashValue(resourceData);
            }
            return lastModified;
        } catch (Exception ex) {
            throw new ImageSourceException("Error obtaining modification date for resource: " + resourcePath, ex);
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", getClass().getSimpleName());
        json.put("resourcePath", ImageUtils.escapeJson(resourcePath));
        // get last modified date
        // It's important, so the images get recreated, when the files in the jar are updated
        try {
            URL resourceUrl = getClass().getResource(resourcePath);
            if (resourceUrl != null) {
                URLConnection conn = resourceUrl.openConnection();
                json.put("modified", conn.getLastModified());
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error obtaining modification date for resource: " + resourcePath, ex);
        }
        return json;
    }

    @Override
    public String getFileName() {
        return resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
    }
}