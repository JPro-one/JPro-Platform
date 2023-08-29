package one.jpro.platform.imagemanager.transformer;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImageTransformerWHTest {

    @Test
    public void testImageTransformerWH() throws Exception {
        BufferedImage originalImage = ImageIO.read(new File("src/test/resources/testImage.png"));
        ImageTransformer transformer = new ImageTransformerWH(200, 300);
        BufferedImage transformedImage = transformer.transform(originalImage);

        assertEquals(200, transformedImage.getWidth());
        assertEquals(300, transformedImage.getHeight());
    }

    @Test
    public void testToJson() {
        ImageTransformer transformer = new ImageTransformerWH(200, 300);
        String json = transformer.toJSON();
        assertEquals("{\"type\":\"ImageTransformerWH\",\"targetWidth\":200,\"targetHeight\":300}", json);
    }
}