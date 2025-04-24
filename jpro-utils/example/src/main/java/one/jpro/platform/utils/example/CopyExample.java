package one.jpro.platform.utils.example;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import one.jpro.platform.utils.CopyUtil;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CopyExample extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Instructional label
        Label instructionLabel = new Label("Enter text to be copied to the clipboard:");

        // TextField with initial text that will be copied.
        TextField textField = new TextField("Hello, world!");
        textField.setPromptText("Enter text to copy");

        // Button that triggers the copy action.
        Button copyButton = new Button("Copy Text");

        // Initially install the copy handler on the button using the TextField's text.
        CopyUtil.setCopyOnClick(copyButton, textField.getText());

        // Update the copy text on the button whenever the text in the text field changes.
        textField.textProperty().addListener((obs, oldText, newText) -> {
            CopyUtil.setCopyOnClick(copyButton, newText);
        });

        var nullButton = new Button("Set to null");
        nullButton.setOnAction(e -> {
            CopyUtil.setCopyOnClick(copyButton, null);
        });

        var copyException = new Button("Copy Exception");
        CopyUtil.setCopyOnClick(copyException, getExceptionText());

        var specialCharacters = "\r$\n\t\\\"'<>";
        var copySpecialCharactersButton = new Button(specialCharacters);
        CopyUtil.setCopyOnClick(copySpecialCharactersButton, specialCharacters);

        // Use a VBox to layout the UI components neatly.
        VBox root = new VBox(15, instructionLabel, textField, copyButton, nullButton, copyException, copySpecialCharactersButton);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 20; -fx-background-color: #f4f4f4;");

        Scene scene = new Scene(root, 400, 250);
        primaryStage.setTitle("Copy Text Utility");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public String getExceptionText() {
        var e = new Exception();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String exceptionAsString = sw.toString();
        return exceptionAsString;
    }
}
