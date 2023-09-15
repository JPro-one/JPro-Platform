package one.jpro.platform.image.manager.encoder;

/**
 * A runtime exception that is thrown when an error occurs during image encoding.
 *
 * @author Besmir Beqiri
 */
public class ImageEncoderException extends RuntimeException {

    /**
     * Constructs a new image encoder exception with the specified message.
     *
     * @param message the exception message.
     */
    public ImageEncoderException(String message) {
        super(message);
    }

    /**
     * Constructs a new image encoder exception with the specified message and cause.
     *
     * @param message the exception message.
     * @param cause   the cause of the exception.
     */
    public ImageEncoderException(String message, Throwable cause) {
        super(message, cause);
    }
}
