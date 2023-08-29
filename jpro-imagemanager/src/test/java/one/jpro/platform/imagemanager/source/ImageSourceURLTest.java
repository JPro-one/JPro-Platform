package one.jpro.platform.imagemanager.source;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

public class ImageSourceURLTest {
    @Test
    public void testLoadImageFromURL() {
        URL testURL = this.getClass().getResource("/testImage.png");
        ImageSourceURL source = new ImageSourceURL(testURL);

        BufferedImage image = source.loadImage();
        assertNotNull(image, "Image should be loaded from URL");
    }

    @Test
    public void testIdentityHashValue() {
        URL testURL = this.getClass().getResource("/testImage.png");
        ImageSourceURL source = new ImageSourceURL(testURL);

        long hashValue = source.identityHashValue();
        assertNotEquals(0, hashValue, "Hash value should be generated from URL");
    }

    @Test
    public void testJsonSerialization() {
        URL testURL = this.getClass().getResource("/testImage.png");
        ImageSourceURL source = new ImageSourceURL(testURL);

        String json = source.toJson();
        assertTrue(json.contains(testURL.toString()), "JSON representation should contain the URL");
    }
}