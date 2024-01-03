package one.jpro.platform.auth.example.basic.page;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import one.jpro.platform.auth.core.basic.UsernamePasswordCredentials;
import one.jpro.platform.auth.core.basic.provider.BasicAuthenticationProvider;
import one.jpro.platform.auth.example.basic.BasicLoginApp;

/**
 * Basic login page with username and password.
 *
 * @author Besmir Beqiri
 */
public class LoginPage extends Page {

    public LoginPage(BasicLoginApp app,
                     BasicAuthenticationProvider authProvider,
                     UsernamePasswordCredentials credentials) {
        final var loginPane = new VBox();
        loginPane.getStyleClass().addAll("login-pane", "basic");
        loginPane.setStyle("-fx-max-width: 480px; -fx-max-height: 400px;");

        final var headerLabel = new Label("Basic Login Form");
        headerLabel.getStyleClass().add("header-label");
        loginPane.getChildren().add(headerLabel);

        final var usernameLabel = new Label("Username : ");
        loginPane.getChildren().add(usernameLabel);
        final var usernameField = new TextField();
        loginPane.getChildren().add(usernameField);

        final var passwordLabel = new Label("Password : ");
        loginPane.getChildren().add(passwordLabel);
        final var passwordField = new PasswordField();
        loginPane.getChildren().add(passwordField);

        final var submitButton = new Button("Login");
        submitButton.setDefaultButton(true);
        loginPane.getChildren().add(submitButton);
        submitButton.setOnAction(event -> {
            credentials.setUsername(usernameField.getText());
            credentials.setPassword(passwordField.getText());
            app.getSessionManager().gotoURL(authProvider.getAuthorizationPath());
        });

        getChildren().add(loginPane);
    }
}
