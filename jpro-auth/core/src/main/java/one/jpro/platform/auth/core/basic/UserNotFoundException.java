package one.jpro.platform.auth.core.basic;

import one.jpro.platform.auth.core.authentication.User;

/**
 * Represents an exception that is thrown when a {@link User} cannot be located.
 *
 * @author Besmir Beqiri
 */
public class UserNotFoundException extends RuntimeException {

    /**
     * Constructs a new UserNotFoundException with the specified detail message.
     *
     * @param msg the detail message. The detail message is saved for
     *            later retrieval by the {@link Throwable#getMessage()} method.
     */
    public UserNotFoundException(String msg) {
        super(msg);
    }

    /**
     * Constructs a new UserNotFoundException with the specified detail message and cause.
     *
     * @param msg   the detail message. The detail message is saved for
     *              later retrieval by the {@link Throwable#getMessage()} method.
     * @param cause the cause (which is saved for later retrieval by the
     *              {@link Throwable#getCause()} method). (A null value is
     *              permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public UserNotFoundException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
