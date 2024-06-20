package one.jpro.platform.mail;

import org.eclipse.collections.api.list.ImmutableList;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * The MailMessage interface provides methods for creating and managing email messages.
 * It includes methods to set and get various email attributes such as sender, recipients,
 * subject, and date, as well as methods to send and save the message.
 *
 * @author Besmir Beqiri
 */
public interface MailMessage {

    /**
     * Creates a new MailMessage instance using the provided MailClient.
     * <p>
     * This is a factory method that delegates the creation of the MailMessage
     * instance to the provided MailClient. The MailClient is expected to
     * implement the logic for creating a new MailMessage.
     *
     * @param mailClient the {@link MailClient} that will create the {@link MailMessage}
     * @return a new MailMessage instance
     */
    static MailMessage create(MailClient mailClient) {
        return mailClient.createMessage();
    }

    /**
     * Returns the "From" attribute addresses. The "From" attribute
     * contains the identity of the person(s) who wished this message
     * to be sent.
     * <p>
     * This method returns <code>null</code> if this attribute
     * is not present in this message. Returns an empty array if
     * this attribute is present, but contains no addresses.
     *
     * @return immutable list of Strings containing the addresses of the sender
     * @throws MailException if the sender address fails to be retrieved
     */
    ImmutableList<String> getFrom();

    /**
     * Set the "From" attribute address in this message.
     *
     * @param address the sender address
     * @throws MailException if the sender address fails to be set
     */
    void setFrom(String address);

    /**
     * Add these addresses to the existing "From" attribute.
     *
     * @param addresses the sender addresses
     * @throws MailException if the sender addresses fail to be added
     */
    void addFrom(ImmutableList<String> addresses);

    /**
     * Returns the "To" attribute addresses. The "To" attribute
     * contains the primary recipients of this message.
     * <p>
     * This method returns <code>null</code> if this attribute
     * is not present in this message. Returns an empty array if
     * this attribute is present, but contains no addresses.
     *
     * @return immutable list of Strings containing the addresses of the primary recipients
     * @throws MailException if the primary recipient addresses fail to be retrieved
     */
    ImmutableList<String> getTo();

    /**
     * Set the "To" attribute addresses in this message.
     *
     * @param addresses the primary recipient addresses
     * @throws MailException if the primary recipient addresses fail to be set
     */
    void setTo(ImmutableList<String> addresses);

    /**
     * Add these addresses to the existing "To" attribute.
     *
     * @param addresses the primary recipient addresses
     * @throws MailException if the primary recipient addresses fail to be added
     */
    void addTo(ImmutableList<String> addresses);

    /**
     * Return the "Cc" attribute addresses. The "Cc" attribute
     * contains secondary recipients of this message.
     * <p>
     * This method returns <code>null</code> if this attribute
     * is not present in this message. Returns an empty array if
     * this attribute is present, but contains no addresses.
     *
     * @return immutable list of Strings containing the addresses of the secondary recipients
     * @throws MailException if the secondary recipient addresses fail to be retrieved
     */
    ImmutableList<String> getCc();

    /**
     * Set the "Cc" attribute addresses in this message.
     *
     * @param addresses the secondary recipient addresses
     * @throws MailException if the secondary recipient addresses fail to be set
     */
    void setCc(ImmutableList<String> addresses);

    /**
     * Add these addresses to the existing "Cc" attribute.
     *
     * @param addresses the secondary recipient addresses
     * @throws MailException if the secondary recipient addresses fail to be added
     */
    void addCc(ImmutableList<String> addresses);

    /**
     * Return the "Bcc" attribute addresses. The "Bcc" attribute
     * contains tertiary recipients of this message.
     * <p>
     * This method returns <code>null</code> if this attribute
     * is not present in this message. Returns an empty array if
     * this attribute is present, but contains no addresses.
     *
     * @return immutable list of Strings containing the addresses of the tertiary recipients
     * @throws MailException if the tertiary recipient addresses fail to be retrieved
     */
    ImmutableList<String> getBcc();

    /**
     * Set the "Bcc" attribute addresses in this message.
     *
     * @param addresses the tertiary recipient addresses
     * @throws MailException if the tertiary recipient addresses fail to be set
     */
    void setBcc(ImmutableList<String> addresses);

    /**
     * Add these addresses to the existing "Bcc" attribute.
     *
     * @param addresses the tertiary recipient addresses
     * @throws MailException if the tertiary recipient addresses fail to be added
     */
    void addBcc(ImmutableList<String> addresses);

