package one.jpro.platform.image;

import com.jpro.webapi.WebAPI;
import javafx.scene.image.Image;

import java.io.File;

public class ImageResult {

    private final File file;
    private final int width;
    private final int height;

    public ImageResult(File file, int width, int height) {
        this.file = file;
        this.width = width;
        this.height = height;
    }

    public File getFile() {
        return file;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Image toFXImage() {
        if (WebAPI.isBrowser()) {
            return WebAPI.createVirtualImage(file.toURI().toString(), width, height);
        } else {
            return new Image(file.toURI().toString(), width, height, false, true);
        }
    }
}