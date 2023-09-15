package one.jpro.platform.image;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javafx.application.Platform;
import javafx.scene.image.Image;
import one.jpro.platform.image.manager.ImageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.CountDownLatch;

public class ImageResultTest {

    @BeforeEach
    void setUp() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
            latch.await();
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void testToFXImage() {
        File testFile = new File("src/test/resources/testImage.png");
        ImageResult result = new ImageResult(testFile, 100, 100);
        Image fxImage = result.toFXImage();

        // The following assertions could be more detailed depending on the requirements
        assertEquals(100, fxImage.getWidth());
        assertEquals(100, fxImage.getHeight());
    }
}