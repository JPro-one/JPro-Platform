package one.jpro.platform.sipjs.example;

import one.jpro.platform.routing.Response;
import one.jpro.platform.routing.Route;
import one.jpro.platform.routing.RouteApp;
import one.jpro.platform.routing.dev.DevFilter;
import one.jpro.platform.sipjs.example.page.AliceAndBobPage;
import one.jpro.platform.sipjs.example.page.AutoAliceAndBobPage;
import one.jpro.platform.sipjs.example.page.EchoPage;
import one.jpro.platform.sipjs.example.page.SelectPage;

public class SIPJSApp extends RouteApp {

    @Override
    public Route createRoute() {

        getScene().getStylesheets().add(getClass().getResource("sipjsapp.css").toString());

        return Route.empty()
                .and(Route.get("/", r -> Response.node(new SelectPage())))
                .and(Route.get("/echo", r -> Response.node(new EchoPage())))
                .and(Route.get("/auto", r -> Response.node(new AutoAliceAndBobPage())))
                .and(Route.get("/aliceAndBob", r -> Response.node(new AliceAndBobPage())))
                .filter(DevFilter.create())
                .and(Route.empty());
    }
}
