package one.jpro.platform.auth.example.simple.page;

import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Error page.
 *
 * @author Besmir Beqiri
 */
public class ErrorPage extends Page {

    public ErrorPage(Throwable throwable) {
        final var headerLabel = new Label("Something unexpected happen:");
        headerLabel.getStyleClass().add("header-label");

        final var errorLabel = new Label();
        errorLabel.setWrapText(true);
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setText(throwable == null ? "" : throwable.getMessage());

        final var errorTextArea = new TextArea();
        errorTextArea.getStyleClass().add("error-text-area");
        VBox.setVgrow(errorTextArea, Priority.ALWAYS);
        errorTextArea.setText((throwable == null) ? "" : printStackTrace(throwable));

        final var pane = new VBox(headerLabel, errorLabel, errorTextArea);
        pane.getStyleClass().add("error-pane");

        getChildren().add(pane);
    }

    private String printStackTrace(Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}
