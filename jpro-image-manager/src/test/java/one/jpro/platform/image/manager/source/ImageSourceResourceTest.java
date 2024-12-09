package one.jpro.platform.image.manager.source;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.URLConnection;

import static org.junit.jupiter.api.Assertions.*;

class ImageSourceResourceTest {

    @Test
    void testLoadImage_validResourcePath_imageLoadedSuccessfully() {
        ImageSourceResource resource = new ImageSourceResource("/testImage.png");
        BufferedImage image = resource.loadImage();
        assertNotNull(image);
    }

    @Test
    void testLoadImage_invalidResourcePath_throwsException() {
        ImageSourceResource resource = new ImageSourceResource("/invalidPath.png");
        assertThrows(RuntimeException.class, resource::loadImage);
    }

    @Test
    void testIdentityHashValue_validResourcePath_returnsHash() {
        ImageSourceResource resource = new ImageSourceResource("/testImage.png");
        long hash = resource.identityHashValue();
        assertNotEquals(0, hash);
    }

    @Test
    void testToJson_returnsExpectedJson() throws Exception {
        ImageSourceResource resource = new ImageSourceResource("/testImage.png");

        JSONObject json = new JSONObject();
        json.put("type", "ImageSourceResource");
        json.put("resourcePath", "/testImage.png");
        URL resourceUrl = getClass().getResource("/testImage.png");
        if (resourceUrl != null) {
            // we don't access any stream here, so we don't need to close it
            URLConnection conn = resourceUrl.openConnection();
            json.put("modified", conn.getLastModified());
        }
        assertTrue(resource.toJSON().similar(json));
    }
}