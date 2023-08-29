package one.jpro.platform.imagemanager.source;

import org.junit.jupiter.api.*;

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
        String json = resource.toJson();
        assertEquals("{\"type\":\"ImageSourceResource\",\"resourcePath\":\"/testImage.png\"}", json);
    }
}