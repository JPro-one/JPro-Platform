package one.jpro.platform.auth.test;

import one.jpro.platform.auth.http.HttpServer;
import one.jpro.platform.auth.http.HttpServerException;

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
        return HttpServer.create(null);
    }
}
