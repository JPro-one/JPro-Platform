package one.jpro.platform.imagemanager.transformer;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImageTransformerFitHeightTest {

    @Test
    public void testTransform() throws IOException {
        File testImageFile = new File("src/test/resources/testImage.png");
        BufferedImage originalImage = ImageIO.read(testImageFile);

        ImageTransformer transformer = new ImageTransformerFitHeight(500);
        BufferedImage transformedImage = transformer.transform(originalImage);

        assertEquals(500, transformedImage.getHeight());
    }

    @Test
    public void testToJson() {
        ImageTransformer transformer = new ImageTransformerFitHeight(500);
        String json = transformer.toJSON();
        assertEquals("{ \"type\": \"fitHeight\", \"targetHeight\": 500 }", json);
    }
}