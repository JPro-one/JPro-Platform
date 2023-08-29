package one.jpro.platform.imagemanager.transformer;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImageTransformerFitWidthTest {

    @Test
    public void testFitWidthTransformation() throws Exception {
        BufferedImage originalImage = ImageIO.read(new File("src/test/resources/testImage.png"));
        ImageTransformerFitWidth transformer = new ImageTransformerFitWidth(100);

        BufferedImage transformedImage = transformer.transform(originalImage);

        assertEquals(100, transformedImage.getWidth());
        assertEquals((originalImage.getHeight() * 100) / originalImage.getWidth(), transformedImage.getHeight());
    }
}