    /**
     * Get all the recipients of this message.
     * This method returns <code>null</code> if the corresponding
     * header is not present. Returns an empty array if the header
     * is present, but contains no addresses.
     *
     * @return all the recipients of this message
     * @throws MailException if the recipients fail to be retrieved
     */
    ImmutableList<String> getAllRecipients();

    /**
     * Get the addresses to which replies should be directed.
     * This will usually be the sender of the message, but
     * some messages may direct replies to a different address.
     * <p>
     * The default implementation simply calls the <code>getFrom</code>
     * method.
     * <p>
     * This method returns <code>null</code> if the corresponding
     * header is not present. Returns an empty array if the header
     * is present, but contains no addresses.
     *
     * @return addresses to which replies should be directed
     * @throws MailException if the reply-to addresses fail to be retrieved
     * @see #getFrom
     */
    ImmutableList<String> getReplyTo();

    /**
     * Set the addresses to which replies should be directed.
     * (Normally only a single address will be specified.)
     * Not all message types allow this to be specified separately
     * from the sender of the message.
     * <p>
     * The default implementation provided here just throws the
     * MethodNotSupportedException.
     *
     * @param addresses addresses to which replies should be directed
     * @throws MailException if the reply-to addresses fail to be set
     */
    void setReplyTo(ImmutableList<String> addresses);

    /**
     * Get the subject of this message.
     *
     * @return the subject string
     * @throws MailException if the subject fails to be retrieved
     */
    String getSubject();

    /**
     * Set the subject of this message.
     *
     * @param subject the subject string
     * @throws MailException if the subject fails to be set
     */
    void setSubject(String subject);

    /**
     * A convenience method that sets the given String as this
     * part's content with a MIME type of "text/plain".
     *
     * @param text the text that is the message's content.
     * @throws MailException if the text fails to be set
     */
    void setText(String text);

    /**
     * Get the date this message was sent.
     *
     * @return the instant this message was sent
     * @throws MailException if the sent date fails to be retrieved
     */
    Instant getSentDate();

    /**
     * Set the sent date of this message.
     *
     * @param instant the instant this message was sent
     * @throws MailException if the sent date fails to be set
     */
    void setSentDate(Instant instant);

    /**
     * Get the date this message was received.
     *
     * @return the instant this message was received
     * @throws MailException if the received date fails to be retrieved
     */
    Instant getReceivedDate();

    /**
     * Save any changes made to this message into the message-store
     * when the containing folder is closed, if the message is contained
     * in a folder. Update any header fields to be consistent with the
     * changed message contents. If any part of a message's headers or
     * contents are changed, this method must be called to ensure that
     * those changes are permanent. If this method is not called, any
     * such modifications may or may not be saved, depending on the
     * message store and folder implementation.
     * <p>
     * Messages obtained from folders opened READ_ONLY should not be
     * modified and <code>saveChanges</code> should not be called on
     * such messages.
     *
     * @return a {@link CompletableFuture} that completes when the changes are saved
     * @throws MailException if the changes fail to be saved
     */
    CompletableFuture<Void> saveChanges();

    /**
     * Send a message. The message will be sent to all recipient
     * addresses specified in the message (as returned from the method
     * {@link MailMessage#getAllRecipients()}),
     * using message transports appropriate to each address.  The
     * <code>send</code> method calls the <code>saveChanges</code>
     * method on the message before sending it.
     * <p>
     * In typical usage, a {@link MailException} reflects an error detected
     * by the server. The details of the {@link MailException} will usually
     * contain the error message from the server (such as an SMTP error
     * message). An address may be detected as invalid for a variety of
     * reasons - the address may not exist, the address may have invalid
     * syntax, the address may have exceeded its quota, etc.
     *
     * @return a {@link CompletableFuture} that completes when the message is sent
     * @throws MailException if the message fails to be sent
     * @see MailMessage#saveChanges
     * @see MailMessage#getAllRecipients
     * @see #send(ImmutableList)
     */
    CompletableFuture<Void> send();

    /**
     * Send the message to the specified addresses, ignoring any
     * recipients specified in the message itself. The
     * <code>send</code> method calls the <code>saveChanges</code>
     * method on the message before sending it.
     *
     * @param addresses the addresses to which to send the message
     * @return a {@link CompletableFuture} that completes when the message is sent
     * @throws MailException if the message fails to be sent
     * @see MailMessage#saveChanges
     * @see #send()
     */
    CompletableFuture<Void> send(ImmutableList<String> addresses);
}
