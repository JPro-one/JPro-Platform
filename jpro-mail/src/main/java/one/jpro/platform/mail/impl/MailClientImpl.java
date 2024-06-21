package one.jpro.platform.mail.impl;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import one.jpro.platform.mail.MailClient;
import one.jpro.platform.mail.MailConfig;
import one.jpro.platform.mail.MailMessage;
import org.jetbrains.annotations.NotNull;

/**
 * Mail client implementation.
 *
 * @author Besmir Beqiri
 */
public class MailClientImpl implements MailClient {

    private final Session session;

    public MailClientImpl(MailConfig mailConfig) {
        session = Session.getInstance(mailConfig.getProperties());
    }

    public MailClientImpl(MailConfig mailConfig, String username, String password) {
        final Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        };
        session = Session.getInstance(mailConfig.getProperties(), authenticator);
    }

    @NotNull
    @Override
    public MailMessage createMessage() {
        return new MailMessageImpl(session);
    }
}
