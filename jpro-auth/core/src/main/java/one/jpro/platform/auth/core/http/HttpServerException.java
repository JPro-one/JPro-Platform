package one.jpro.platform.auth.core.http;

/**
 * A runtime exception thrown when the http server fails.
 *
 * @author Besmir Beqiri
 */
public class HttpServerException extends RuntimeException {

    /**
     * Constructs a new http server exception with the specified
     * detail message. The cause is not initialized, and may subsequently
     * be initialized by a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public HttpServerException(String message) {
        super(message);
    }

    /**
     * Constructs a new http server exception with the specified cause and
     * a detail message of (cause==null ? null : cause.toString()) (which
     * typically contains the class and detail message of cause).
     *
     * @param cause the cause (which is saved for later retrieval by the
     *              {@link #getCause()} method). (A null value is permitted,
     *              and indicates that the cause is nonexistent or unknown.)
     */
    public HttpServerException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new http server exception with the specified
     * detail message and cause.
     * <p>
     * Note that the detail message associated with {@code cause} is <i>not</i>
     * automatically incorporated in this runtime exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param cause   the cause which is saved for later retrieval by the
     *                {@link #getCause()} method. A {@code null} value is
     *                permitted and indicates that the cause is nonexistent or
     *                unknown.
     */
    public HttpServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
