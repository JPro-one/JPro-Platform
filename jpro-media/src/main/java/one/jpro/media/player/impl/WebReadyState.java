package one.jpro.media.player.impl;

import java.util.Arrays;
import java.util.Optional;

/**
 * HTML Audio/Video readyState for the {@link WebMediaPlayer}.
 *
 * @author Besmir Beqiri
 */
public enum WebReadyState {

    /**
     * No information is available about the media resource.
     */
    HAVE_NOTHING(0, "no information whether or not the audio/video is ready"),

    /**
     * Enough of the media resource has been retrieved that the metadata attributes are initialized.
     */
    HAVE_METADATA(1, "metadata for the audio/video is ready"),

    /**
     * Data is available for the current playback position, but not enough to actually play more than one frame.
     */
    HAVE_CURRENT_DATA(2, "data for the current playback position is available, but not enough data to play next frame/millisecond"),

    /**
     * Data for the current playback position and at least the next frame is available.
     */
    HAVE_FUTURE_DATA(3, "data for the current and at least the next frame is available"),

    /**
     * Enough data available to start playing.
     */
    HAVE_ENOUGH_DATA(4, "enough data available to start playing");

    private final int code;
    private final String description;

    WebReadyState(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * The code related to this ready state.
     *
     * @return the code
     */
    public final int getCode() {
        return code;
    }

    /**
     * The description of this ready state.
     *
     * @return the description
     */
    public final String getDescription() {
        return description;
    }

    /**
     * Returns the ready state for the given code.
     *
     * @param code the code
     * @return an optional {@link WebReadyState} object
     */
    public static Optional<WebReadyState> fromCode(int code) {
        return Arrays.stream(values()).filter(state -> state.getCode() == code).findFirst();
    }
}
