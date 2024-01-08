package one.jpro.platform.auth.core.basic;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.ResourceBundle;

/**
 * This class represents a login pane. It is designed to provide a user interface for login functionality.
 * The pane includes labels, text fields for username and password, and a submit button.
 * <p>
 * It uses {@link UsernamePasswordCredentials} for binding username and password properties.
 * <p>
 * Style classes are used for each component for easy customization.
 *
 * @author Besmir Beqiri
 */
public class LoginPane extends VBox {

    private static final String DEFAULT_STYLE_CLASS = "login-pane";

    private final Label headerLabel;
    private final Label usernameLabel;
    private final TextField usernameField;
    private final Label passwordLabel;
    private final PasswordField passwordField;
    private final Button submitButton;

    /**
     * Constructs a new LoginPane with the specified credentials.
     * This constructor uses a default resource bundle for localization.
     *
     * @param credentials The {@code UsernamePasswordCredentials} object
     *                    containing the credentials for the login.
     *                    This object cannot be {@code null}.
     * @throws NullPointerException if {@code credentials} is {@code null}.
     */
    public LoginPane(@NotNull UsernamePasswordCredentials credentials) {
        this(credentials, ResourceBundle.getBundle("one.jpro.platform.auth.core.basic.login-pane"));
    }

    /**
     * Constructs a new LoginPane with the specified credentials and
     * a resource bundle for localization.
     *
     * @param credentials the {@code UsernamePasswordCredentials} object containing the credentials for the login
     * @param langBundle  the {@code ResourceBundle} used for localizing the UI text
     * @throws NullPointerException if either {@code credentials} or {@code langBundle} is {@code null}.
     */
    public LoginPane(@NotNull UsernamePasswordCredentials credentials,
                     @NotNull ResourceBundle langBundle) {
        Objects.requireNonNull(credentials, "Credentials cannot be null");
        Objects.requireNonNull(langBundle, "Language bundle cannot be null");

        getStyleClass().add(DEFAULT_STYLE_CLASS);

        headerLabel = new Label(langBundle.getString("label.header"));
        headerLabel.getStyleClass().add("header-label");

        usernameLabel = new Label(langBundle.getString("label.username"));
        usernameLabel.getStyleClass().add("username-label");
        usernameField = new TextField();
        usernameField.getStyleClass().add("username-field");
        credentials.usernameProperty().bind(usernameField.textProperty());

        passwordLabel = new Label(langBundle.getString("label.password"));
        passwordLabel.getStyleClass().add("password-label");
        passwordField = new PasswordField();
        passwordField.getStyleClass().add("password-field");
        credentials.passwordProperty().bind(passwordField.textProperty());

        submitButton = new Button(langBundle.getString("button.submit"));
        submitButton.getStyleClass().add("submit-button");
        submitButton.setDefaultButton(true);

        getChildren().addAll(headerLabel, usernameLabel, usernameField, passwordLabel, passwordField, submitButton);
    }

    /**
     * Returns the header label of the login pane.
     *
     * @return the header label
     */
    @NotNull
    public final Label getHeaderLabel() {
        return headerLabel;
    }

    /**
     * Returns the username label of the login pane.
     *
     * @return the username label
     */
    @NotNull
    public final Label getUsernameLabel() {
        return usernameLabel;
    }

    /**
     * Returns the username text field of the login pane.
     *
     * @return the username text field
     */
    @NotNull
    public final TextField getUsernameField() {
        return usernameField;
    }

    /**
     * Returns the password label of the login pane.
     *
     * @return the password label
     */
    @NotNull
    public final Label getPasswordLabel() {
        return passwordLabel;
    }

    /**
     * Returns the password field of the login pane.
     *
     * @return the password field
     */
    @NotNull
    public final PasswordField getPasswordField() {
        return passwordField;
    }

    /**
     * Returns the submit button of the login pane.
     *
     * @return the submit button
     */
    @NotNull
    public final Button getSubmitButton() {
        return submitButton;
    }
}
