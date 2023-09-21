package one.jpro.platform.sessions.example;

import atlantafx.base.theme.CupertinoLight;
import com.jpro.webapi.WebAPI;
import javafx.application.Application;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import one.jpro.platform.sessionmanager.SessionManager;

public class SessionManagerSample extends Application {

    static SessionManager sm = new SessionManager("example-sm");

    @Override
    public void start(Stage stage) {
        stage.setTitle("JPro Session Manager Sample");
        Scene scene = new Scene(createRoot(stage), 1000,600);
        scene.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
        stage.setScene(scene);
        stage.show();
    }

    public Parent createRoot(Stage stage) {
        ObservableMap<String,String> session = sm.getSession(WebAPI.getWebAPI(stage));

        VBox rootPane = new VBox();
        rootPane.setAlignment(Pos.CENTER);
        rootPane.setSpacing(8.0);

        Label title = new Label("Session Manager Sample");
        title.setFont(new Font(25));
        rootPane.getChildren().add(title);

        Button button1 = new Button("add");
        TextField t1 = new TextField();
        t1.setPromptText("key");
        TextField t2 = new TextField();
        t2.setPromptText("value");
        button1.setOnAction(e -> session.put(t1.getText(), t2.getText()));
        HBox hb1 = new HBox(button1, t1, t2);
        hb1.setAlignment(Pos.CENTER);
        rootPane.getChildren().add(hb1);

        Button button2 = new Button("remove");
        TextField t3 = new TextField();
        t3.setPromptText("key");
        button2.setOnAction(e -> session.remove(t3.getText()));
        HBox hb2 = new HBox(button2, t3);
        hb2.setAlignment(Pos.CENTER);
        rootPane.getChildren().add(hb2);

        // Show the content of the session
        Label sessionLabel = new Label();
        sessionLabel.setFont(new Font(15));
        session.addListener((MapChangeListener<String, String>) change -> sessionLabel.setText("Session: " + session));
        sessionLabel.setText("Session: " + session);
        rootPane.getChildren().add(sessionLabel);
        return rootPane;
    }
}
