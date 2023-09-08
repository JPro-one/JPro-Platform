package one.jpro.platform.image.source;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

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
    void testToJson_returnsExpectedJson() {
        ImageSourceResource resource = new ImageSourceResource("/testImage.png");

        JSONObject json = new JSONObject();
        json.put("type", "ImageSourceResource");
        json.put("resourcePath", "/testImage.png");
        assertTrue(resource.toJSON().similar(json));
    }
}