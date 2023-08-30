package one.jpro.platform.imagemanager;

import javafx.scene.image.Image;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;

import one.jpro.platform.imagemanager.encoder.ImageEncoder;
import one.jpro.platform.imagemanager.source.ImageSource;
import one.jpro.platform.imagemanager.transformer.ImageTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageManager {

    private static final Logger logger = LoggerFactory.getLogger(ImageManager.class);
    private static final String CACHE_DIR_HOME = System.getProperty("user.home") + "/.jproimageloader/imagecache";
    private static String CACHE_DIR = null;
    private static volatile ImageManager defaultInstance;

    private ImageManager() {}

    public static ImageManager getInstance() {
        if (defaultInstance == null) {
            synchronized (ImageManager.class) {
                if (defaultInstance == null) {
                    // If jpro.imagemanager.cache is set, use that as cache directory
                    if (System.getProperty("jpro.imagemanager.cache") != null) {
                        CACHE_DIR = System.getProperty("jpro.imagemanager.cache");
                    } else {
                        CACHE_DIR = CACHE_DIR_HOME;
                    }
                    defaultInstance = new ImageManager();
                }
            }
        }
        return defaultInstance;
    }

    File getCacheDir() {
        return new File(CACHE_DIR);
    }

    public ImageResult loadImage(ImageDefinition def) {
        String origFileName = def.getSource().fileName();
        String baseName = origFileName.substring(0, origFileName.lastIndexOf("."));
        String fileName = baseName + "." + def.getEncoder().fileExtension();

        try {
            String hash = computeImageDefinitionHash(def);
            File hashDir = new File(CACHE_DIR, hash);
            File imageFile = new File(hashDir, fileName);

            if (hashDir.exists()) {
                File keyFile = new File(hashDir, "key");
                if (keyFile.exists() && imageFile.exists()) {
                    String savedDef = Files.readString(keyFile.toPath());
                    if (savedDef.equals(def.toJSON().toString())) {
                        String wh = Files.readString(new File(hashDir, "wh").toPath());
                        String[] dims = wh.split(",");
                        return new ImageResult(imageFile, Integer.parseInt(dims[0]), Integer.parseInt(dims[1]));
                    }
                }
            }

            BufferedImage img = def.getSource().loadImage();
            img = def.getTransformer().transform(img);
            def.getEncoder().saveImage(img, imageFile);

            ImageResult result = new ImageResult(imageFile, img.getWidth(), img.getHeight());

            // Save metadata
            Files.write(new File(hashDir, "key").toPath(), def.toJSON().toString().getBytes(StandardCharsets.UTF_8));
            Files.write(new File(hashDir, "wh").toPath(), (img.getWidth() + "," + img.getHeight()).getBytes(StandardCharsets.UTF_8));

            return result;
        } catch (IOException ex) {
            logger.error("Error while loading image", ex);
            throw new RuntimeException(ex);
        }
    }

    CompletableFuture<ImageResult> loadImageFuture(ImageDefinition def) {
        return CompletableFuture.supplyAsync(() -> loadImage(def));
    }

    Image loadFXImage(ImageSource source, ImageTransformer transformation, ImageEncoder encoding) {
        ImageDefinition def = new ImageDefinition(source, transformation, encoding);
        ImageResult result = loadImage(def);
        return result.toFXImage();
    }

    CompletableFuture<Image> loadFXImageFuture(ImageSource source, ImageTransformer transformer, ImageEncoder encoder) {
        return CompletableFuture.supplyAsync(() -> loadFXImage(source, transformer, encoder));
    }

    private String computeImageDefinitionHash(ImageDefinition def) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashBytes = digest.digest(def.toJSON().toString().getBytes(StandardCharsets.UTF_8));
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

    void clearCache() {
        File cacheDirectory = new File(CACHE_DIR);
        deleteDirectoryRecursively(cacheDirectory);
    }

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