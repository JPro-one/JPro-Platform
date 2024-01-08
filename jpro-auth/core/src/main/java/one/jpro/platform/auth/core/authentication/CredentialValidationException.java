package one.jpro.platform.auth.core.authentication;

/**
 * Represents an exception that is thrown when {@link Credentials} validation fails.
 *
 * @author Besmir Beqiri
 */
public class CredentialValidationException extends RuntimeException {

    /**
     * Constructs a new CredentialValidationException with the specified detail message.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link Throwable#getMessage()} method.
     */
    public CredentialValidationException(String message) {
        super(message);
    }

    /**
     * Constructs a new CredentialValidationException with the specified detail message and cause.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link Throwable#getMessage()} method.
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link Throwable#getCause()} method). (A null value is
     *                permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public CredentialValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
