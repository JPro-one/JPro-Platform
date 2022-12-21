package one.jpro.media.player.impl;

import java.util.Arrays;
import java.util.Optional;

/**
 * HTML Audio/Video error codes for the {@link WebMediaPlayer}.
 *
 * @author Besmir Beqiri
 */
public enum WebMediaError {

    /**
     * Fetching process aborted by user.
     */
    MEDIA_ERR_ABORTED(0, "fetching process aborted by user"),

    /**
     * Network error occurred while fetching the media.
     */
    MEDIA_ERR_NETWORK(1, "error occurred when downloading"),

    /**
     * Decoding failed.
     */
    MEDIA_ERR_DECODE(2, "error occurred when decoding"),

    /**
     * Unsupported video/audio format
     */
    MEDIA_ERR_SRC_NOT_SUPPORTED(3, "audio/video not supported");

    private final int code;
    private final String description;

    WebMediaError(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static Optional<WebMediaError> fromCode(int code) {
        return Arrays.stream(values()).filter(error -> error.getCode() == code).findFirst();
    }
}
