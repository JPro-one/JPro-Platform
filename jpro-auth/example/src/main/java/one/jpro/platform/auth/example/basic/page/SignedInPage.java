package one.jpro.platform.auth.example.basic.page;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import one.jpro.platform.auth.example.basic.BasicLoginApp;

/**
 * Signed in page.
 *
 * @author Besmir Beqiri
 */
public class SignedInPage extends Page {

    public SignedInPage(BasicLoginApp app) {
        final var headerLabel = new Label("Not signed in.");
        headerLabel.getStyleClass().add("header-label");

        final var user = app.getUser();
        if (user == null) {
            getChildren().add(headerLabel);
        } else {
            headerLabel.setText("Signed in as user: " + user.getName());

            final var userInfoTextArea = new TextArea();
            userInfoTextArea.setWrapText(true);
            userInfoTextArea.setEditable(false);
            userInfoTextArea.setPrefHeight(600.0);
            userInfoTextArea.setText(user.toJSON().toString());

            final var signOutButton = new Button("Sign out");
            signOutButton.setOnAction(event -> {
                app.setUser(null);
                app.getSessionManager().gotoURL("/");
            });
            signOutButton.setDefaultButton(true);

            final var pane = new VBox(headerLabel, userInfoTextArea, signOutButton);
            pane.getStyleClass().add("signed-in-user-pane");

            getChildren().add(pane);
        }
    }
}
