package one.jpro.platform.image;

import one.jpro.platform.image.manager.ImageDefinition;
import one.jpro.platform.image.manager.encoder.ImageEncoder;
import one.jpro.platform.image.manager.encoder.ImageEncoderPNG;
import one.jpro.platform.image.manager.source.ImageSource;
import one.jpro.platform.image.manager.source.ImageSourceFile;
import one.jpro.platform.image.manager.transformer.ImageTransformer;
import one.jpro.platform.image.manager.transformer.ImageTransformerFitWidth;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageDefinitionTest {

    @Test
    void testToJson() {
        ImageSource source = new ImageSourceFile(new File("path/to/file.png"));
        ImageTransformer transformer = new ImageTransformerFitWidth(500);
        ImageEncoder encoder = new ImageEncoderPNG();

        ImageDefinition definition = new ImageDefinition(source, transformer, encoder);

        JSONObject json = new JSONObject();
        json.put("source", source.toJSON());
        json.put("transformer", transformer.toJSON());
        json.put("encoder", encoder.toJSON());

        assertTrue(definition.toJSON().similar(json));
    }

}