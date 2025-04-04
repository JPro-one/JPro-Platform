package one.jpro.platform.utils.example;

import com.jpro.webapi.WebAPI;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import one.jpro.platform.utils.UserPlatform;

/**
 * A JavaFX test application showing the current WebAPI values and corresponding
 * UserPlatform details on the left, and providing controls on the right to test custom values.
 * <p>
 * Additionally, it registers a keyboard shortcut (Meta/Control + D) to test the meta key.
 * When the shortcut is triggered, a feedback message is shown for one second.
 * </p>
 */
public class UserPlatformExample extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Retrieve the real WebAPI from the current browser.
        var webAPI = WebAPI.getWebAPI(primaryStage);

        // Left side: Show the current WebAPI values and UserPlatform based on these values.
        VBox leftPane = new VBox(10);
        leftPane.setPadding(new Insets(10));
        leftPane.setAlignment(Pos.TOP_LEFT);
        Label leftTitle = new Label("Current WebAPI & UserPlatform");
        leftTitle.setStyle("-fx-font-weight: bold; -fx-underline: true;");

        // Display the raw WebAPI values.
        String webAPIValues = "WebAPI.getPlatform() / navigator.platform: " + webAPI.getPlatform() + "\n"
                + "WebAPI.getPlatformOld() / navigator.userAgentData.platform: " + webAPI.getPlatformOld() + "\n";

        // Create a UserPlatform instance using the real WebAPI.
        UserPlatform userPlatformFromAPI = new UserPlatform(webAPI);
        // Also create a UserPlatform instance using desktop system properties.
        UserPlatform userPlatformDesktop = UserPlatform.simulateNative();

        String currentInfo = webAPIValues + "\n"
                + getUserPlatformInfo("UserPlatform from WebAPI", userPlatformFromAPI)
                + "\n"
                + getUserPlatformInfo("UserPlatform from Desktop", userPlatformDesktop);

        Label leftLabel = new Label(currentInfo);
        leftPane.getChildren().addAll(leftTitle, leftLabel);

        // Right side: UI to input custom values.
        GridPane rightPane = new GridPane();
        rightPane.setPadding(new Insets(10));
        rightPane.setHgap(10);
        rightPane.setVgap(10);

        Label rightTitle = new Label("Custom UserPlatform Tester");
        rightTitle.setStyle("-fx-font-weight: bold; -fx-underline: true;");
        rightPane.add(rightTitle, 0, 0, 2, 1);

        Label platformLabel = new Label("Platform:");
        TextField platformField = new TextField();
        platformField.setPromptText("e.g. Windows NT");
        rightPane.add(platformLabel, 0, 1);
        rightPane.add(platformField, 1, 1);

        Label platformOldLabel = new Label("PlatformOld:");
        TextField platformOldField = new TextField();
        platformOldField.setPromptText("e.g. Win32");
        rightPane.add(platformOldLabel, 0, 2);
        rightPane.add(platformOldField, 1, 2);

        Button updateButton = new Button("Update");
        rightPane.add(updateButton, 0, 3, 2, 1);

        Label customResultLabel = new Label();
        rightPane.add(customResultLabel, 0, 4, 2, 1);

        updateButton.setOnAction(e -> {
            String customPlatform = platformField.getText();
            String customPlatformOld = platformOldField.getText();
            UserPlatform customUserPlatform = UserPlatform.simulateWeb(customPlatform, customPlatformOld);
            String result = getUserPlatformInfo("UserPlatform from Custom Values", customUserPlatform);
            customResultLabel.setText(result);
        });

        // Bottom: Feedback label for the shortcut test.
        Label feedbackLabel = new Label("Press Meta/Control + (U) to test shortcut");
        feedbackLabel.setPadding(new Insets(10));

        // Arrange left and right panes in a BorderPane.
        BorderPane root = new BorderPane();
        root.setLeft(leftPane);
        root.setCenter(rightPane);
        root.setBottom(feedbackLabel);

        Scene scene = new Scene(root, 700, 400);
        primaryStage.setTitle("UserPlatform Test");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Register the shortcut based on the meta key from UserPlatform.
        KeyCombination shortcut = new KeyCodeCombination(
                KeyCode.U,
                userPlatformFromAPI.getModifierKeyCombination()
        );

        // Add accelerator to the scene.
        scene.getAccelerators().put(shortcut, () -> {
            feedbackLabel.setText("Shortcut " + shortcut.getDisplayText() + " triggered!");
            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(event -> feedbackLabel.setText("Press Meta/Control + U to test shortcut"));
            pause.play();
        });
    }

    /**
     * Helper method to format UserPlatform information.
     *
     * @param title    Title for the information block.
     * @param platform The UserPlatform instance.
     * @return Formatted string with platform values and method results.
     */
    private static String getUserPlatformInfo(String title, UserPlatform platform) {
        StringBuilder sb = new StringBuilder();
        sb.append("== ").append(title).append(" ==\n");
        sb.append("isWindows: ").append(platform.isWindows()).append("\n");
        sb.append("isMac: ").append(platform.isMac()).append("\n");
        sb.append("isLinux: ").append(platform.isLinux()).append("\n");
        sb.append("isWeb: ").append(platform.isWeb()).append("\n");
        sb.append("isMobile: ").append(platform.isMobile()).append("\n");
        sb.append("MetaKey: ").append(getKeyCodeName(platform.getModifierKey())).append("\n");
        return sb.toString();
    }

    /**
     * Helper method to get a readable name for the KeyCode.
     *
     * @param code the KeyCode
     * @return its name or "null"
     */
    private static String getKeyCodeName(KeyCode code) {
        return (code != null) ? code.getName() : "null";
    }
}
