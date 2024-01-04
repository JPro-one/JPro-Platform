package one.jpro.platform.auth.core.basic;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

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
    private final TextField usernameTextField;
    private final Label passwordLabel;
    private final PasswordField passwordField;
    private final Button submitButton;

    /**
     * Constructs a new LoginPane with the specified credentials.
     *
     * @param credentials the credentials object to bind to the username and password fields
     */
    public LoginPane(@NotNull UsernamePasswordCredentials credentials) {
        getStyleClass().add(DEFAULT_STYLE_CLASS);

        headerLabel = new Label("Basic Login Form");
        headerLabel.getStyleClass().add("header-label");

        usernameLabel = new Label("Username :");
        usernameLabel.getStyleClass().add("username-label");
        usernameTextField = new TextField();
        usernameTextField.getStyleClass().add("username-field");
        credentials.usernameProperty().bind(usernameTextField.textProperty());

        passwordLabel = new Label("Password :");
        passwordLabel.getStyleClass().add("password-label");
        passwordField = new PasswordField();
        passwordField.getStyleClass().add("password-field");
        credentials.passwordProperty().bind(passwordField.textProperty());

        submitButton = new Button("Sign In");
        submitButton.getStyleClass().add("submit-button");
        submitButton.setDefaultButton(true);

        getChildren().addAll(headerLabel, usernameLabel, usernameTextField, passwordLabel, passwordField, submitButton);
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
    public final TextField getUsernameTextField() {
        return usernameTextField;
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
