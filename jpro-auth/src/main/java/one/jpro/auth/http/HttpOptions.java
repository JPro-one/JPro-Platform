package one.jpro.auth.http;

import one.jpro.auth.authentication.Options;
import org.json.JSONObject;

import java.time.Duration;

/**
 * Http option.
 *
 * @author Besmir Beqiri
 */
public class HttpOptions implements Options {

    public static final String DEFAULT_HOST = "localhost";
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

    public String getHost() {
        return host;
    }

    public HttpOptions setHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public HttpOptions setPort(int port) {
        this.port = port;
        return this;
    }

    public boolean isReuseAddr() {
        return reuseAddr;
    }

    public HttpOptions setReuseAddr(boolean reuseAddr) {
        this.reuseAddr = reuseAddr;
        return this;
    }

    public boolean isReusePort() {
        return reusePort;
    }

    public HttpOptions setReusePort(boolean reusePort) {
        this.reusePort = reusePort;
        return this;
    }

    public Duration getResolution() {
        return resolution;
    }

    public HttpOptions setResolution(Duration resolution) {
        this.resolution = resolution;
        return this;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public HttpOptions setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    public int getReadBufferSize() {
        return readBufferSize;
    }

    public HttpOptions setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
        return this;
    }

    public int getAcceptLength() {
        return acceptLength;
    }

    public HttpOptions setAcceptLength(int acceptLength) {
        this.acceptLength = acceptLength;
        return this;
    }

    public int getMaxRequestSize() {
        return maxRequestSize;
    }

    public HttpOptions setMaxRequestSize(int maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
        return this;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public HttpOptions setConcurrency(int concurrency) {
        this.concurrency = concurrency;
        return this;
    }

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
