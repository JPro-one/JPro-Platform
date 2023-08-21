package one.jpro.auth.jwt;

import one.jpro.auth.http.HttpMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static one.jpro.auth.utils.AuthUtils.*;

/**
 * JWTAuth API class.
 *
 * @author Besmir Beqiri
 */
public class JWTAuthAPI {

    @NotNull
    private final JWTAuthOptions options;
    @NotNull
    private final HttpClient httpClient;

    public JWTAuthAPI(@NotNull final JWTAuthOptions options) {
        this.options = options;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Post a request to the token endpoint to obtain a JWT.
     *
     * @param tokenPath the path to the token endpoint.
     * @param params    the parameters to be sent.
     * @see <a href="https://tools.ietf.org/html/rfc6749">https://tools.ietf.org/html/rfc6749</a>
     */
    public CompletableFuture<JSONObject> token(@NotNull final String tokenPath,
                                               @NotNull final JSONObject params) {
        // Send authorization params in the body
        final String payload = new JSONObject(params.toString()).toString();

        final JSONObject headers = new JSONObject();
        // specify preferred accepted content type
        headers.put("Accept", "application/json,application/x-www-form-urlencoded;q=0.9");
        // specify content type
        headers.put("Content-Type", "application/json");

        return fetch(HttpMethod.POST, tokenPath, headers, payload)
                .thenCompose(response -> {
                    if (response.body() == null || response.body().isEmpty()) {
                        return CompletableFuture.failedFuture(new RuntimeException("No Body"));
                    }

                    final JSONObject json;
                    final var header = response.headers();
                    if (containsValue(header, "application/json")) {
                        json = new JSONObject(response.body());
                    } else if (containsValue(header, "application/x-www-form-urlencoded")
                            || containsValue(header, "text/plain")) {
                        json = queryToJson(response.body());
                    } else {
                        return CompletableFuture.failedFuture(
                                new RuntimeException("Cannot handle content type: "
                                        + header.map().get("Content-Type")));
                    }

                    if (json == null || json.has("error")) {
                        return CompletableFuture.failedFuture(
                                new RuntimeException(extractErrorDescription(json)));
                    } else {
                        return CompletableFuture.completedFuture(json);
                    }
                });
    }

    /**
     * Fetch a resource from the given path.
     *
     * @param method  the HTTP method to use
     * @param path    the path to the resource
     * @param headers the headers to be sent
     * @param payload the payload to be sent
     * @return a result future
     */
    private CompletableFuture<HttpResponse<String>> fetch(@NotNull final HttpMethod method,
                                                          @NotNull final String path,
                                                          @Nullable final JSONObject headers,
                                                          @NotNull String payload) {
        if (path == null || path.length() == 0) {
            // and this can happen as it is a config option that is dependent on the provider
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid path"));
        }

        final String url = path.charAt(0) == '/' ? options.getSite() + path : path;
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(url));

        if (headers != null) {
            for (Map.Entry<String, Object> kv : headers.toMap().entrySet()) {
                requestBuilder.header(kv.getKey(), (String) kv.getValue());
            }
        }

        if (method != HttpMethod.POST && method != HttpMethod.PATCH && method != HttpMethod.PUT) {
            payload = null;
        }

        // create a request
        return makeRequest(requestBuilder, payload);
    }

    /**
     * Make and send a request asynchronously.
     *
     * @param requestBuilder the request builder
     * @param payload        the payload to be sent
     */
    private CompletableFuture<HttpResponse<String>> makeRequest(HttpRequest.Builder requestBuilder, String payload) {
        // send
        if (payload != null) {
            requestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(payload.getBytes()));
        }

        return httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    // read the body regardless
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        if (response.body() == null || response.body().length() == 0) {
                            return CompletableFuture.failedFuture(
                                    new RuntimeException("Status code: " + response.statusCode()));
                        } else {
                            if (containsValue(response.headers(), "application/json")) {
                                // if value is json, extract error, error_descriptions
                                JSONObject error = new JSONObject(response.body());
                                if (!error.optString("error").isEmpty()) {
                                    if (!error.optString("error_description").isEmpty()) {
                                        return CompletableFuture.failedFuture(
                                                new RuntimeException(error.getString("error") +
                                                        ": " + error.getString("error_description")));
                                    } else {
                                        return CompletableFuture.failedFuture(
                                                new RuntimeException(error.getString("error")));
                                    }
                                }
                            }
                            return CompletableFuture.failedFuture(
                                    new RuntimeException(response.statusCode() + ": " + response.body()));
                        }
                    } else {
                        return CompletableFuture.completedFuture(response);
                    }
                });
    }
}
