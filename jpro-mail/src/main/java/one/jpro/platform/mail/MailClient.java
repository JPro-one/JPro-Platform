package one.jpro.platform.mail;

import one.jpro.platform.mail.impl.MailClientImpl;
import org.jetbrains.annotations.NotNull;

/**
 * The MailClient interface provides methods for creating and managing mail clients.
 * It includes static methods to create instances of MailClient and a method to create mail messages.
 *
 * @author Besmir Beqiri
 */
public interface MailClient {

    /**
     * Creates a new MailClient instance with the given mail configuration.
     *
     * @param mailConfig the {@link MailConfig} object containing the mail configuration
     * @return a new <code>MailClient</code> instance
     */
    @NotNull
    static MailClient create(@NotNull MailConfig mailConfig) {
        return new MailClientImpl(mailConfig);
    }

    /**
     * Creates a new <code>MailClient</code> instance with the given mail configuration, username, and password.
     *
     * @param mailConfig the {@link MailConfig} object containing the mail configuration
     * @param username   the username for authentication
     * @param password   the password for authentication
     * @return a new <code>MailClient</code> instance
     */
    @NotNull
    static MailClient create(@NotNull MailConfig mailConfig,
                             @NotNull String username,
                             @NotNull String password) {
        return new MailClientImpl(mailConfig, username, password);
    }

    /**
     * Creates a new mail message.
     *
     * @return a new <code>MailMessage</code> instance
     */
    @NotNull
    MailMessage createMessage();
}
