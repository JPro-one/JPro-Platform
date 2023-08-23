package one.jpro.platform.media;

import com.jpro.webapi.WebAPI;
import javafx.beans.NamedArg;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import one.jpro.platform.media.recorder.MediaRecorder;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;

/**
 * The <code>MediaSource</code> class represents a media resource.
 * It is instantiated from the string form of a source {@link URI}
 * or {@link WebAPI.JSFile} depending on whenever the resource is
 * available locally, on the JPro server or in the browser's client.
 * <p>The same <code>MediaSource</code> object may be shared among multiple
 * <code>MediaPlayer</code> or <code>MediaRecorder</code> objects.
 *
 * @param source  string form of a media source URI
 * @param isLocal <code>true</code> if this media source is local when running
 *                on a desktop/mobile or as a local resource to JPro server,
 *                otherwise <code>false</code> when inside the client's browser
 * @param jsFile  the non-local media source located inside the client's browser
 * @author Besmir Beqiri
 * @see MediaPlayer
 * @see MediaRecorder
 * @see MediaException
 */
public record MediaSource(String source, boolean isLocal, WebAPI.JSFile jsFile) {

    /**
     * Compact constructor.
     *
     * @throws NullPointerException     if the source is <code>null</code>.
     * @throws IllegalArgumentException if the source is not a valid URI.
     */
    public MediaSource {
        Objects.requireNonNull(source, "Source can not be null");

        if (isLocal) {
            try {
                new URI(source);
            } catch (URISyntaxException use) {
                throw new IllegalArgumentException(use);
            }
        }
    }

    /**
     * Construct a media source for the given URI string.
     *
     * @param source a URI string
     */
    public MediaSource(@NamedArg("source") String source) {
        this(source, true, null);
    }

    /**
     * Construct a media source for the given {@link WebAPI.JSFile} object.
     *
     * @param jsFile a JS file retrieved from the client's browser.
     */
    public MediaSource(WebAPI.JSFile jsFile) {
        this(jsFile.getObjectURL().getName(), false, jsFile);
    }

    /**
     * Construct a media source for the given {@link File} object.
     *
     * @param file a local file
     */
    public MediaSource(File file) {
        this(file.toURI().toString());
    }

    /**
     * Returns the local file representing this media source.
     *
     * @return an optional file object.
     */
    public Optional<File> file() {
        if (isLocal) {
            return Optional.of(new File(URI.create(source)));
        }
        return Optional.empty();
    }
}
