package one.jpro.platform.auth.core.http;

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
public class AuthenticationServerTests {

    @Test
    public void testHttpServer() throws IOException, InterruptedException {
        try (HttpServer httpServer = TestHttpServer.create()) {
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
            assertEquals("""
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta http-equiv="Content-Type" content="text/html" charset="UTF-8">
                        <title>Authentication</title>
                    </head>
                    <body>
                        <div style="text-align: center; font-family: sans-serif; margin-top: 20px;">
                            <h3>Authentication Successful</h3>
                            <i>Please close the page.</i>
                        </div>
                    </body>
                    </html>""", httpResponse.body());
            assertEquals("{ {content-length=[350], content-type=[text/html]} }",
                    "{ " + httpResponse.headers().map() + " }");
            assertEquals("localhost", httpServer.getServerHost());
            assertEquals(8080, httpServer.getServerPort());
            assertEquals("/auth?foo&bar=HTTP/1.1", httpServer.getFullRequestedURL());
            assertEquals("{bar=HTTP/1.1, foo=}", httpServer.getQueryParams().toString());
        }
    }
}
