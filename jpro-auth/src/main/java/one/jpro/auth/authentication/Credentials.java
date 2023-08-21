package one.jpro.auth.authentication;

import org.json.JSONObject;

/**
 * Abstract representation of a Credentials object.
 *
 * @author Besmir Beqiri
 */
public interface Credentials {

    /**
     * Convert the credential information to JSON format.
     *
     * @return a JSON object.
     */
    JSONObject toJSON();

    /**
     * Implementors should override this method to perform validation.
     * An argument is allowed to allow custom validation.
     *
     * @param arg optional argument or null.
     * @param <V> the generic type of the argument
     * @throws CredentialValidationException when the validation fails
     */
    default <V> void validate(V arg) throws CredentialValidationException {
    }

    /**
     * Encodes this credential as an HTTP Authorization
     * <a href="https://tools.ietf.org/html/rfc7235">https://tools.ietf.org/html/rfc7235</a>.
     *
     * @throws UnsupportedOperationException when the credential object cannot be converted to an HTTP Authorization.
     * @return HTTP header including scheme.
     */
    default String toHttpAuthorization() {
        throw new UnsupportedOperationException(getClass().getName() + " cannot be converted to HTTP Authorization");
    }
}
