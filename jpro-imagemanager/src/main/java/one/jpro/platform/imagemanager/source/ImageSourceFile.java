package one.jpro.platform.imagemanager.source;

import one.jpro.platform.imagemanager.Utils;
import org.json.JSONObject;

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
        } catch (IOException ex) {
            throw new ImageSourceException("Failed to load image from file", ex);
        }
    }

    @Override
    public long identityHashValue() {
        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            return Utils.computeHashValue(fileBytes);
        } catch (IOException ex) {
            throw new ImageSourceException("Failed to compute hash value for the file", ex);
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", getClass().getSimpleName());
        json.put("path", Utils.escapeJson(file.getAbsolutePath()));
        return json;
    }

    @Override
    public String fileName() {
        return file.getName();
    }
}