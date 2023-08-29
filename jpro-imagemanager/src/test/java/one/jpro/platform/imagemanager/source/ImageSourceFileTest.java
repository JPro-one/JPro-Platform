package one.jpro.platform.imagemanager.source;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.File;

public class ImageSourceFileTest {

    @Test
    public void testLoadImage() {
        File testImageFile = new File("src/test/resources/testImage.png");
        ImageSourceFile imageSource = new ImageSourceFile(testImageFile);

        BufferedImage image = imageSource.loadImage();
        assertNotNull(image);
    }

    @Test
    public void testIdentityHashValue() {
        File testImageFile = new File("src/test/resources/testImage.png");
        ImageSourceFile imageSource = new ImageSourceFile(testImageFile);

        long hashValue = imageSource.identityHashValue();
        assertTrue(hashValue != 0);
    }

    @Test
    public void testToJson() {
        File testImageFile = new File("src/test/resources/testImage.png");
        ImageSourceFile imageSource = new ImageSourceFile(testImageFile);

        String json = imageSource.toJSON();
        assertTrue(json.contains("ImageSourceFile"));
        assertTrue(json.contains(testImageFile.getAbsolutePath()));
    }
}