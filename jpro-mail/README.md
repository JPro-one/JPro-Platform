# JPro Mail
A library that provides platform-independent and protocol-independent API for sending emails. The receiving of emails is
not yet implemented, but will be added in the future. These are currently the major components implemented:

- `MailConfig`: Represents the configuration for a mail client. It provides methods to get and set various properties
related to the mail configuration.

    * Pre-included configurations
        1. **GoogleMailConfig**: Provides a predefined configuration for connecting to the `Google Mail` (Gmail) SMTP
            server. It sets specific properties required to use Gmail's SMTP server for sending emails.
        2. **MailTrapConfig**: Provides a predefined configuration for connecting to the `MailTrap` SMTP server.
            It sets specific properties required to use MailTrap's sandbox environment for sending emails.

    * Usage Example

      ```java
      MailConfig mailConfig = MailConfig.create();
      mailConfig.setMailSmtpAuth(true);
      mailConfig.setMailSmtpStartTLS(StartTLSOption.ENABLED);
      mailConfig.setMailSmtpHost("smtp.gmail.com");
      mailConfig.setMailSmtpPort(587);
        ```

- `MailClient`: Provides methods for creating and managing mail clients. It includes static methods to create instances
of MailClient and a method to create mail messages.

    * Usage Example

      ```java
      MailClient mailClient = MailClient.create(mailConfig, "SENDER_MAIL_USERNAME", "SENDER_MAIL_PASSWORD");
      ```

- `MailMessage`: Provides methods for creating and managing email messages. It includes methods to set and get various 
email attributes such as sender, recipients, subject, and date, as well as methods to send and save the message.

    * Usage Example

      ```java
      MailMessage mailMessage = MailMessage.create(mailClient);
      // Setting the sender's email address
      mailMessage.addFrom("joe.doe@example.com");
      // Setting the recipient email addresses
      mailMessage.setTo(parseMailAddresses("alice@example.com, bob@example.com"));
      // Setting the CC email addresses
      mailMessage.setCc(parseMailAddresses("carol@example.com, dave@example.com"));
      // Setting the BCC email addresses
      mailMessage.setBcc(parseMailAddresses("eve@example.com, frank@example.com"));
      // Setting the subject of the email
      mailMessage.setSubject("Meeting Reminder");
      // Setting the HTML content of the email
      mailMessage.setHtml("<html><p>Dear team,</p><p>This is a reminder for the meeting scheduled at 3 PM tomorrow.</p><p>Best regards,<br>Joe</p></html>");
      // Adding an attachment to the email
      mailMessage.addAttachment(logoFile, "<logo>");
      // Sending the email
      mailMessage.send();
      ```

### Installation
- Gradle
    ```groovy
    dependencies {
        implementation("one.jpro.platform:jpro-mail:0.5.5-SNAPSHOT")
    }
    ```
- Maven
    ```xml
    <dependencies>
      <dependency>
        <groupId>one.jpro.platform</groupId>
        <artifactId>jpro-mail</artifactId>
        <version>0.5.5-SNAPSHOT</version>
      </dependency>
    </dependencies>
    ```

### Launch the examples
[**Compose Mail sample**](https://github.com/JPro-one/jpro-platform/blob/main/jpro-mail/example/src/main/java/one/jpro/platform/mail/example/compose/ComposeMailSample.java)
* As desktop application
  ```shell
  ./gradlew jpro-mail:example:run -Psample=compose-mail
  ```
* As JPro application
  ```shell
  ./gradlew jpro-mail:example:jproRun -Psample=compose-mail
  ```