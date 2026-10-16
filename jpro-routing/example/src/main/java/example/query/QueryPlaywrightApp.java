package example.query;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import one.jpro.platform.routing.LinkUtil;
import one.jpro.platform.routing.Request;
import one.jpro.platform.routing.Response;
import one.jpro.platform.routing.Route;
import one.jpro.platform.routing.RouteApp;

import java.util.Map;
import java.util.TreeMap;

/**
 * Test app for the Playwright tests: shows the query parameters of the current request, one label per
 * parameter ({@code .jpro-id-param-<key>}), plus a link that navigates to an encoded query inside the app.
 */
public class QueryPlaywrightApp extends RouteApp {

    @Override
    public Route createRoute() {
        return Route.empty()
                .and(Route.get("/query", QueryPlaywrightApp::view));
    }

    private static Response view(Request request) {
        VBox root = new VBox(8);
        Label path = new Label(request.getPath());
        path.setId("path");
        root.getChildren().add(path);

        Map<String, String> params = new TreeMap<>(request.getQueryParameters());
        Label keys = new Label(String.join(",", params.keySet()));
        keys.setId("keys");
        root.getChildren().add(keys);
        params.forEach((key, value) -> {
            Label label = new Label("[" + value + "]");
            label.setId("param-" + key);
            root.getChildren().add(label);
        });

        Label link = new Label("encoded link");
        link.setId("encoded-link");
        LinkUtil.setLink(link, "/query?next=a%26b&space=x+y");
        root.getChildren().add(link);
        return Response.node(root);
    }
}
