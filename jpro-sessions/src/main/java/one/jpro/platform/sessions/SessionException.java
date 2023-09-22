package one.jpro.platform.sessions;

/**
 * SessionException is a runtime exception class that is used to indicate an error or exceptional condition
 * that occurs during session handling.
 *
 * @author Besmir Beqiri
 */
public class SessionException extends RuntimeException {

    /**
     * Constructs a new SessionException with no detail message.
     */
    public SessionException() {
        super();
    }

    /**
     * Constructs a new SessionException with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the getMessage() method)
     */
    public SessionException(String message) {
        super(message);
    }

    /**
     * Constructs a new SessionException with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the getMessage() method)
     * @param cause the cause (which is saved for later retrieval by the getCause() method)
     */
    public SessionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new SessionException with the specified cause.
     *
     * @param cause the cause (which is saved for later retrieval by the getCause() method)
     */
    public SessionException(Throwable cause) {
        super(cause);
    }
}
