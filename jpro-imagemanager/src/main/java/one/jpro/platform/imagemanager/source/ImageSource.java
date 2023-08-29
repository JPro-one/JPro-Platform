package one.jpro.platform.imagemanager.source;

import one.jpro.platform.imagemanager.JsonStringConvertible;
import java.awt.image.BufferedImage;

public interface ImageSource extends JsonStringConvertible {

    BufferedImage loadImage();

    long identityHashValue();

    String fileName();
}
