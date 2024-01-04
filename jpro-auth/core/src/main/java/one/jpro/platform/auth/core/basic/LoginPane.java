package one.jpro.platform.auth.core.basic;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

/**
 * Login pane.
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

    @NotNull
    public final Label getHeaderLabel() {
        return headerLabel;
    }

    @NotNull
    public final Label getUsernameLabel() {
        return usernameLabel;
    }

    @NotNull
    public final TextField getUsernameTextField() {
        return usernameTextField;
    }

    @NotNull
    public final Label getPasswordLabel() {
        return passwordLabel;
    }

    @NotNull
    public final PasswordField getPasswordField() {
        return passwordField;
    }

    @NotNull
    public final Button getSubmitButton() {
        return submitButton;
    }
}
