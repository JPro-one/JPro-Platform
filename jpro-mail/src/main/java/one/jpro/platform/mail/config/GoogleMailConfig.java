package one.jpro.platform.mail.config;

import one.jpro.platform.mail.StartTLSOption;
import one.jpro.platform.mail.impl.MailConfigImpl;

/**
 * The GoogleMailConfig class provides a predefined configuration for connecting to the Google Mail (Gmail) SMTP server.
 * It sets specific properties required to use Gmail's SMTP server for sending emails.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * MailConfig config = new GoogleMailConfig();
 * // Further configuration or usage of config
 * }</pre>
 *
 * @author Besmir Beqiri
 * @see StartTLSOption
 */
public class GoogleMailConfig extends MailConfigImpl {

    /**
     * Constructs a new GoogleMailConfig instance with default settings for connecting to
     * Google's SMTP server. The configuration includes enabling SMTP authentication,
     * enabling STARTTLS, setting the SMTP host to Google's SMTP server, and setting the
     * SMTP port to the standard port for TLS/STARTTLS.
     */
    public GoogleMailConfig() {
        setMailSmtpAuth(true);
        setMailSmtpStartTLS(StartTLSOption.ENABLED);
        setMailSmtpHost("smtp.gmail.com");
        setMailSmtpPort(587);
    }
}
