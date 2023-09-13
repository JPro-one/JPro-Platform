package one.jpro.platform.auth.http.impl;

import java.util.List;

/**
 * This class represents discrete HTTP requests with request line, headers, and body.
 *
 * @author Besmir Beqiri
 */
public record Request(String method, String uri, String version, List<Header> headers, byte[] body) {

    /**
     * Retrieves the value of the specified header.
     *
     * @param name The name of the header.
     * @return The value of the header, or null if the header is not found.
     */
    public String header(String name) {
        for (Header header : headers) {
            if (header.name().equalsIgnoreCase(name)) {
                return header.value();
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
            if (header.name().equalsIgnoreCase(name) && header.value().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
