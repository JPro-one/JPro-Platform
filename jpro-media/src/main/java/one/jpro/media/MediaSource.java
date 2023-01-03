package one.jpro.media;

import com.jpro.webapi.WebAPI;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import one.jpro.media.recorder.MediaRecorder;

import java.net.URI;
import java.util.Objects;

/**
 * The <code>MediaSource</code> class represents a media resource.
 * It is instantiated from the string form of a source {@link URI}
 * or {@link WebAPI.JSFile} depending on whenever the resource is
 * available locally, on the JPro server or in the browser's client.
 * <p>The same <code>MediaSource</code> object may be shared among multiple
 * <code>MediaPlayer</code> or <code>MediaRecorder</code> objects.
 *
 * @param source string form of a media source URI
 * @param isLocal <code>true</code> if this media source is local when running
 *                on a desktop/mobile or as a local resource to JPro server,
 *                otherwise <code>false</code> when inside the client's browser
 * @param jsFile the non-local media source located inside the client's browser
 *
 * @see MediaPlayer
 * @see MediaRecorder
 * @see MediaException
 *
 * @author Besmir Beqiri
 */
public record MediaSource(String source, boolean isLocal, WebAPI.JSFile jsFile) {

    public MediaSource {
        Objects.requireNonNull(source, "Source can not be null");
    }

    /**
     * Construct a media resource for the given URI string.
     *
     * @param source a URI string
     */
    public MediaSource(String source) {
        this(source, true, null);
    }

    /**
     * Construct a media resource for the given {@link WebAPI.JSFile} object.
     *
     * @param jsFile a JS file retrieved from the client's browser.
     */
    public MediaSource(WebAPI.JSFile jsFile) {
        this(jsFile.getObjectURL().getName(), false, jsFile);
    }
}
