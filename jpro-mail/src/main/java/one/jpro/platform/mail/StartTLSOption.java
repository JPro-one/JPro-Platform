package one.jpro.platform.mail;

/**
 * Possible options for a secure connection using TLS protocol.
 *
 * @author Besmir Beqiri
 */
public enum StartTLSOption {

    /**
     * StartTLS is disabled and will not be used in any case.
     */
    DISABLED,

    /**
     * StartTLS is enabled and will be used if the server supports it.
     */
    ENABLED,

    /**
     * StartTLS is required and will be used if the server supports it and the send operation will fail otherwise.
     */
    REQUIRED
}
