package one.jpro.platform.image.manager.encoder;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class ImageEncoderJPGTest {

    @Test
    public void testSaveImage() {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ImageEncoderJPG encoder = new ImageEncoderJPG(1.0);

        File tempFile;
        try {
            tempFile = File.createTempFile("test", ".jpg");
            encoder.saveImage(image, tempFile);
            assertTrue(tempFile.exists());
            assertTrue(tempFile.length() > 0);
        } catch (Exception e) {
            fail("Failed to create temp file or save image", e);
        }
    }

    @Test
    public void testSaveImage2() throws Exception {
        BufferedImage testImage = ImageIO.read(new File("src/test/resources/logo.png"));
        System.out.println("testImage: " + testImage.getWidth() + "x" + testImage.getHeight());
        File outputFile = File.createTempFile("image", ".jpg");
        System.out.println("testOutputImage: " + outputFile.getAbsolutePath());

        ImageEncoder encoder = new ImageEncoderPNG();
        encoder.saveImage(testImage, outputFile);

        assertTrue(outputFile.exists());

        BufferedImage loadedOutput = ImageIO.read(outputFile);
        assertEquals(testImage.getWidth(), loadedOutput.getWidth());
        assertEquals(testImage.getHeight(), loadedOutput.getHeight());

        // Cleanup
        outputFile.delete();
    }

    @Test
    public void testFileExtension() {
        ImageEncoderJPG encoder = new ImageEncoderJPG(0.5);
        assertEquals("jpg", encoder.getFileExtension());
    }

    @Test
    public void testToJson() {
        ImageEncoderJPG encoder = new ImageEncoderJPG(0.5);

        JSONObject json = new JSONObject();
        json.put("type", ImageEncoderJPG.class.getSimpleName());
        json.put("quality", 0.5);
        json.put("fileExtension", "jpg");
        assertTrue(encoder.toJSON().similar(json));
    }
}