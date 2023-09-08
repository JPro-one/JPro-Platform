package one.jpro.platform.image.encoder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class ImageEncoderPNGTest {

    private ImageEncoderPNG encoder;

    @BeforeEach
    public void setup() {
        encoder = new ImageEncoderPNG();
    }

    @Test
    public void testSaveImage() throws Exception {
        BufferedImage testImage = ImageIO.read(new File("src/test/resources/testImage.png"));
        File outputFile = new File("src/test/resources/testOutputImage.png");

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
        assertEquals("png", encoder.getFileExtension());
    }
}