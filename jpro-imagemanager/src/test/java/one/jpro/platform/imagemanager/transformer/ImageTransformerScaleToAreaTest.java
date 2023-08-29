package one.jpro.platform.imagemanager.transformer;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ImageTransformerScaleToAreaTest {

    @Test
    public void testTransform() throws IOException {
        BufferedImage testImage = ImageIO.read(new File("src/test/resources/testImage.png"));
        ImageTransformer transformer = new ImageTransformerScaleToArea(40000); // Area of 200x200

        BufferedImage transformedImage = transformer.transform(testImage);
        assertNotNull(transformedImage);

        int newArea = transformedImage.getWidth() * transformedImage.getHeight();
        // we need a tolerance of about 2% because of rounding errors
        assertEquals(40000, newArea, 800);
    }
}