package one.jpro.platform.mail.config;

import one.jpro.platform.mail.StartTLSOption;
import one.jpro.platform.mail.impl.MailConfigImpl;

/**
 * The MailTrapConfig class provides a predefined configuration for connecting to the MailTrap SMTP server.
 * It sets specific properties required to use MailTrap's sandbox environment for sending emails.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * MailConfig config = new MailTrapConfig();
 * // Further configuration or usage of config
 * }</pre>
 *
 * @author Besmir Beqiri
 * @see StartTLSOption
 */
public class MailTrapConfig extends MailConfigImpl {

    /**
     * Constructs a new MailTrapConfig instance with default settings for connecting to
     * MailTrap's SMTP server. The configuration includes enabling SMTP authentication,
     * enabling STARTTLS, setting the SMTP host to MailTrap's sandbox server, setting the
     * SMTP port and trusting the MailTrap sandbox host for SSL connections.
     */
    public MailTrapConfig() {
        setMailSmtpAuth(true);
        setMailSmtpStartTLS(StartTLSOption.ENABLED);
        setMailSmtpHost("sandbox.smtp.mailtrap.io");
        setMailSmtpPort(25);
        setMailSmtpSslTrust("sandbox.smtp.mailtrap.io");
    }
}
