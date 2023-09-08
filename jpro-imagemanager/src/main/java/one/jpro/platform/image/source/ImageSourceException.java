package one.jpro.platform.image.source;

/**
 * A runtime exception that is thrown when an error occurs during image source loading.
 *
 * @author Besmir Beqiri
 */
public class ImageSourceException extends RuntimeException {

    /**
     * Constructs a new image source exception with the specified message.
     *
     * @param message the exception message.
     */
    public ImageSourceException(String message) {
        super(message);
    }

    /**
     * Constructs a new image source exception with the specified message and cause.
     *
     * @param message the exception message.
     * @param cause   the cause of the exception.
     */
    public ImageSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
