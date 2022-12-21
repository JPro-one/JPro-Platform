package one.jpro.media.player.impl;

import java.util.Arrays;
import java.util.Optional;

/**
 * HTML Audio/Video readyState for the {@link WebMediaPlayer}.
 *
 * @author Besmir Beqiri
 */
public enum WebReadyState {

    HAVE_NOTHING(0, "no information whether or not the audio/video is ready"),
    HAVE_METADATA(1, "metadata for the audio/video is ready"),
    HAVE_CURRENT_DATA(2, "data for the current playback position is available, but not enough data to play next frame/millisecond"),
    HAVE_FUTURE_DATA(3, "data for the current and at least the next frame is available"),
    HAVE_ENOUGH_DATA(4, "enough data available to start playing");

    private final int code;
    private final String description;

    WebReadyState(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public final int getCode() {
        return code;
    }

    public final String getDescription() {
        return description;
    }

    public static Optional<WebReadyState> fromCode(int code) {
        return Arrays.stream(values()).filter(state -> state.getCode() == code).findFirst();
    }
}
