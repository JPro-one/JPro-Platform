package one.jpro.platform.auth.core.test;

import one.jpro.platform.auth.core.http.HttpOptions;
import one.jpro.platform.auth.core.http.HttpServer;
import one.jpro.platform.auth.core.http.HttpServerException;
import one.jpro.platform.auth.core.http.impl.HttpServerImpl;

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
            HttpServer httpServer = new HttpServerImpl(null, new HttpOptions()
                    .setReuseAddr(true)
                    .setReusePort(true));
            httpServer.start();
            return httpServer;
        } catch (IOException ex) {
            throw new HttpServerException(ex);
        }
    }
}
