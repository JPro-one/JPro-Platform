package one.jpro.platform.playwright.testapp;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import one.jpro.platform.routing.Response;
import one.jpro.platform.routing.Route;
import one.jpro.platform.routing.RouteApp;
import one.jpro.platform.routing.Transformers;

import static one.jpro.platform.routing.Route.get;
import static one.jpro.platform.routing.Route.redirect;

/**
 * Minimal JPro app used as the target for this module's own Playwright tests, and as a worked
 * example of an app that is testable from Playwright. Every interactive node has an id, and
 * {@code jpro.conf} sets {@code mirrorCSSToDOM = true}, so each is reachable as {@code "#jpro-<id>"}.
 */
public class Main extends RouteApp {

    @Override
    public Route createRoute() {
        return Route.empty()
                .and(redirect("/", "/textinput"))
                .and(get("/textinput", request -> Response.node(createView())))
                .transform(Transformers.fullscreen(true));
    }

    private Node createView() {
        // Live echo: label is bound to the field, so it updates on every keystroke round-trip.
        TextField textField = new TextField();
        textField.setId("textfield");
        Label echo = new Label();
        echo.setId("echo");
        echo.textProperty().bind(textField.textProperty());

        // Commit echo: label updates only on Enter (onAction), demonstrating commit semantics.
        TextField committed = new TextField();
        committed.setId("committed");
        Label committedEcho = new Label();
        committedEcho.setId("committed-echo");
        committed.setOnAction(event -> committedEcho.setText(committed.getText()));

        // Click counter: a full click round-trip probe.
        int[] clicks = {0};
        Label count = new Label("0");
        count.setId("count");
        Button button = new Button("Click");
        button.setId("button");
        button.setOnAction(event -> count.setText(String.valueOf(++clicks[0])));

        return new VBox(textField, echo, committed, committedEcho, button, count);
    }
}
