package one.jpro.platform.image.source;

import one.jpro.platform.image.ImageUtils;
import org.json.JSONObject;
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
        final File testImageFile = new File("src/test/resources/testImage.png");
        final ImageSourceFile imageSource = new ImageSourceFile(testImageFile);

        JSONObject json = new JSONObject();
        json.put("type", "ImageSourceFile");
        json.put("path", ImageUtils.escapeJson(testImageFile.getAbsolutePath()));

        assertTrue(imageSource.toJSON().similar(json));
    }
}