package one.jpro.platform.sipjs.example;

import one.jpro.platform.routing.Response;
import one.jpro.platform.routing.Route;
import one.jpro.platform.routing.RouteApp;
import one.jpro.platform.routing.dev.DevFilter;
import one.jpro.platform.sipjs.example.page.*;

public class SIPJSApp extends RouteApp {

    @Override
    public Route createRoute() {

        getScene().getStylesheets().add(getClass().getResource("sipjsapp.css").toString());

        return Route.empty()
                .and(Route.get("/", r -> Response.node(new SelectPage())))
                .and(Route.get("/echo", r -> Response.node(new EchoPage())))
                .and(Route.get("/auto", r -> Response.node(new AutoAliceAndBobPage())))
                .and(Route.get("/aliceAndBob", r -> Response.node(new AliceAndBobPage())))
                .and(Route.get("/alice", r -> Response.node(new AlicePage())))
                .and(Route.get("/bob", r -> Response.node(new BobPage())))
                .filter(DevFilter.create())
                .and(Route.empty());
    }
}
