package one.jpro.platform.auth.test;

import one.jpro.platform.auth.http.HttpOptions;
import one.jpro.platform.auth.http.HttpServer;
import one.jpro.platform.auth.http.HttpServerException;
import one.jpro.platform.auth.http.impl.HttpServerImpl;

import java.io.IOException;

/**
 * Test HTTP server.
 *
 * @author Besmir Beqiri
 */
public interface TestHttpServer {

    /**
     * Creates a local http server. This method must be used
     * only for desktop/mobile applications that run locally.
     *
     * @return the HTTP server instance
     * @throws HttpServerException if an error occurs
     */
    static HttpServer create() {
        try {
            return new HttpServerImpl(null, new HttpOptions()
                    .setReuseAddr(true)
                    .setReusePort(true));
        } catch (IOException ex) {
            throw new HttpServerException(ex);
        }
    }
}
