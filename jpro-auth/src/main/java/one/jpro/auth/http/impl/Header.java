package one.jpro.auth.http.impl;

/**
 * HTTP header.
 *
 * @author Besmir Beqiri
 */
public final class Header {

    private final String name;
    private final String value;

    /**
     * Creates a header with the given name and value.
     *
     * @param name  the header name
     * @param value the header value
     */
    public Header(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Return the header name.
     *
     * @return the header name
     */
    public String getName() {
        return name;
    }

    /**
     * Return the header value.
     *
     * @return the header value
     */
    public String getValue() {
        return value;
    }
}
