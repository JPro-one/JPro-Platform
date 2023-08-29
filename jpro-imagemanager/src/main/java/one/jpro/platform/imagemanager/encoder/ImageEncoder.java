package one.jpro.platform.imagemanager.encoder;

import one.jpro.platform.imagemanager.JsonStringConvertible;

import java.awt.image.BufferedImage;
import java.io.File;

public interface ImageEncoder extends JsonStringConvertible {
    void saveImage(BufferedImage image, File target);
    String fileExtension();
}