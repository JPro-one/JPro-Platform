package one.jpro.platform.image.manager.transformer;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        JSONObject json = new JSONObject();
        json.put("type", "ImageTransformerWH");
        json.put("targetWidth", 200);
        json.put("targetHeight", 300);
        assertTrue(transformer.toJSON().similar(json));
    }
}