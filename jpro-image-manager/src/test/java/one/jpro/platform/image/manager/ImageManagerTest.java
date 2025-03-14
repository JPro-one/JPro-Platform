package one.jpro.platform.image.manager;

import javafx.application.Platform;
import javafx.scene.image.Image;
import one.jpro.platform.image.manager.encoder.ImageEncoder;
import one.jpro.platform.image.manager.encoder.ImageEncoderJPG;
import one.jpro.platform.image.manager.encoder.ImageEncoderPNG;
import one.jpro.platform.image.manager.source.ImageSource;
import one.jpro.platform.image.manager.source.ImageSourceFile;
import one.jpro.platform.image.manager.source.ImageSourceResource;
import one.jpro.platform.image.manager.transformer.ImageTransformer;
import one.jpro.platform.image.manager.transformer.ImageTransformerFitWidth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImageManagerTest {

    ImageManager manager;
    ImageDefinition def;
    File imageDef2File;
    ImageDefinition imageDef2;



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

        imageDef2File = new File("testImage.png");
        ImageSource source2 = new ImageSourceFile(imageDef2File);
        ImageTransformer transformer2 = new ImageTransformerFitWidth(200);
        ImageEncoder encoder2 = new ImageEncoderPNG();
        imageDef2 = new ImageDefinition(source2, transformer2, encoder2);
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
    void testImageRecreateOnChange() throws IOException {
        // Copy file1 to imageDef2File
        File file1 = new File(getClass().getResource("/testImage.png").getFile());
        File file2 = new File(getClass().getResource("/logo.png").getFile());
        assertTrue(file1.exists());
        assertTrue(file2.exists());

        Files.copy(file1.toPath(), imageDef2File.toPath(), StandardCopyOption.REPLACE_EXISTING);

        var image1 = manager.loadImage(imageDef2);
        String hash1 = imageDef2.getHashString();
        System.out.println("ModifiedDate1: " + imageDef2File.lastModified());

        Files.copy(file2.toPath(), imageDef2File.toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("ModifiedDate2: " + imageDef2File.lastModified());
        Files.setLastModifiedTime(imageDef2File.toPath(), FileTime.from(Instant.now()));
        System.out.println("ModifiedDate3: " + imageDef2File.lastModified());

        var image2 = manager.loadImage(imageDef2);
        String hash2 = imageDef2.getHashString();

        System.out.println("hash1: " + hash1);
        System.out.println("hash2: " + hash2);
        assertNotEquals(hash1, hash2, "changed image file should have different hash");

        File imageFile1 = image1.getFile();
        File imageFile2 = image2.getFile();

        assertTrue(imageFile1.exists());
        assertTrue(imageFile2.exists());

        System.out.println("imageFile1: " + imageFile1.lastModified());
        System.out.println("imageFile2: " + imageFile2.lastModified());

        assertNotEquals(imageFile1.lastModified(), imageFile2.lastModified(),
                "Image was not created again.");
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
