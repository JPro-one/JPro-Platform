package one.jpro.platform.auth.http;

import one.jpro.platform.auth.authentication.Options;
import org.json.JSONObject;

import java.net.InetAddress;
import java.time.Duration;

/**
 * Represents configuration options for {@link HttpServer} configuration.
 * Usage example:
 * <pre>{@code
 * HttpOptions options = new HttpOptions()
 *                         .setHost("127.0.0.1")
 *                         .setPort(8080)
 *                         .setReuseAddr(true);
 * }</pre>
 *
 * @author Besmir Beqiri
 */
public class HttpOptions implements Options {

    public static final String DEFAULT_HOST = InetAddress.getLoopbackAddress().getHostAddress(); // loopback IP address
    public static final int DEFAULT_PORT = 8080;
    public static final boolean DEFAULT_REUSE_ADDR = false;
    public static final boolean DEFAULT_REUSE_PORT = false;
    public static final Duration DEFAULT_RESOLUTION = Duration.ofMillis(100);
    public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(60);
    public static final int DEFAULT_READ_BUFFER_SIZE = 1_024 * 64;
    public static final int DEFAULT_ACCEPT_LENGTH = 0;
    public static final int DEFAULT_MAX_REQUEST_SIZE = 1_024 * 1_024;
    public static final int DEFAULT_CONCURRENCY = Runtime.getRuntime().availableProcessors();

    private String host = DEFAULT_HOST;
    private int port = DEFAULT_PORT;
    private boolean reuseAddr = DEFAULT_REUSE_ADDR;
    private boolean reusePort = DEFAULT_REUSE_PORT;
    private Duration resolution = DEFAULT_RESOLUTION;
    private Duration requestTimeout = DEFAULT_REQUEST_TIMEOUT;
    private int readBufferSize = DEFAULT_READ_BUFFER_SIZE;
    private int acceptLength = DEFAULT_ACCEPT_LENGTH;
    private int maxRequestSize = DEFAULT_MAX_REQUEST_SIZE;
    private int concurrency = DEFAULT_CONCURRENCY;

    /**
     * Retrieves the host address currently set for HTTP connections.
     *
     * @return the host address as a string
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the host address for HTTP connections.
     *
     * @param host the string representation of the host address
     * @return the {@code HttpOptions} instance for method chaining
     */
    public HttpOptions setHost(String host) {
        this.host = host;
        return this;
    }

    /**
     * Retrieves the port number currently set for HTTP connections.
     *
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the port number for HTTP connections.
     *
     * @param port the port number
     * @return the {@code HttpOptions} instance for method chaining
     */
    public HttpOptions setPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * Returns whether the reuse address flag is enabled for the HTTP connections.
     *
     * @return {@code true} if the flag is enabled, {@code false} otherwise.
     */
    public boolean isReuseAddr() {
        return reuseAddr;
    }

    /**
     * Sets the reuse address flag for the HTTP connections.
     *
     * @param reuseAddr the value to set for the reuse address flag
     * @return the HttpOptions object itself, for method chaining
     */
    public HttpOptions setReuseAddr(boolean reuseAddr) {
        this.reuseAddr = reuseAddr;
        return this;
    }

    /**
     * Returns the value of the reuse port flag for the HTTP connections.
     *
     * @return the value of the reuse port flag
     */
    public boolean isReusePort() {
        return reusePort;
    }

    /**
     * Sets the value of the reuse port flag for the HTTP connections.
     *
     * @param reusePort the value to be set for the reuse port flag
     * @return the HttpOptions object itself, for method chaining
     */
    public HttpOptions setReusePort(boolean reusePort) {
        this.reusePort = reusePort;
        return this;
    }

    /**
     * Retrieves the resolution duration for HTTP connections.
     *
     * @return the resolution as a {@code Duration}
     */
    public Duration getResolution() {
        return resolution;
    }

    /**
     * Sets the resolution duration for HTTP connections.
     *
     * @param resolution the resolution as a {@code Duration}
     * @return the {@code HttpOptions} instance for method chaining
     */
    public HttpOptions setResolution(Duration resolution) {
        this.resolution = resolution;
        return this;
    }

    /**
     * Retrieves the request timeout duration for HTTP operations.
     *
     * @return the request timeout as a {@code Duration}
     */
    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * Sets the request timeout duration for HTTP operations.
     *
     * @param requestTimeout the request timeout as a {@code Duration}
     * @return the {@code HttpOptions} instance for method chaining
     */
    public HttpOptions setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    /**
     * Retrieves the size of the read buffer for HTTP connections.
     *
     * @return the read buffer size in bytes
     */
    public int getReadBufferSize() {
        return readBufferSize;
    }

    /**
     * Sets the size of the read buffer for HTTP connections.
     *
     * @param readBufferSize the size of the read buffer in bytes
     * @return the {@code HttpOptions} instance for method chaining
     */
    public HttpOptions setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
        return this;
    }

    /**
     * Retrieves the maximum length of the accept queue for incoming connections.
     *
     * @return the accept length (queue size)
     */
    public int getAcceptLength() {
        return acceptLength;
    }

    /**
     * Sets the maximum length of the accept queue for incoming connections.
     *
     * @param acceptLength the length of the accept queue
     * @return the {@code HttpOptions} instance for method chaining
     */
    public HttpOptions setAcceptLength(int acceptLength) {
        this.acceptLength = acceptLength;
        return this;
    }

    /**
     * Retrieves the maximum size for HTTP requests.
     *
     * @return the maximum size of an HTTP request in bytes
     */
    public int getMaxRequestSize() {
        return maxRequestSize;
    }

    /**
     * Sets the maximum size for HTTP requests.
     *
     * @param maxRequestSize the maximum size in bytes for an HTTP request
     * @return the {@code HttpOptions} instance for method chaining
     */
    public HttpOptions setMaxRequestSize(int maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
        return this;
    }

    /**
     * Retrieves the level of concurrency for HTTP operations.
     *
     * @return the concurrency level
     */
    public int getConcurrency() {
        return concurrency;
    }

    /**
     * Sets the level of concurrency for HTTP operations.
     *
     * @param concurrency the concurrency level to set
     * @return the {@code HttpOptions} instance for method chaining
     */
    public HttpOptions setConcurrency(int concurrency) {
        this.concurrency = concurrency;
        return this;
    }

    /**
     * Converts the current settings of {@code HttpOptions} to a JSON representation.
     * This is useful for debugging or storing the configuration state.
     *
     * @return a {@code JSONObject} representing the current configuration settings.
     */
    @Override
    public JSONObject toJSON() {
        final JSONObject json = new JSONObject();
        json.put("host", host);
        json.put("port", port);
        json.put("reuseAddr", reuseAddr);
        json.put("reusePort", reusePort);
        json.put("resolution", resolution.toMillis());
        json.put("requestTimeout", requestTimeout.toMillis());
        json.put("readBufferSize", readBufferSize);
        json.put("acceptLength", acceptLength);
        json.put("maxRequestSize", maxRequestSize);
        json.put("concurrency", concurrency);
        return json;
    }
}
