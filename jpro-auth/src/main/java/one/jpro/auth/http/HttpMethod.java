package one.jpro.auth.http;

/**
 * Represents an HTTP method. Intended for use
 * with {@link java.net.http.HttpClient} and {@link java.net.http.HttpRequest}.
 *
 * @author Besmir Beqiri
 */
public enum HttpMethod {

    /**
     * The HTTP method {@code OPTIONS}.
     *
     * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.2">HTTP 1.1, section 9.2</a>
     */
    OPTIONS,

    /**
     * The HTTP method {@code GET}.
     *
     * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.3">HTTP 1.1, section 9.3</a>
     */
    GET,

    /**
     * The HTTP method {@code HEAD}.
     *
     * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.4">HTTP 1.1, section 9.4</a>
     */
    HEAD,

    /**
     * The HTTP method {@code POST}.
     *
     * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.5">HTTP 1.1, section 9.5</a>
     */
    POST,

    /**
     * The HTTP method {@code PUT}.
     *
     * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.6">HTTP 1.1, section 9.6</a>
     */
    PUT,

    /**
     * The HTTP method {@code PATCH}.
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc5789#section-2">RFC 5789</a>
     */
    PATCH,

    /**
     * The HTTP method {@code DELETE}.
     *
     * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.7">HTTP 1.1, section 9.7</a>
     */
    DELETE,

    /**
     * The HTTP method {@code TRACE}.
     *
     * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.8">HTTP 1.1, section 9.8</a>
     */
    TRACE
}
