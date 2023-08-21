package one.jpro.auth.oath2;

import one.jpro.auth.authentication.Options;
import org.json.JSONObject;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Options describing Key stored in PEM format.
 *
 * @author Besmir Beqiri
 */
public class PubSecKeyOptions implements Options {

    private String id;
    private String algorithm;
    private Buffer buffer;

    /**
     * Default constructor
     */
    public PubSecKeyOptions() {
    }

    /**
     * Copy constructor.
     *
     * @param other the options to copy
     */
    public PubSecKeyOptions(PubSecKeyOptions other) {
        this.id = other.id;
        this.algorithm = other.algorithm;
        this.buffer = other.buffer;
    }

    /**
     * Returns the key identifier.
     *
     * @return a string
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the key identifier.
     *
     * @param id the key identifier
     * @return a reference to this, so the API can be used fluently
     */
    public PubSecKeyOptions setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Returns the algorithm.
     *
     * @return a string
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Sets the algorithm.
     *
     * @param algorithm the algorithm
     * @return a reference to this, so the API can be used fluently
     */
    public PubSecKeyOptions setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
        return this;
    }

    /**
     * The PEM or Secret key buffer. When working with secret materials, the material is expected to be encoded in
     * {@code UTF-8}. PEM files are expected to be {@code US_ASCII} as the format uses a base64 encoding for the
     * payload.
     *
     * @return the buffer
     */
    public Buffer getBuffer() {
        return buffer;
    }

    /**
     * The PEM or Secret key buffer. When working with secret materials, the material is expected to be encoded in
     * {@code UTF-8}. PEM files are expected to be {@code US_ASCII} as the format uses a base64 encoding for the
     * payload.
     *
     * @param buffer the PEM or Secret key string
     * @return a reference to this, so the API can be used fluently
     */
    public PubSecKeyOptions setBuffer(String buffer) {
        this.buffer = ByteBuffer.wrap(buffer.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    /**
     * The PEM or Secret key buffer. When working with secret materials, the material is expected to be encoded in
     * {@code UTF-8}. PEM files are expected to be {@code US_ASCII} as the format uses a base64 encoding for the
     * payload.
     *
     * @param buffer the PEM or Secret key buffer
     * @return a reference to this, so the API can be used fluently
     */
    public PubSecKeyOptions setBuffer(Buffer buffer) {
        this.buffer = buffer;
        return this;
    }

    public JSONObject toJSON() {
        final JSONObject json = new JSONObject();
        Optional.ofNullable(getId()).ifPresent(id -> json.put("id", id));
        Optional.ofNullable(getAlgorithm()).ifPresent(algorithm -> json.put("algorithm", algorithm));
        Optional.ofNullable(getBuffer()).ifPresent(buffer -> json.put("buffer", buffer));
        return json;
    }
}
