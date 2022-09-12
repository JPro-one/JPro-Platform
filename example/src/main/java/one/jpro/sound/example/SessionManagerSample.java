package one.jpro.sound.example;

import com.jpro.webapi.WebAPI;
import javafx.application.Application;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import one.jpro.sessionmanager.SessionManager;

public class SessionManagerSample extends Application {

    static SessionManager sm = new SessionManager("example-sm");

    @Override
    public void start(Stage primaryStage) throws Exception {
        ObservableMap session = sm.getSession(WebAPI.getWebAPI(primaryStage));

        VBox pin = new VBox();
        pin.setAlignment(Pos.CENTER);

        Label title = new Label("Session Manager Sample");
        title.setFont(new Font(25));
        pin.getChildren().add(title);

        Button button1 = new Button("add");
        TextField t1 = new TextField();
        t1.setPromptText("key");
        TextField t2 = new TextField();
        t2.setPromptText("value");
        button1.setOnAction(e -> {
            session.put(t1.getText(), t2.getText());
        });
        HBox hb1 = new HBox(button1, t1, t2);
        hb1.setAlignment(Pos.CENTER);
        pin.getChildren().add(hb1);

        Button button2 = new Button("remove");
        TextField t3 = new TextField();
        t3.setPromptText("key");
        button2.setOnAction(e -> {
            session.remove(t3.getText());
        });
        HBox hb2 = new HBox(button2, t3);
        hb2.setAlignment(Pos.CENTER);
        pin.getChildren().add(hb2);

        // Show the content of the session
        Label sessionLabel = new Label();
        sessionLabel.setFont(new Font(15));
        session.addListener((MapChangeListener) c -> {
            sessionLabel.setText("Session: " + session);
        });
        sessionLabel.setText("Session: " + session);
        pin.getChildren().add(sessionLabel);

        primaryStage.setTitle("SessionManagerSample");
        primaryStage.setScene(new Scene(pin));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
