package one.jpro.utils.example;

import atlantafx.base.theme.PrimerLight;
import com.jpro.routing.Filters;
import com.jpro.routing.Route;
import com.jpro.routing.RouteApp;
import com.jpro.routing.RouteUtils;
import com.jpro.routing.extensions.linkheader.LinkHeaderFilter;
import javafx.scene.Node;
import one.jpro.example.media.MediaPlayerSample;
import one.jpro.example.media.MediaRecorderAndPlayerSample;

import java.util.ArrayList;


public class Main extends RouteApp {


    @Override
    public Route createRoute() {
        var links = new ArrayList<LinkHeaderFilter.Link>();
        links.add(new LinkHeaderFilter.Link("Video1","/video1"));
        links.add(new LinkHeaderFilter.Link("Video2","/video2"));
        links.add(new LinkHeaderFilter.Link("Scrollpane","/scrollpane"));

        getScene().setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        return Route.empty()
            .and(RouteUtils.redirect("/", "/video1"))
            .and(RouteUtils.getNode("/video1", (r) ->
                        new MediaPlayerSample().createRoot(getStage())))
            .and(RouteUtils.getNode("/video2", (r) ->
                new MediaRecorderAndPlayerSample().createRoot(getStage())))
            .and(RouteUtils.getNode("/scrollpane", (r) ->
                    new HTMLScrollPaneSample().createRoot()))
            .filter(LinkHeaderFilter.create(links))
            .filter(Filters.FullscreenFilter(true));
    }
}


