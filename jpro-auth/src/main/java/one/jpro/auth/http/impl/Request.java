package one.jpro.auth.http.impl;

import java.util.List;

/**
 * This class represents discrete HTTP requests with request line, headers, and body.
 *
 * @author Besmir Beqiri
 */
final class Request {

    private final String method;
    private final String uri;
    private final String version;
    private final List<Header> headers;
    private final byte[] body;

    /**
     * Constructs a new Request object with the specified method, URI, version, headers, and body.
     *
     * @param method  The HTTP method of the request.
     * @param uri     The URI of the request.
     * @param version The HTTP version of the request.
     * @param headers The list of headers of the request.
     * @param body    The body of the request.
     */
    public Request(String method, String uri, String version, List<Header> headers, byte[] body) {
        this.method = method;
        this.uri = uri;
        this.version = version;
        this.headers = headers;
        this.body = body;
    }

    /**
     * Returns the HTTP method of the request.
     *
     * @return The HTTP method.
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the URI of the request.
     *
     * @return The URI.
     */
    public String getUri() {
        return uri;
    }

    /**
     * Returns the HTTP version of the request.
     *
     * @return The HTTP version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the list of headers of the request.
     *
     * @return The list of headers.
     */
    public List<Header> getHeaders() {
        return headers;
    }

    /**
     * Returns the body of the request.
     *
     * @return The body.
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * Retrieves the value of the specified header.
     *
     * @param name The name of the header.
     * @return The value of the header, or null if the header is not found.
     */
    public String header(String name) {
        for (Header header : headers) {
            if (header.getName().equalsIgnoreCase(name)) {
                return header.getValue();
            }
        }
        return null;
    }

    /**
     * Checks if the request has a header with the specified name and value.
     *
     * @param name  The name of the header.
     * @param value The value of the header.
     * @return true if the request has a header with the specified name and value, false otherwise.
     */
    public boolean hasHeader(String name, String value) {
        for (Header header : headers) {
            if (header.getName().equalsIgnoreCase(name) && header.getValue().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
