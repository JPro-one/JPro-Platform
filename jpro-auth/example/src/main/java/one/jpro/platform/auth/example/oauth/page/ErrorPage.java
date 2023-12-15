package one.jpro.platform.auth.example.oauth.page;

import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import one.jpro.platform.auth.example.oauth.OAuthApp;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Error page.
 *
 * @author Besmir Beqiri
 */
public class ErrorPage extends Page {

    public ErrorPage(OAuthApp loginApp) {
        final var headerLabel = new Label("Something unexpected happen:");
        headerLabel.getStyleClass().add("header-label");

        final var errorLabel = new Label();
        errorLabel.setWrapText(true);
        errorLabel.getStyleClass().add("error-label");
        errorLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            final Throwable throwable = loginApp.getError();
            return throwable == null ? "" : throwable.getMessage();
        }, loginApp.errorProperty()));

        final var errorTextArea = new TextArea();
        errorTextArea.getStyleClass().add("error-text-area");
        VBox.setVgrow(errorTextArea, Priority.ALWAYS);
        errorTextArea.textProperty().bind(Bindings.createStringBinding(() -> {
            final Throwable throwable = loginApp.getError();
            if (throwable == null) {
                return "";
            } else {
                final StringWriter sw = new StringWriter();
                final PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                return sw.toString();
            }
        }, loginApp.errorProperty()));

        final var pane = new VBox(headerLabel, errorLabel, errorTextArea);
        pane.getStyleClass().add("error-pane");

        getChildren().add(pane);
    }
}
