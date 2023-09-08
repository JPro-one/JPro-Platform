package one.jpro.platform.image;

import javafx.scene.image.Image;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;

import one.jpro.platform.image.encoder.ImageEncoder;
import one.jpro.platform.image.source.ImageSource;
import one.jpro.platform.image.transformer.ImageTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the process of loading and caching images, allowing efficient retrieval and processing.
 *
 * @author Florian Kirmaier
 */
public class ImageManager {

    private static final Logger logger = LoggerFactory.getLogger(ImageManager.class);

    private static final String CACHE_DIR_NAME = "jpro.imagemanager.cache";
    private static final String CACHE_DIR_HOME = System.getProperty("user.home") + "/." + CACHE_DIR_NAME;
    private static String CACHE_DIR = null;
    private static volatile ImageManager defaultInstance;

    // Private constructor to prevent instantiation
    private ImageManager() {}

    /**
     * Returns the singleton instance of ImageManager.
     *
     * @return the single instance of ImageManager
     */
    public static ImageManager getInstance() {
        if (defaultInstance == null) {
            synchronized (ImageManager.class) {
                if (defaultInstance == null) {
                    // If `jpro.imagemanager.cache` is set, use that as cache directory
                    if (System.getProperty(CACHE_DIR_NAME) != null) {
                        CACHE_DIR = System.getProperty(CACHE_DIR_NAME);
                    } else {
                        CACHE_DIR = CACHE_DIR_HOME;
                    }
                    defaultInstance = new ImageManager();
                }
            }
        }
        return defaultInstance;
    }

    /**
     * Returns the cache directory as a File object.
     *
     * @return the cache directory
     */
    File getCacheDir() {
        return new File(CACHE_DIR);
    }

    /**
     * Loads the image based on the given definition, caches it, and returns the image result.
     *
     * @param imageDefinition the image definition containing source, transformation and encoder
     * @return the loaded and potentially cached image result
     */
    public ImageResult loadImage(ImageDefinition imageDefinition) {
        String origFileName = imageDefinition.getSource().getFileName();
        String baseName = origFileName.substring(0, origFileName.lastIndexOf("."));
        String fileName = baseName + "." + imageDefinition.getEncoder().getFileExtension();

        try {
            String hash = computeImageDefinitionHash(imageDefinition);
            File hashDir = new File(CACHE_DIR, hash);
            File imageFile = new File(hashDir, fileName);

            if (hashDir.exists()) {
                File keyFile = new File(hashDir, "key");
                if (keyFile.exists() && imageFile.exists()) {
                    String savedDef = Files.readString(keyFile.toPath());
                    if (savedDef.equals(imageDefinition.toJSON().toString())) {
                        String wh = Files.readString(new File(hashDir, "wh").toPath());
                        String[] dims = wh.split(",");
                        return new ImageResult(imageFile, Integer.parseInt(dims[0]), Integer.parseInt(dims[1]));
                    }
                }
            }

            BufferedImage img = imageDefinition.getSource().loadImage();
            img = imageDefinition.getTransformer().transform(img);
            imageDefinition.getEncoder().saveImage(img, imageFile);

            ImageResult result = new ImageResult(imageFile, img.getWidth(), img.getHeight());

            // Save metadata
            Files.write(new File(hashDir, "key").toPath(), imageDefinition.toJSON().toString().getBytes(StandardCharsets.UTF_8));
            Files.write(new File(hashDir, "wh").toPath(), (img.getWidth() + "," + img.getHeight()).getBytes(StandardCharsets.UTF_8));

            return result;
        } catch (IOException ex) {
            logger.error("Error while loading image", ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Loads the image asynchronously based on the given definition.
     *
     * @param imageDefinition the image definition containing source, transformation, and encoder
     * @return a CompletableFuture containing the image result
     */
    CompletableFuture<ImageResult> loadImageFuture(ImageDefinition imageDefinition) {
        return CompletableFuture.supplyAsync(() -> loadImage(imageDefinition));
    }

    /**
     * Loads and returns an FX image after applying the specified transformation and encoding.
     *
     * @param source         the image source
     * @param transformation the image transformer to apply
     * @param encoding       the image encoder to use
     * @return the processed FX image
     */
    Image loadFXImage(ImageSource source, ImageTransformer transformation, ImageEncoder encoding) {
        ImageDefinition def = new ImageDefinition(source, transformation, encoding);
        ImageResult result = loadImage(def);
        return result.toFXImage();
    }

    /**
     * Loads an JavaFX image asynchronously after applying the specified transformation and encoding.
     *
     * @param source      the image source
     * @param transformer the image transformer to apply
     * @param encoder     the image encoder to use
     * @return a CompletableFuture containing the processed FX image
     */
    CompletableFuture<Image> loadFXImageFuture(ImageSource source, ImageTransformer transformer, ImageEncoder encoder) {
        return CompletableFuture.supplyAsync(() -> loadFXImage(source, transformer, encoder));
    }

    /**
     * Computes the hash for the given image definition.
     *
     * @param imageDefinition the image definition
     * @return the computed MD5 hash as a string
     */
    private String computeImageDefinitionHash(ImageDefinition imageDefinition) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashBytes = digest.digest(imageDefinition.toJSON().toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException ex) {
            logger.error("Error computing MD5 hash", ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Clears the entire image cache.
     */
    void clearCache() {
        File cacheDirectory = new File(CACHE_DIR);
        deleteDirectoryRecursively(cacheDirectory);
    }

    /**
     * Recursively deletes the given directory or file.
     *
     * @param file the file or directory to delete
     */
    private void deleteDirectoryRecursively(File file) {
        if (file.isDirectory()) {
            final File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteDirectoryRecursively(child);
                }
            }
        }
        file.delete();
    }
}