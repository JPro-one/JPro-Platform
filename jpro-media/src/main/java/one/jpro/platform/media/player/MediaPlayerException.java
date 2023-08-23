package one.jpro.platform.media.player;

import java.util.Optional;

/**
 * Media player exception.
 *
 * @author Besmir Beqiri
 */
public class MediaPlayerException extends RuntimeException {

    public MediaPlayerException(String message) {
        super(message);
    }

    public MediaPlayerException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String toString() {
        final StringBuilder errStrBuilder = new StringBuilder(getClass().getName() + " [");
        Optional.ofNullable(getMessage()).filter(message -> !message.isBlank())
                .ifPresent(message -> errStrBuilder.append("message: ").append(message));
        Optional.ofNullable(getCause()).ifPresent(cause -> errStrBuilder.append(", cause: ").append(cause));
        return errStrBuilder.append(']').toString();
    }
}
