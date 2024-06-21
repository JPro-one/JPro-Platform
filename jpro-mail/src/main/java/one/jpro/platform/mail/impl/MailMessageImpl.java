package one.jpro.platform.mail.impl;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import one.jpro.platform.mail.MailException;
import one.jpro.platform.mail.MailMessage;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

import java.io.File;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

/**
 * Mail message implementation.
 *
 * @author Besmir Beqiri
 */
public class MailMessageImpl implements MailMessage {

    private final Message message;
    private final Multipart multipart;

    MailMessageImpl(Session session) {
        this.message = new MimeMessage(session);
        this.multipart = new MimeMultipart();
    }

    @Override
    public ImmutableList<String> getFrom() {
        final ImmutableList<String> fromAddresses;
        try {
            final Address[] addresses = message.getFrom();
            fromAddresses = Lists.immutable.of(addresses).collect(Address::toString);
        } catch (MessagingException ex) {
            throw new MailException("Failed to get the addresses from the \"From\" attribute in this message", ex);
        }

        return fromAddresses;
    }

    @Override
    public void setFrom(String address) {
        try {
            message.setFrom(new InternetAddress(address));
        } catch (MessagingException ex) {
            throw new MailException("Failed to set the address to the \"From\" attribute in this message", ex);
        }
    }

    @Override
    public void addFrom(ImmutableList<String> addresses) {
        try {
            Address[] fromAddresses = new InternetAddress[addresses.size()];
            for (int i = 0; i < addresses.size(); i++) {
                fromAddresses[i] = new InternetAddress(addresses.get(i));
            }
            message.addFrom(fromAddresses);
        } catch (MessagingException ex) {
            throw new MailException("Failed to add the addresses to the existing \"From\" attribute in this message", ex);
        }
    }

    @Override
    public ImmutableList<String> getTo() {
        final ImmutableList<String> toAddresses;
        try {
            final Address[] addresses = message.getRecipients(Message.RecipientType.TO);
            toAddresses = Lists.immutable.of(addresses).collect(Address::toString);
        } catch (MessagingException ex) {
            throw new MailException("Failed to get the addresses from the \"To\" attribute in this message", ex);
        }
        return toAddresses;
    }

    @Override
    public void setTo(String address) {
        this.setTo(Lists.immutable.of(address));
    }

    @Override
    public void setTo(ImmutableList<String> addresses) {
        try {
            Address[] toAddresses = new InternetAddress[addresses.size()];
            for (int i = 0; i < addresses.size(); i++) {
                toAddresses[i] = new InternetAddress(addresses.get(i));
            }
            message.setRecipients(Message.RecipientType.TO, toAddresses);
        } catch (MessagingException ex) {
            throw new MailException("Failed to set the addresses to the \"To\" attribute in this message", ex);
        }
    }

    @Override
    public void addTo(ImmutableList<String> addresses) {
        try {
            Address[] toAddresses = new InternetAddress[addresses.size()];
            for (int i = 0; i < addresses.size(); i++) {
                toAddresses[i] = new InternetAddress(addresses.get(i));
            }
            message.addRecipients(Message.RecipientType.TO, toAddresses);
        } catch (MessagingException ex) {
            throw new MailException("Failed to add the addresses to the existing \"To\" attribute in this message", ex);
        }
    }

    @Override
    public ImmutableList<String> getCc() {
        final ImmutableList<String> ccAddresses;
        try {
            final Address[] addresses = message.getRecipients(Message.RecipientType.CC);
            ccAddresses = Lists.immutable.of(addresses).collect(Address::toString);
        } catch (MessagingException ex) {
            throw new MailException("Failed to get the addresses from the \"Cc\" attribute in this message", ex);
        }
        return ccAddresses;
    }

    @Override
    public void setCc(ImmutableList<String> addresses) {
        try {
            Address[] ccAddresses = new InternetAddress[addresses.size()];
            for (int i = 0; i < addresses.size(); i++) {
                ccAddresses[i] = new InternetAddress(addresses.get(i));
            }
            message.setRecipients(Message.RecipientType.CC, ccAddresses);
        } catch (MessagingException ex) {
            throw new MailException("Failed to set the addresses to the \"Cc\" attribute in this message", ex);
        }
    }

    @Override
    public void addCc(ImmutableList<String> addresses) {
        try {
            Address[] ccAddresses = new InternetAddress[addresses.size()];
            for (int i = 0; i < addresses.size(); i++) {
                ccAddresses[i] = new InternetAddress(addresses.get(i));
            }
            message.addRecipients(Message.RecipientType.CC, ccAddresses);
        } catch (MessagingException ex) {
            throw new MailException("Failed to add the addresses to the existing \"Cc\" attribute in this message", ex);
        }
    }

    @Override
    public ImmutableList<String> getBcc() {
        final ImmutableList<String> bccAddresses;
        try {
            final Address[] addresses = message.getRecipients(Message.RecipientType.BCC);
            bccAddresses = Lists.immutable.of(addresses).collect(Address::toString);
        } catch (MessagingException ex) {
            throw new MailException("Failed to get the addresses from the \"Bcc\" attribute in this message", ex);
        }
        return bccAddresses;
    }

    @Override
    public void setBcc(ImmutableList<String> addresses) {
        try {
            Address[] bccAddresses = new InternetAddress[addresses.size()];
            for (int i = 0; i < addresses.size(); i++) {
                bccAddresses[i] = new InternetAddress(addresses.get(i));
            }
            message.setRecipients(Message.RecipientType.BCC, bccAddresses);
        } catch (MessagingException ex) {
            throw new MailException("Failed to set the addresses to the \"Bcc\" attribute in this message", ex);
        }
    }

