package one.jpro.platform.mail;

import one.jpro.platform.mail.impl.MailConfigImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;

/**
 * This interface represents the configuration for a mail client.
 * It provides methods to get and set various properties related to the mail configuration.
 * The properties include SMTP settings, SSL settings, and debug settings.
 *
 * @author Besmir Beqiri
 */
public interface MailConfig {

    // General properties
    /**
     * Property for enabling or disabling debugging.
     */
    public static final String MAIL_DEBUG = "mail.debug";

    // SMTP properties
    /**
     * Default user name for SMTP.
     */
    public static final String MAIL_SMTP_USER = "mail.smtp.user";

    /**
     * The SMTP server to connect to.
     */
    public static final String MAIL_SMTP_HOST = "mail.smtp.host";

    /**
     * The SMTP server port to connect to, if the connect() method doesn't explicitly specify one.
     * Defaults to <code>25</code>.
     */
    public static final String MAIL_SMTP_PORT = "mail.smtp.port";

    /**
     * Socket connection timeout value in milliseconds. This timeout is implemented by java.net.Socket.
     * Default is infinite timeout.
     */
    public static final String MAIL_SMTP_CONNECTIONTIMEOUT = "mail.smtp.connectiontimeout";

    /**
     * Socket read timeout value in milliseconds. This timeout is implemented by java.net.Socket.
     * Default is infinite timeout.
     */
    public static final String MAIL_SMTP_TIMEOUT = "mail.smtp.timeout";

    /**
     * If <code>true</code>, attempt to authenticate the user using the AUTH command. Defaults to <code>false</code>.
     */
    public static final String MAIL_SMTP_AUTH = "mail.smtp.auth";

    /**
     * If set, lists the authentication mechanisms to consider, and the order in which to consider them.
     * Only mechanisms supported by the server and supported by the current implementation will be used.
     * The default is "LOGIN PLAIN DIGEST-MD5 NTLM", which includes all the authentication mechanisms
     * supported by the current implementation except "XOAUTH2".
     */
    public static final String MAIL_SMTP_AUTH_MECHANISMS = "mail.smtp.auth.mechanisms";

    /**
     * If <code>true</code>, prevents use of the "AUTH LOGIN" command. Default is <code>false</code>.
     */
    public static final String MAIL_SMTP_AUTH_LOGIN_DISABLE = "mail.smtp.auth.login.disable";

    /**
     * If <code>true</code>, prevents use of the <code>AUTH PLAIN</code> command. Default is <code>false</code>.
     */
    public static final String MAIL_SMTP_AUTH_PLAIN_DISABLE = "mail.smtp.auth.plain.disable";

    /**
     * If <code>true</code>, prevents use of the "AUTH DIGEST-MD5" command. Default is <code>false</code>.
     */
    public static final String MAIL_SMTP_AUTH_DIGEST_MD5_DISABLE = "mail.smtp.auth.digest-md5.disable";

    /**
     * If <code>true</code>, prevents use of the "AUTHENTICATE XOAUTH2" command. Because the OAuth 2.0
     * protocol requires a special access token instead of a password, this mechanism is disabled by default.
     * Enable it by explicitly setting this property to "false" or by setting the "mail.smtp.auth.mechanisms" property
     * to "XOAUTH2".
     */
    public static final String MAIL_SMTP_AUTH_XOAUTH2_DISABLE = "mail.smtp.auth.xoauth2.disable";

    /**
     * If <code>true</code>, enables the use of the "STARTTLS" command (if supported by the server) to switch the
     * connection to a TLS-protected connection before issuing any login commands. If the server does not support
     * "STARTTLS", the connection continues without the use of TLS; see the "mail.smtp.starttls.required" property to
     * fail if "STARTTLS" isn't supported. Note that an appropriate trust store must be configured so that the client
     * will trust the server's certificate.
     * Defaults to <code>false</code>.
     */
    public static final String MAIL_SMTP_STARTTLS_ENABLE = "mail.smtp.starttls.enable";

    /**
     * If <code>true</code>, requires the use of the "STARTTLS" command. If the server doesn't support the
     * "STARTTLS" command, or the command fails, the connect method will fail.
     * Defaults to <code>false</code>.
     */
    public static final String MAIL_SMTP_STARTTLS_REQUIRED = "mail.smtp.starttls.required";

    /**
     * If set to <code>true</code>, use SSL to connect and use the SSL port by default.
     * Defaults to <code>false</code> for the "smtp" protocol and <code>true</code> for the "smtps" protocol.
     */
    public static final String MAIL_SMTP_SSL_ENABLE = "mail.smtp.ssl.enable";

    /**
     * If set to <code>true</code>, check the server identity as specified by
     * <a href="https://datatracker.ietf.org/doc/html/rfc2595">RFC 2595</a>. These additional checks based on the
     * content of the server's certificate are intended to prevent man-in-the-middle attacks.
     * Defaults to <code>false</code>.
     */
    public static final String MAIL_SMTP_SSL_CHECKSERVERIDENTITY = "mail.smtp.ssl.checkserveridentity";

