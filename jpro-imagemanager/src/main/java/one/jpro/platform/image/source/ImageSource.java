package one.jpro.platform.image.source;

import one.jpro.platform.image.JsonConvertible;
import java.awt.image.BufferedImage;

public interface ImageSource extends JsonConvertible {

    BufferedImage loadImage();

    long identityHashValue();

    String fileName();
}
