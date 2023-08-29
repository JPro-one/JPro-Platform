package one.jpro.platform.imagemanager.source;

import one.jpro.platform.imagemanager.Utils;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.nio.file.Files;
import java.io.IOException;

public class ImageSourceFile implements ImageSource {

    private final File file;

    public ImageSourceFile(File file) {
        this.file = file;
    }

    public ImageSourceFile(String path) {
        this.file = new File(path);
    }

    @Override
    public BufferedImage loadImage() {
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load image from file", e);
        }
    }

    @Override
    public long identityHashValue() {
        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            return Utils.computeHashValue(fileBytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to compute hash value for the file", e);
        }
    }

    @Override
    public String toJSON() {
        return "{\"type\":\"ImageSourceFile\", \"path\":\"" + Utils.escapeJson(file.getAbsolutePath()) + "\"}";
    }

    @Override
    public String fileName() {
        return file.getName();
    }
}