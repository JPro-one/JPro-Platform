package one.jpro.media;

import com.jpro.webapi.WebAPI;

import java.util.Objects;

/**
 * Media source.
 *
 * @author Besmir Beqiri
 */
public record MediaSource(String source, boolean isLocal, WebAPI.JSFile jsFile) {

    public MediaSource {
        Objects.requireNonNull(source, "Source can not be null");
    }

    public MediaSource(String source) {
        this(source, true, null);
    }

    public MediaSource(WebAPI.JSFile jsFile) {
        this(jsFile.getObjectURL().getName(), false, jsFile);
    }
}
