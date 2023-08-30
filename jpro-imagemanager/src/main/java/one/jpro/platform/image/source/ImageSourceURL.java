package one.jpro.platform.image.source;

import one.jpro.platform.image.Utils;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * This class provides an implementation of the ImageSource interface
 * for an image source fetched from a given URL.
 *
 * @author Florian Kirmaier
 * @author Besmir Beqiri
 * @see ImageSource
 */
public class ImageSourceURL implements ImageSource {

    private final URL url;

    public ImageSourceURL(URL url) {
        this.url = url;
    }

    @Override
    public BufferedImage loadImage() {
        try {
            return ImageIO.read(url);
        } catch (IOException ex) {
            throw new ImageSourceException("Error while reading image from URL: " + url, ex);
        }
    }

    @Override
    public long identityHashValue() {
        try {
            // First, get the modification date.
            URLConnection connection = url.openConnection();
            long lastModified = connection.getLastModified();

            // Combine the URL and modification date to generate the hash.
            String combined = url.toString() + lastModified;
            return Utils.computeHashValue(combined.getBytes());

        } catch (IOException e) {
            throw new ImageSourceException("Error while getting modification date for URL: " + url, e);
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", getClass().getSimpleName());
        json.put("url", Utils.escapeJson(url.toString()));
        return json;
    }

    @Override
    public String getFileName() {
        return url.toString().substring(url.toString().lastIndexOf('/') + 1);
    }
}
