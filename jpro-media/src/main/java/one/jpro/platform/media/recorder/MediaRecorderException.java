package one.jpro.platform.media.recorder;

import org.json.JSONObject;

import java.util.Optional;

/**
 * Media recorder exception.
 *
 * @author Besmir Beqiri
 */
public class MediaRecorderException extends RuntimeException {

    private final String code;

    /**
     * Create an {@link MediaRecorderException} with the given message and cause.
     *
     * @param message the error message
     * @param cause a {@link Throwable} object
     */
    public MediaRecorderException(String message, Throwable cause) {
        super(message, cause);
        this.code = "0";
    }

    /**
     * Create an {@link MediaRecorderException} with the given error type and message.
     *
     * @param code the error code
     * @param message the error message
     */
    public MediaRecorderException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Create an {@link MediaRecorderException} with the given error type, message and cause.
     *
     * @param code the error code
     * @param message the error message
     * @param cause a {@link Throwable} object
     */
    public MediaRecorderException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * Return the error's code.
     *
     * @return a string value
     */
    public final String getCode() {
        return code;
    }

    @Override
    public String toString() {
        final StringBuilder errStrBuilder = new StringBuilder(getClass().getName() + " [code: " + code);
        Optional.ofNullable(getMessage()).filter(message -> !message.isBlank())
                .ifPresent(message -> errStrBuilder.append(", message: ").append(message));
        Optional.ofNullable(getCause()).ifPresent(cause -> errStrBuilder.append(", cause: ").append(cause));
        return errStrBuilder.append(']').toString();
    }

    public static MediaRecorderException fromJSON(String source) {
        if (source == null || source.length() <= 2) {
            throw new IllegalArgumentException("Source string cannot be null or empty");
        }

        if (source.startsWith("\"") && source.endsWith("\"")) { // a string from JSON.stringify()
            source = source.substring(1, source.length() - 1).replace("\\", "");
        }

        JSONObject json = new JSONObject(source);
        final String code = json.getString("code");
        final String message = json.getString("message");
        return new MediaRecorderException(code, message);
    }
}
