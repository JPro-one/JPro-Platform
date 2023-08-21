package one.jpro.auth.authentication;

/**
 * Signals that the validation of the {@link Credentials} implementation is not valid.
 *
 * @author Besmir Beqiri
 */
public class CredentialValidationException extends RuntimeException {

    public CredentialValidationException(String message) {
        super(message);
    }

    public CredentialValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
