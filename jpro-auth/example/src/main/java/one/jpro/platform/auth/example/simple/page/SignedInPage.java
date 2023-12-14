package one.jpro.platform.auth.example.simple.page;

import javafx.beans.binding.Bindings;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import one.jpro.platform.auth.core.oauth2.OAuth2AuthenticationProvider;
import one.jpro.platform.auth.example.simple.SimpleApp;
import simplefx.experimental.parts.FXFuture;

/**
 * Signed in page.
 *
 * @author Besmir Beqiri
 */
public class SignedInPage extends Page {

    public SignedInPage(SimpleApp app,
                        OAuth2AuthenticationProvider authProvider) {
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
            userInfoTextArea.textProperty().bind(Bindings.createStringBinding(() ->
                    user.toJSON().toString(), app.userProperty()));

            final var signOutButton = new Button("Sign out");
            signOutButton.setOnAction(event ->
                    FXFuture.fromJava(authProvider.revoke(app.getUser(), "access_token"))
                            .onSuccess(nothing -> {
                                app.setUser(null);
                                app.getSessionManager().gotoURL("/");
                                return nothing;
                            }));
            signOutButton.setDefaultButton(true);

            final var pane = new VBox(headerLabel, userInfoTextArea, signOutButton);
            pane.getStyleClass().add("signed-in-user-pane");

            getChildren().add(pane);
        }
    }
}
