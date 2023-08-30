package one.jpro.platform.imagemanager.source;

import one.jpro.platform.imagemanager.JsonConvertible;
import java.awt.image.BufferedImage;

public interface ImageSource extends JsonConvertible {

    BufferedImage loadImage();

    long identityHashValue();

    String fileName();
}
