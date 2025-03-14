package one.jpro.platform.mail.impl;

import one.jpro.platform.mail.MailConfig;
import one.jpro.platform.mail.StartTLSOption;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;

/**
 * Mail configuration implementation.
 *
 * @author Besmir Beqiri
 */
public class MailConfigImpl implements MailConfig {

    private final Properties properties;

    public MailConfigImpl() {
        this.properties = new Properties();
    }

    @NotNull
    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public boolean isMailDebug() {
        return Boolean.parseBoolean(properties.getProperty(MAIL_DEBUG, String.valueOf(DEFAULT_MAIL_DEBUG)));
    }

    @NotNull
    @Override
    public MailConfig setMailDebug(boolean mailDebug) {
        properties.put(MAIL_DEBUG, mailDebug);
        return this;
    }

    @NotNull
    @Override
    public String getMailSmtpUser() {
        return properties.getProperty(MAIL_SMTP_USER);
    }

    @NotNull
    @Override
    public MailConfig setMailSmtpUser(String mailSmtpUser) {
        properties.put(MAIL_SMTP_USER, mailSmtpUser);
        return this;
    }

    @NotNull
    @Override
    public String getMailSmtpHost() {
        return properties.getProperty(MAIL_SMTP_HOST, DEFAULT_MAIL_SMTP_HOST);
    }

    @NotNull
    @Override
    public MailConfig setMailSmtpHost(String mailSmtpHost) {
        properties.put(MAIL_SMTP_HOST, mailSmtpHost);
        return this;
    }

    @Override
    public int getMailSmtpPort() {
        return Integer.parseInt(properties.getProperty(MAIL_SMTP_PORT, String.valueOf(DEFAULT_MAIL_SMTP_PORT)));
    }

    @NotNull
    @Override
    public MailConfig setMailSmtpPort(int mailSmtpPort) {
        properties.put(MAIL_SMTP_PORT, mailSmtpPort);
        return this;
    }

    @Override
    public int getMailSmtpConnectionTimeout() {
        return Integer.parseInt(properties.getProperty(MAIL_SMTP_CONNECTIONTIMEOUT));
    }

    @NotNull
    @Override
    public MailConfig setMailSmtpConnectionTimeout(int mailSmtpConnectionTimeout) {
        properties.put(MAIL_SMTP_CONNECTIONTIMEOUT, mailSmtpConnectionTimeout);
        return this;
    }

    @Override
    public int getMailSmtpTimeout() {
        return Integer.parseInt(properties.getProperty(MAIL_SMTP_TIMEOUT));
    }

    @NotNull
    @Override
    public MailConfig setMailSmtpTimeout(int mailSmtpTimeout) {
        properties.put(MAIL_SMTP_TIMEOUT, mailSmtpTimeout);
        return this;
    }

    @Override
    public boolean isMailSmtpAuth() {
        return Boolean.parseBoolean(properties.getProperty(MAIL_SMTP_AUTH, String.valueOf(DEFAULT_MAIL_SMTP_AUTH)));
    }

    @NotNull
    @Override
    public MailConfig setMailSmtpAuth(boolean mailSmtpAuth) {
        properties.put(MAIL_SMTP_AUTH, mailSmtpAuth);
        return this;
    }

    @NotNull
    @Override
    public String getMailSmtpAuthMechanisms() {
        return properties.getProperty(MAIL_SMTP_AUTH_MECHANISMS);
    }

    @NotNull
    @Override
    public MailConfig setMailSmtpAuthMechanisms(String mailSmtpAuthMechanisms) {
        properties.put(MAIL_SMTP_AUTH_MECHANISMS, mailSmtpAuthMechanisms);
        return this;
    }

    @Override
    public boolean isMailSmtpAuthLoginDisable() {
        return Boolean.parseBoolean(properties.getProperty(MAIL_SMTP_AUTH_LOGIN_DISABLE));
    }

    @NotNull
    @Override
    public MailConfig setMailSmtpAuthLoginDisable(boolean mailSmtpAuthLoginDisable) {
        properties.put(MAIL_SMTP_AUTH_LOGIN_DISABLE, mailSmtpAuthLoginDisable);
        return this;
    }

    @Override
    public boolean isMailSmtpAuthPlainDisable() {
        return Boolean.parseBoolean(properties.getProperty(MAIL_SMTP_AUTH_PLAIN_DISABLE));
    }