    /**
     * If set, and a socket factory hasn't been specified, enables use of a
     * <a href="https://github.com/javaee/javamail/blob/master/mail/src/main/java/com/sun/mail/util/MailSSLSocketFactory.java">MailSSLSocketFactory</a>.
     * If set to "*", all hosts are trusted. If set to a whitespace separated list of hosts, those hosts are trusted.
     * Otherwise, trust depends on the certificate the server presents.
     */
    public static final String MAIL_SMTP_SSL_TRUST = "mail.smtp.ssl.trust";

    // Default values
    public static final boolean DEFAULT_MAIL_DEBUG = false;
    public static final String DEFAULT_MAIL_SMTP_HOST = "";
    public static final int DEFAULT_MAIL_SMTP_PORT = 25;
    public static final boolean DEFAULT_MAIL_SMTP_AUTH = false;
    public static final boolean DEFAULT_MAIL_SMTP_AUTH_DIGEST_MD5_DISABLE = false;
    public static final boolean DEFAULT_MAIL_SMTP_AUTH_XOAUTH2_DISABLE = true;
    public static final StartTLSOption DEFAULT_MAIL_SMTP_STARTTLS = StartTLSOption.DISABLED;
    public static final boolean DEFAULT_MAIL_SMTP_SSL_ENABLE = false;
    public static final boolean DEFAULT_MAIL_SMTP_SSL_CHECKSERVERIDENTITY = false;

    /**
     * Creates a new instance of MailConfig.
     *
     * @return a new <code>MailConfig</code> instance.
     */
    @NotNull
    public static MailConfig create() {
        return new MailConfigImpl();
    }

    /**
     * Gets the mail properties.
     *
     * @return a {@link Properties} object containing mail properties
     */
    @NotNull
    Properties getProperties();

    /**
     * Checks if mail debugging is enabled.
     *
     * @return <code>true</code> if mail debugging is enabled, <code>false</code> otherwise
     */
    boolean isMailDebug();

    /**
     * Sets the mail debugging option.
     *
     * @param mailDebug <code>true</code> to enable mail debugging, <code>false</code> to disable it
     * @return the updated <code>MailConfig</code> instance for method chaining
     */
    MailConfig setMailDebug(boolean mailDebug);

    /**
     * Gets the SMTP user.
     *
     * @return the SMTP user as a string
     */
    String getMailSmtpUser();

    /**
     * Sets the SMTP user.
     *
     * @param mailSmtpUser the SMTP user to set
     * @return the updated <code>MailConfig</code> instance for method chaining
     */
    MailConfig setMailSmtpUser(String mailSmtpUser);

    /**
     * Gets the SMTP host.
     *
     * @return the SMTP host
     */
    String getMailSmtpHost();

    /**
     * Sets the SMTP host.
     *
     * @param mailSmtpHost the SMTP host to set
     * @return the updated <code>MailConfig</code> instance for method chaining
     */
    MailConfig setMailSmtpHost(String mailSmtpHost);

    /**
     * Gets the SMTP port.
     *
     * @return the SMTP port
     */
    int getMailSmtpPort();

    /**
     * Sets the SMTP port.
     *
     * @param mailSmtpPort the SMTP port to set
     * @return the updated <code>MailConfig</code> instance for method chaining
     */
    MailConfig setMailSmtpPort(int mailSmtpPort);

    /**
     * Gets the SMTP connection timeout.
     *
     * @return the SMTP connection timeout in milliseconds
     */
    int getMailSmtpConnectionTimeout();

    /**
     * Sets the SMTP connection timeout.
     *
     * @param mailSmtpConnectionTimeout the SMTP connection timeout in milliseconds
     * @return the updated <code>MailConfig</code> instance for method chaining
     */
    MailConfig setMailSmtpConnectionTimeout(int mailSmtpConnectionTimeout);

    /**
     * Gets the SMTP timeout.
     *
     * @return the SMTP timeout in milliseconds
     */
    int getMailSmtpTimeout();

    /**
     * Sets the SMTP timeout.
     *
     * @param mailSmtpTimeout the SMTP timeout in milliseconds
     * @return the updated <code>MailConfig</code> instance for method chaining
     */
    MailConfig setMailSmtpTimeout(int mailSmtpTimeout);

    /**
     * Checks if SMTP authentication is enabled.
     *
     * @return <code>true</code> if SMTP authentication is enabled, <code>false</code> otherwise
     */
    boolean isMailSmtpAuth();

    /**
     * Sets the SMTP authentication option.
     *
     * @param mailSmtpAuth <code>true</code> to enable SMTP authentication, <code>false</code> to disable
     * @return the updated <code>MailConfig</code> instance for method chaining
     */
    MailConfig setMailSmtpAuth(boolean mailSmtpAuth);

    /**
     * Gets the SMTP authentication mechanisms.
     *
     * @return a string representing the SMTP authentication mechanisms
     */
    String getMailSmtpAuthMechanisms();

