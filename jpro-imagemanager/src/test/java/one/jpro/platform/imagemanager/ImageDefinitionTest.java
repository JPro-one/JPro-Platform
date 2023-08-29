package one.jpro.platform.imagemanager;

import one.jpro.platform.imagemanager.encoder.ImageEncoder;
import one.jpro.platform.imagemanager.encoder.ImageEncoderPNG;
import one.jpro.platform.imagemanager.source.ImageSource;
import one.jpro.platform.imagemanager.source.ImageSourceFile;
import one.jpro.platform.imagemanager.transformer.ImageTransformer;
import one.jpro.platform.imagemanager.transformer.ImageTransformerFitWidth;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class ImageDefinitionTest {

    @Test
    void testToJson() {
        ImageSource source = new ImageSourceFile(new File("path/to/file.png"));
        ImageTransformer transformer = new ImageTransformerFitWidth(500);
        ImageEncoder encoding = new ImageEncoderPNG();

        ImageDefinition definition = new ImageDefinition(source, transformer, encoding);
        // got
        String json = definition.toJSON();
        // check whether it's valid JSON
        try {
            new JSONObject(json);
        } catch (JSONException e) {
            fail("Invalid JSON format, got: " + json);
        }
    }

}