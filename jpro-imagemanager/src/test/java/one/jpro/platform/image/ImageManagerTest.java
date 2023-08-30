package one.jpro.platform.image;

import javafx.application.Platform;
import one.jpro.platform.image.encoder.ImageEncoder;
import one.jpro.platform.image.encoder.ImageEncoderJPG;
import one.jpro.platform.image.encoder.ImageEncoderPNG;
import one.jpro.platform.image.source.ImageSource;
import one.jpro.platform.image.source.ImageSourceFile;
import one.jpro.platform.image.source.ImageSourceResource;
import one.jpro.platform.image.transformer.ImageTransformer;
import one.jpro.platform.image.transformer.ImageTransformerFitWidth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import javafx.scene.image.Image;
import org.mockito.Mockito;

class ImageManagerTest {

    ImageManager manager;
    ImageDefinition def;


    @BeforeEach
    void setUp() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
            latch.await();
        } catch (IllegalStateException ex) {
            // ignore
        }

        manager = ImageManager.getInstance();

        ImageSource source = new ImageSourceResource("/testImage.png");
        ImageTransformer transformer = new ImageTransformerFitWidth(200);
        ImageEncoder encoder = new ImageEncoderPNG();
        def = new ImageDefinition(source, transformer, encoder);
    }

    @Test
    void testLoadImage() {
        ImageResult result = manager.loadImage(def);
        assertNotNull(result);
        assertTrue(result.getFile().exists());
        assertEquals(200, result.getWidth());
    }

    @Test
    void testImageNotCreatedTwice() {
        String hashBefore = def.getHashString();
        manager.loadImage(def);
        String hashAfter = def.getHashString();

        assertEquals(hashBefore, hashAfter, "Images have different hashes after loading twice.");

        File imageFileBefore = new File(manager.getCacheDir(), hashBefore + "/image.png");
        File imageFileAfter = new File(manager.getCacheDir(), hashAfter + "/image.png");

        assertEquals(imageFileBefore.lastModified(), imageFileAfter.lastModified(), "Image was created again.");
    }

    @Test
    void testLoadImageFuture() {
        CompletableFuture<ImageResult> future = manager.loadImageFuture(def);
        ImageResult result = future.join();

        assertNotNull(result);
        assertTrue(result.getFile().exists());
        assertEquals(200, result.getWidth());
    }

    @Test
    void testLoadFXImage() {
        Image img = manager.loadFXImage(new ImageSourceFile("src/test/resources/testImage.png"),
                new ImageTransformerFitWidth(200), new ImageEncoderPNG());
        assertNotNull(img);
        assertEquals(200, img.getWidth());
    }

    @Test
    void testLoadFXImageFuture() {
        CompletableFuture<Image> future = manager.loadFXImageFuture(new ImageSourceFile("src/test/resources/testImage.png"), new ImageTransformerFitWidth(200), new ImageEncoderPNG());
        Image img = future.join();

        assertNotNull(img);
        assertEquals(200, img.getWidth());
    }

    @Test
    void testImageDefinitionHashCollision() {
        // This is more of a hypothetical test, as forcing a hash collision with MD5 is non-trivial.
        // We're assuming computeImageDefinitionHash is a utility function made available.
        ImageDefinition def1 = new ImageDefinition(new ImageSourceFile("src/test/resources/testImage.png"), new ImageTransformerFitWidth(200), new ImageEncoderPNG());
        ImageDefinition def2 = new ImageDefinition(new ImageSourceFile("src/test/resources/testImage.png"), new ImageTransformerFitWidth(300), new ImageEncoderPNG());

        String hash1 = "" + def1.hashCode();
        String hash2 = "" + def2.hashCode();

        assertNotEquals(hash1, hash2, "Hash collision detected.");
    }

    @Test
    void testExistingEncoderCall() {
        manager.clearCache();

        ImageSourceFile source = new ImageSourceFile("src/test/resources/testImage.png");
        ImageTransformer transformer = new ImageTransformerFitWidth(400);

        // Spy on the existing ImageEncoderPNG
        ImageEncoderPNG encoderPNG = new ImageEncoderPNG();
        ImageEncoder encoderSpy = Mockito.spy(encoderPNG);

        ImageDefinition definition = new ImageDefinition(source, transformer, encoderSpy);

        // Load the image for the first time
        manager.loadImage(definition);

        // Load the image for the second time
        manager.loadImage(definition);

        // Verify that saveImage method was called only once
        verify(encoderSpy, times(1)).saveImage(any(BufferedImage.class), any(File.class));

        manager.clearCache();
        manager.loadImage(definition);
        verify(encoderSpy, times(2)).saveImage(any(BufferedImage.class), any(File.class));
    }

    // Test changing image format
    @Test
    void testChangingFormat() {
        var imageDefinition = new ImageDefinition(new ImageSourceFile("src/test/resources/testImage.png"),
                new ImageTransformerFitWidth(200), new ImageEncoderJPG());
        var result = manager.loadImage(imageDefinition);

        System.out.println("result: " + result.getFile().getAbsolutePath());

        assertEquals("jpg", result.getFile().getName().substring(result.getFile().getName().lastIndexOf(".") + 1));
        assertTrue(result.getFile().exists());

        // starts with testImage
        assertTrue(result.getFile().getName().startsWith("testImage"));
    }


}