    /**
     * Sets the SMTP authentication mechanisms.
     *
     * @param mailSmtpAuthMechanisms the SMTP authentication mechanisms to set
     * @return the updated <code>MailConfig</code> instance for method chaining
     */
    MailConfig setMailSmtpAuthMechanisms(String mailSmtpAuthMechanisms);

    /**
     * Checks if the "AUTH LOGIN" command is disabled.
     *
     * @return <code>true</code> if the "AUTH LOGIN" command is disabled, <code>false</code> otherwise
     */
    boolean isMailSmtpAuthLoginDisable();

    /**
     * Sets the option to disable the "AUTH LOGIN" command.
     *
     * @param mailSmtpAuthLoginDisable <code>true</code> to disable the "AUTH LOGIN" command,
     *                                 <code>false</code> to enable
     * @return the updated <code>MailConfig</code> instance for method chaining
     */
    MailConfig setMailSmtpAuthLoginDisable(boolean mailSmtpAuthLoginDisable);

    /**
     * Checks if the "AUTH PLAIN" command is disabled.
     *
     * @return <code>true</code> if the "AUTH PLAIN" command is disabled, <code>false</code> otherwise
     */
    boolean isMailSmtpAuthPlainDisable();

    /**
     * Sets the option to disable the "AUTH PLAIN" command.
     *
     * @param mailSmtpAuthPlainDisable <code>true</code> to disable the "AUTH PLAIN" command,
     *                                 <code>false</code> to enable
     * @return the updated <code>MailConfig</code> instance for method chaining
     */
    MailConfig setMailSmtpAuthPlainDisable(boolean mailSmtpAuthPlainDisable);

    /**
     * Checks if the "AUTH DIGEST-MD5" command is disabled.
     *
     * @return <code>true</code> if the "AUTH DIGEST-MD5" command is disabled, <code>false</code> otherwise
     */
    boolean isMailSmtpAuthDigestMd5Disable();

    /**
     * Sets the option to disable the "AUTH DIGEST-MD5" command.
     *
     * @param mailSmtpAuthDigestMd5Disable <code>true</code> to disable the "AUTH DIGEST-MD5" command,
     *                                     <code>false</code> to enable
     * @return the updated <code>MailConfig</code> instance for method chaining
     */
    MailConfig setMailSmtpAuthDigestMd5Disable(boolean mailSmtpAuthDigestMd5Disable);

    /**
     * Checks if the "AUTH XOAUTH2" command is disabled.
     *
     * @return <code>true</code> if the "AUTH XOAUTH2" command is disabled, <code>false</code> otherwise
     */
    boolean isMailSmtpAuthXOAuth2Disable();

    /**
     * Sets the option to disable the "AUTH XOAUTH2" command.
     *
     * @param mailSmtpAuthXOAuth2Disable <code>true</code> to disable the "AUTH XOAUTH2" command,
     *                                   <code>false</code> to enable
     * @return the updated <code>MailConfig</code> instance for method chaining
     */
    MailConfig setMailSmtpAuthXOAuth2Disable(boolean mailSmtpAuthXOAuth2Disable);

    /**
     * Gets the STARTTLS option.
     *
     * @return the STARTTLS option
     */
    StartTLSOption getMailSmtpStartTLS();

    /**
     * Sets the STARTTLS option.
     *
     * @param mailSmtpStarttls the STARTTLS option to set
     * @return the updated <code>MailConfig</code> instance for method chaining
     */
    MailConfig setMailSmtpStartTLS(StartTLSOption mailSmtpStarttls);

    /**
     * Checks if SSL is enabled for SMTP.
     *
     * @return <code>true</code> if SSL is enabled, <code>false</code> otherwise
     */
    boolean isMailSmtpSslEnable();

    /**
     * Sets the option to enable SSL for SMTP.
     *
     * @param mailSmtpSslEnable <code>true</code> to enable SSL, <code>false</code> to disable
     * @return the updated <code>MailConfig</code> instance for method chaining
     */
    MailConfig setMailSmtpSslEnable(boolean mailSmtpSslEnable);

    /**
     * Checks if the server identity is checked for SMTP SSL.
     *
     * @return <code>true</code> if the server identity is checked, <code>false</code> otherwise
     */
    boolean isMailSmtpSslCheckServerIdentity();

    /**
     * Sets the option to check the server identity for SMTP SSL.
     *
     * @param mailSmtpSslCheckServerIdentity <code>true</code> to check the server identity,
     *                                       <code>false</code> to disable
     * @return the updated <code>MailConfig</code> instance for method chaining
     */
    MailConfig setMailSmtpSslCheckServerIdentity(boolean mailSmtpSslCheckServerIdentity);

    /**
     * Gets the SMTP SSL trust setting.
     *
     * @return a string representing the SMTP SSL trust setting
     */
    String getMailSmtpSslTrust();

    /**
     * Sets the SMTP SSL trust setting.
     *
     * @param mailSmtpSslTrust the SMTP SSL trust setting to set
     * @return the updated <code>MailConfig</code> instance for method chaining
     */
    MailConfig setMailSmtpSslTrust(String mailSmtpSslTrust);
}
