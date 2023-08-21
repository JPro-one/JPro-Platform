package one.jpro.auth.http.impl;

import java.util.List;

/**
 * Represents an HTTP response.
 *
 * @author Besmir Beqiri
 */
final class Response {

    /**
     * The byte array representing the ": " separator.
     */
    static final byte[] COLON_SPACE = ": ".getBytes();

    /**
     * The byte array representing a space.
     */
    static final byte[] SPACE = " ".getBytes();

    /**
     * The byte array representing the carriage return and line feed.
     */
    static final byte[] CRLF = "\r\n".getBytes();

    private final int status;
    private final String reason;
    private final List<Header> headers;
    private final byte[] body;

    /**
     * Constructs a new Response object with the specified status, reason, headers, and body.
     *
     * @param status  the status code of the response
     * @param reason  the reason phrase of the response
     * @param headers the headers of the response
     * @param body    the body of the response
     */
    public Response(int status, String reason, List<Header> headers, byte[] body) {
        this.status = status;
        this.reason = reason;
        this.headers = headers;
        this.body = body;
    }

    /**
     * Returns the status code of the response.
     *
     * @return the status code
     */
    public int getStatus() {
        return status;
    }

    /**
     * Returns the reason phrase of the response.
     *
     * @return the reason phrase
     */
    public String getReason() {
        return reason;
    }

    /**
     * Returns the headers of the response.
     *
     * @return the headers
     */
    public List<Header> getHeaders() {
        return headers;
    }

    /**
     * Returns the body of the response.
     *
     * @return the body
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * Checks if the response has a header with the specified name.
     *
     * @param name the name of the header to check
     * @return true if the header exists, false otherwise
     */
    public boolean hasHeader(String name) {
        for (Header header : headers) {
            if (header.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Serializes the response to a byte array using the specified version and headers.
     *
     * @param version the HTTP version to use in the serialization
     * @param headers the additional headers to include in the serialization
     * @return the serialized response as a byte array
     */
    byte[] serialize(String version, List<Header> headers) {
        ByteMerger merger = new ByteMerger();
        merger.add(version.getBytes());
        merger.add(SPACE);
        merger.add(Integer.toString(status).getBytes());
        merger.add(SPACE);
        merger.add(reason.getBytes());
        merger.add(CRLF);
        appendHeaders(merger, headers);
        appendHeaders(merger, this.headers);
        merger.add(CRLF);
        merger.add(body);
        return merger.merge();
    }

    /**
     * Appends the headers to the specified ByteMerger.
     *
     * @param merger  the ByteMerger to append the headers to
     * @param headers the headers to append
     */
    private static void appendHeaders(ByteMerger merger, List<Header> headers) {
        for (Header header : headers) {
            merger.add(header.getName().getBytes());
            merger.add(COLON_SPACE);
            merger.add(header.getValue().getBytes());
            merger.add(CRLF);
        }
    }
}
