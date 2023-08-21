package one.jpro.auth.test;

import one.jpro.auth.http.HttpOptions;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HttpOptions tests.
 *
 * @author Besmir Beqiri
 */
public class HttpOptionsTest {

    @Test
    public void testDefaultValuesFromHttpOptions() {
        final var httpOptions = new HttpOptions();
        assertEquals(httpOptions.getHost(), HttpOptions.DEFAULT_HOST);
        assertEquals(httpOptions.getPort(), HttpOptions.DEFAULT_PORT);
        assertEquals(httpOptions.isReuseAddr(), HttpOptions.DEFAULT_REUSE_ADDR);
        assertEquals(httpOptions.isReusePort(), HttpOptions.DEFAULT_REUSE_PORT);
        assertEquals(httpOptions.getResolution(), HttpOptions.DEFAULT_RESOLUTION);
        assertEquals(httpOptions.getRequestTimeout(), HttpOptions.DEFAULT_REQUEST_TIMEOUT);
        assertEquals(httpOptions.getReadBufferSize(), HttpOptions.DEFAULT_READ_BUFFER_SIZE);
        assertEquals(httpOptions.getAcceptLength(), HttpOptions.DEFAULT_ACCEPT_LENGTH);
        assertEquals(httpOptions.getMaxRequestSize(), HttpOptions.DEFAULT_MAX_REQUEST_SIZE);
        assertEquals(httpOptions.getConcurrency(), HttpOptions.DEFAULT_CONCURRENCY);
    }

    @Test
    public void toJSONMethodProvidesTheExpectedResult() {
        final var httpOptions = new HttpOptions();

        final JSONObject json = new JSONObject();
        json.put("host", httpOptions.getHost());
        json.put("port", httpOptions.getPort());
        json.put("reuseAddr", httpOptions.isReuseAddr());
        json.put("reusePort", httpOptions.isReusePort());
        json.put("resolution", httpOptions.getResolution().toMillis());
        json.put("requestTimeout", httpOptions.getRequestTimeout().toMillis());
        json.put("readBufferSize", httpOptions.getReadBufferSize());
        json.put("acceptLength", httpOptions.getAcceptLength());
        json.put("maxRequestSize", httpOptions.getMaxRequestSize());
        json.put("concurrency", httpOptions.getConcurrency());


        assertTrue(httpOptions.toJSON().similar(json));
    }
}
