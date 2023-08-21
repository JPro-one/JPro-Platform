package one.jpro.auth.test;

import one.jpro.auth.http.HttpServer;
import one.jpro.auth.http.HttpMethod;
import one.jpro.auth.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Authentication server tests.
 *
 * @author Besmir Beqiri
 */
public class AuthenticationServerTest {

    @Test
    public void testHttpServer() throws IOException, InterruptedException {
        try (HttpServer httpServer = HttpServer.create()) {
            var requestUri = URI.create("http://"
                    + httpServer.getServerHost() + ":"
                    + httpServer.getServerPort() + "/auth?foo&bar=HTTP/1.1");
            assertEquals(requestUri.toString(), "http://localhost:8080/auth?foo&bar=HTTP/1.1");
            var request = HttpRequest.newBuilder()
                    .uri(requestUri)
                    .GET()
                    .build();
            var httpResponse = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(HttpStatus.OK, HttpStatus.fromCode(httpResponse.statusCode()));
            assertEquals(HttpMethod.GET, HttpMethod.valueOf(request.method()));
            assertEquals(URI.create("http://localhost:8080/auth?foo&bar=HTTP/1.1"), request.uri());
            assertEquals("<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <title>JPro-Auth</title>\n" +
                    "    <script>\n" +
                    "        window.open('', '_self', '');\n" +
                    "        window.close();\n" +
                    "    </script>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "\n" +
                    "</body>\n" +
                    "</html>", httpResponse.body());
            assertEquals("{ {content-length=[216], content-type=[text/html]} }",
                    "{ " + httpResponse.headers().map() + " }");
            assertEquals("localhost", httpServer.getServerHost());
            assertEquals(8080, httpServer.getServerPort());
            assertEquals("/auth?foo&bar=HTTP/1.1", httpServer.getFullRequestedURL());
            assertEquals("{bar=HTTP/1.1, foo=}", httpServer.getQueryParams().toString());
        }
    }
}