    @Override
    public void addBcc(ImmutableList<String> addresses) {
        try {
            Address[] bccAddresses = new InternetAddress[addresses.size()];
            for (int i = 0; i < addresses.size(); i++) {
                bccAddresses[i] = new InternetAddress(addresses.get(i));
            }
            message.addRecipients(Message.RecipientType.BCC, bccAddresses);
        } catch (MessagingException ex) {
            throw new MailException("Failed to add the addresses to the existing \"Bcc\" attribute in this message", ex);
        }
    }

    @Override
    public ImmutableList<String> getAllRecipients() {
        final ImmutableList<String> allRecipients;
        try {
            final Address[] addresses = message.getAllRecipients();
            allRecipients = Lists.immutable.of(addresses).collect(Address::toString);
        } catch (MessagingException ex) {
            throw new MailException("Failed to get all the recipients for this message", ex);
        }
        return allRecipients;
    }

    @Override
    public ImmutableList<String> getReplyTo() {
        final ImmutableList<String> replyToAddresses;
        try {
            final Address[] addresses = message.getReplyTo();
            replyToAddresses = Lists.immutable.of(addresses).collect(Address::toString);
        } catch (MessagingException ex) {
            throw new MailException("Failed to get the addresses from the \"Reply-To\" attribute in this message", ex);
        }
        return replyToAddresses;
    }

    @Override
    public void setReplyTo(ImmutableList<String> addresses) {
        try {
            Address[] replyToAddresses = new InternetAddress[addresses.size()];
            for (int i = 0; i < addresses.size(); i++) {
                replyToAddresses[i] = new InternetAddress(addresses.get(i));
            }
            message.setReplyTo(replyToAddresses);
        } catch (MessagingException ex) {
            throw new MailException("Failed to set the addresses to the \"Reply-To\" attribute in this message", ex);
        }
    }

    @Override
    public String getSubject() {
        try {
            return message.getSubject();
        } catch (MessagingException ex) {
            throw new MailException("Failed to get the subject from this message", ex);
        }
    }

    @Override
    public void setSubject(String subject) {
        try {
            message.setSubject(subject);
        } catch (MessagingException ex) {
            throw new MailException("Failed to set the subject to this message", ex);
        }
    }

    @Override
    public void setText(String text) {
        try {
            message.setText(text);
        } catch (MessagingException ex) {
            throw new MailException("Failed to set the text to this message", ex);
        }
    }

    @Override
    public void setHtml(String htmlText) {
        try {
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(htmlText, "text/html; charset=utf-8");
            multipart.addBodyPart(mimeBodyPart);
            message.setContent(multipart);
        } catch (MessagingException ex) {
            throw new MailException("Failed to set the HTML content to this message", ex);
        }
    }

    @Override
    public void addAttachment(File file) {
        try {
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.attachFile(file);
            multipart.addBodyPart(mimeBodyPart);
            message.setContent(multipart);
        } catch (Exception ex) {
            throw new MailException("Failed to attach the file to this message", ex);
        }
    }

    @Override
    public void addAttachment(File file, String contentType, String encoding) {
        try {
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.attachFile(file, contentType, encoding);
            multipart.addBodyPart(mimeBodyPart);
            message.setContent(multipart);
        } catch (Exception ex) {
            throw new MailException("Failed to attach the file to this message", ex);
        }
    }

    @Override
    public Instant getSentDate() {
        try {
            return message.getSentDate().toInstant();
        } catch (MessagingException ex) {
            throw new MailException("Failed to get the sent date from this message", ex);
        }
    }

    @Override
    public void setSentDate(Instant instant) {
        try {
            message.setSentDate(Date.from(instant));
        } catch (MessagingException ex) {
            throw new MailException("Failed to set the sent date to this message", ex);
        }
    }

    @Override
    public Instant getReceivedDate() {
        try {
            return message.getReceivedDate().toInstant();
        } catch (MessagingException ex) {
            throw new MailException("Failed to get the received date from this message", ex);
        }
    }

    @Override
    public CompletableFuture<Void> saveChanges() {
        return CompletableFuture.runAsync(() -> {
            try {
                message.saveChanges();
            } catch (MessagingException ex) {
                throw new MailException("Failed to save changes to this message", ex);
            }
        });
    }

    @Override
    public CompletableFuture<Void> send() {
        return CompletableFuture.runAsync(() -> {
            try {
                Transport.send(message);
            } catch (MessagingException ex) {
                throw new MailException("Failed to send this message to the specified addresses: "
                        + getAllRecipients(), ex);
            }
        });
    }

    @Override
    public CompletableFuture<Void> send(ImmutableList<String> addresses) {
        return CompletableFuture.supplyAsync(() -> {
            final Address[] addressArray = new Address[addresses.size()];
            for (int i = 0; i < addresses.size(); i++) {
                try {
                    addressArray[i] = new InternetAddress(addresses.get(i));
                } catch (AddressException ex) {
                    throw new MailException("Failed to parse this mail address: " + addresses.get(i), ex);
                }
            }
            return addressArray;
        }).thenAccept(addressesArray -> {
            try {
                Transport.send(message, addressesArray);
            } catch (MessagingException ex) {
                throw new MailException("Failed to send this message to the specified addresses: " + addresses, ex);
            }
        });
    }
}