    @NotNull
    @Override
    public MailConfig setMailSmtpAuthPlainDisable(boolean mailSmtpAuthPlainDisable) {
        properties.put(MAIL_SMTP_AUTH_PLAIN_DISABLE, mailSmtpAuthPlainDisable);
        return this;
    }

    @Override
    public boolean isMailSmtpAuthDigestMd5Disable() {
        return Boolean.parseBoolean(properties.getProperty(MAIL_SMTP_AUTH_DIGEST_MD5_DISABLE,
                String.valueOf(DEFAULT_MAIL_SMTP_AUTH_DIGEST_MD5_DISABLE)));
    }

    @NotNull
    @Override
    public MailConfig setMailSmtpAuthDigestMd5Disable(boolean mailSmtpAuthDigestMd5Disable) {
        properties.put(MAIL_SMTP_AUTH_DIGEST_MD5_DISABLE, mailSmtpAuthDigestMd5Disable);
        return this;
    }

    @Override
    public boolean isMailSmtpAuthXOAuth2Disable() {
        return Boolean.parseBoolean(properties.getProperty(MAIL_SMTP_AUTH_XOAUTH2_DISABLE,
                String.valueOf(DEFAULT_MAIL_SMTP_AUTH_XOAUTH2_DISABLE)));
    }

    @NotNull
    @Override
    public MailConfig setMailSmtpAuthXOAuth2Disable(boolean mailSmtpAuthXOAuth2Disable) {
        properties.put(MAIL_SMTP_AUTH_XOAUTH2_DISABLE, mailSmtpAuthXOAuth2Disable);
        return this;
    }

    @NotNull
    @Override
    public StartTLSOption getMailSmtpStartTLS() {
        final boolean starttlsEnable = Boolean.parseBoolean(properties
                .getProperty(MAIL_SMTP_STARTTLS_ENABLE, "false"));
        final boolean starttlsRequired = Boolean.parseBoolean(properties
                .getProperty(MAIL_SMTP_STARTTLS_REQUIRED, "false"));

        if (starttlsEnable && starttlsRequired) {
            return StartTLSOption.REQUIRED;
        } else if (starttlsEnable) {
            return StartTLSOption.ENABLED;
        } else {
            return StartTLSOption.DISABLED;
        }
    }

    @NotNull
    @Override
    public MailConfig setMailSmtpStartTLS(StartTLSOption mailSmtpStarttls) {
        switch (mailSmtpStarttls) {
            case DISABLED:
                properties.put(MAIL_SMTP_STARTTLS_ENABLE, "false");
                properties.put(MAIL_SMTP_STARTTLS_REQUIRED, "false");
                break;
            case ENABLED:
                properties.put(MAIL_SMTP_STARTTLS_ENABLE, "true");
                properties.put(MAIL_SMTP_STARTTLS_REQUIRED, "false");
                break;
            case REQUIRED:
                properties.put(MAIL_SMTP_STARTTLS_ENABLE, "true");
                properties.put(MAIL_SMTP_STARTTLS_REQUIRED, "true");
                break;
        }
        return this;
    }

    @Override
    public boolean isMailSmtpSslEnable() {
        return Boolean.parseBoolean(properties
                .getProperty(MAIL_SMTP_SSL_ENABLE, String.valueOf(DEFAULT_MAIL_SMTP_SSL_ENABLE)));
    }

    @NotNull
    @Override
    public MailConfig setMailSmtpSslEnable(boolean mailSmtpSslEnable) {
        properties.put(MAIL_SMTP_SSL_ENABLE, mailSmtpSslEnable);
        return this;
    }

    @Override
    public boolean isMailSmtpSslCheckServerIdentity() {
        return Boolean.parseBoolean(properties.getProperty(MAIL_SMTP_SSL_CHECKSERVERIDENTITY,
                String.valueOf(DEFAULT_MAIL_SMTP_SSL_CHECKSERVERIDENTITY)));
    }

    @NotNull
    @Override
    public MailConfig setMailSmtpSslCheckServerIdentity(boolean mailSmtpSslCheckServerIdentity) {
        properties.put(MAIL_SMTP_SSL_CHECKSERVERIDENTITY, mailSmtpSslCheckServerIdentity);
        return this;
    }

    @NotNull
    @Override
    public String getMailSmtpSslTrust() {
        return properties.getProperty(MAIL_SMTP_SSL_TRUST);
    }

    @NotNull
    @Override
    public MailConfig setMailSmtpSslTrust(String mailSmtpSslTrust) {
        properties.put(MAIL_SMTP_SSL_TRUST, mailSmtpSslTrust);
        return this;
    }
}
