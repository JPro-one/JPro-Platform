package one.jpro.platform.mail.example.compose;

import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.Styles;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import one.jpro.platform.mail.MailClient;
import one.jpro.platform.mail.MailConfig;
import one.jpro.platform.mail.MailMessage;
import one.jpro.platform.mail.config.GoogleMailConfig;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;
import org.slf4j.Logger;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * This class provides a sample application for composing and sending emails using the JPro Mail module.
 * It creates a UI for composing an email with fields for the sender, recipient, cc, bcc, subject, and message body.
 *
 * @author Besmir Beqiri
 */
public class ComposeMailSample extends Application {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ComposeMailSample.class);
    private static final String SENDER_MAIL_USERNAME = System.getenv("SENDER_MAIL_USERNAME");
    private static final String SENDER_MAIL_PASSWORD = System.getenv("SENDER_MAIL_PASSWORD");
    private static final String TEST_MAIL_FROM = "bb@sandec.de";
    private static final String TEST_MAIL_TO = "ib@sandec.de";
    private static final ImmutableList<String> TEST_MAIL_CC =
            Lists.immutable.of("fk@sandec.de", "th@sandec.de");
    private static final String TEST_MAIL_SUBJECT = "Test Mail Subject";
    private static final String TEST_MAIL_MESSAGE = "Hello, this is a test mail sent using JPro Mail module.";

    @Override
    public void start(Stage stage) {
        stage.setTitle("JPro Compose Mail Sample");
        Scene scene = new Scene(createRoot(stage), 960, 640);
        Optional.ofNullable(CupertinoLight.class.getResource(new CupertinoLight().getUserAgentStylesheet()))
                .map(URL::toExternalForm)
                .ifPresent(scene.getStylesheets()::add);
        scene.getStylesheets().add(ComposeMailSample.class
                .getResource("css/compose-mail.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    public Parent createRoot(Stage stage) {
        // Creating the header controls
        Label newMessageLabel = new Label("New Message");
        newMessageLabel.getStyleClass().add("new-message-label");

        Label fromLabel = new Label("From:");
        TextField fromTextField = new TextField();
        fromTextField.setText(TEST_MAIL_FROM);
        GridPane.setHgrow(fromTextField, Priority.ALWAYS);

        Label toLabel = new Label("To:");
        TextField toTextField = new TextField();
        toTextField.setText(TEST_MAIL_TO);
        GridPane.setHgrow(toTextField, Priority.ALWAYS);

        Label ccLabel = new Label("Cc:");
        TextField ccTextField = new TextField();
        ccTextField.setText(TEST_MAIL_CC.makeString(", "));

        Label bccLabel = new Label("Bcc:");
        TextField bccTextField = new TextField();

        Label subjectLabel = new Label("Subject:");
        TextField subjectTextField = new TextField();
        subjectTextField.setText(TEST_MAIL_SUBJECT);

        // Creating a GridPane
        GridPane topPane = new GridPane();
        topPane.getStyleClass().add("top-pane");
        topPane.add(newMessageLabel, 0, 0, 2, 1); // Spanning across two columns

        topPane.add(fromLabel, 0, 1);
        topPane.add(fromTextField, 1, 1);

        topPane.add(toLabel, 0, 2);
        topPane.add(toTextField, 1, 2);

        topPane.add(ccLabel, 0, 3);
        topPane.add(ccTextField, 1, 3);

        topPane.add(bccLabel, 0, 4);
        topPane.add(bccTextField, 1, 4);

        topPane.add(subjectLabel, 0, 5);
        topPane.add(subjectTextField, 1, 5);

        TextArea contentMessageArea = new TextArea();
        contentMessageArea.getStyleClass().add("content-message-area");
        contentMessageArea.setText(TEST_MAIL_MESSAGE);

        MailConfig mailConfig = new GoogleMailConfig();
        mailConfig.setMailDebug(true);
        MailClient mailClient = MailClient.create(mailConfig, SENDER_MAIL_USERNAME, SENDER_MAIL_PASSWORD);

        Button sendButton = new Button("Send", new FontIcon(Material2MZ.SEND));
        sendButton.setDefaultButton(true);
        sendButton.setOnAction(event -> {
            LOGGER.info("Sending mail to: {}", toTextField.getText());
            LOGGER.info("Cc: {}", ccTextField.getText());
            LOGGER.info("Bcc: {}", bccTextField.getText());
            LOGGER.info("Subject: {}", subjectTextField.getText());
            LOGGER.info("Message: {}", contentMessageArea.getText());

            MailMessage mailMessage = MailMessage.create(mailClient);
            mailMessage.addFrom(parseMailAddresses(fromTextField.getText()));
            mailMessage.setTo(parseMailAddresses(toTextField.getText()));
            mailMessage.setCc(parseMailAddresses(ccTextField.getText()));
            mailMessage.setBcc(parseMailAddresses(bccTextField.getText()));
            mailMessage.setSubject(subjectTextField.getText());
            mailMessage.setText(contentMessageArea.getText());
            mailMessage.setSentDate(ZonedDateTime.now().minusDays(7).toInstant());
            mailMessage.send()
                    .thenAccept(result -> LOGGER.info("Mail sent successfully on {}", ZonedDateTime.now()))
                    .exceptionally(throwable -> {
                        LOGGER.error("Error sending mail", throwable);
                        return null;
                    });
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button discardButton = new Button("Discard", new FontIcon(Material2AL.DELETE_OUTLINE));
        discardButton.getStyleClass().add(Styles.BUTTON_OUTLINED);
        discardButton.setOnAction(event -> {
            toTextField.clear();
            ccTextField.clear();
            bccTextField.clear();
            subjectTextField.clear();
            contentMessageArea.clear();
        });
        HBox bottomPane = new HBox(sendButton, spacer, discardButton);
        bottomPane.getStyleClass().add("bottom-pane");

        BorderPane rootPane = new BorderPane(contentMessageArea);
        rootPane.getStyleClass().add("root-pane");
        rootPane.setTop(topPane);
        rootPane.setBottom(bottomPane);

        return rootPane;
    }

    /**
     * Parses a comma-separated string of email addresses into an immutable list.
     *
     * @param mailAddresses a comma-separated string of email addresses
     * @return an immutable list of email addresses
     */
    private ImmutableList<String> parseMailAddresses(String mailAddresses) {
        if (mailAddresses == null || mailAddresses.isBlank()) {
            return Lists.immutable.empty();
        }
        return Lists.immutable.of(mailAddresses.split(",")).collect(String::trim);
    }
}
