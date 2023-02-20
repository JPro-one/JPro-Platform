package one.jpro.utils.example;

import atlantafx.base.theme.PrimerLight;
import com.jpro.routing.Filters;
import com.jpro.routing.Route;
import com.jpro.routing.RouteApp;
import com.jpro.routing.extensions.linkheader.LinkHeaderFilter;
import one.jpro.example.media.MediaPlayerSample;
import one.jpro.example.media.MediaRecorderAndPlayerSample;
import one.jpro.example.media.MediaRecorderSample;

import java.util.ArrayList;

import static com.jpro.routing.RouteUtils.getNode;
import static com.jpro.routing.RouteUtils.redirect;
import static com.jpro.routing.extensions.linkheader.LinkHeaderFilter.Link;

/**
 * Launcher class to switch example applications via routing.
 *
 * @author Florian Kirmaier
 * @author Besmir Beqiri
 */
public class Main extends RouteApp {

    private final Link mediaPlayerSampleLink = new Link("MediaPlayer", "/media/player");
    private final Link mediaRecorderSampleLink = new Link("MediaRecorder", "/media/recorder");
    private final Link mediaRecorderAndPlayerSampleLink = new Link("MediaRecorderAndPlayer", "/media/recorder_player");
    private final Link scrollPaneLink = new Link("ScrollPane", "/scrollpane");

    @Override
    public Route createRoute() {
        var links = new ArrayList<LinkHeaderFilter.Link>();
        links.add(mediaPlayerSampleLink);
        links.add(mediaRecorderSampleLink);
        links.add(mediaRecorderAndPlayerSampleLink);
        links.add(scrollPaneLink);

        getScene().setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        return Route.empty()
                .and(redirect("/", mediaPlayerSampleLink.prefix()))
                .and(getNode(mediaPlayerSampleLink.prefix(), request ->
                        new MediaPlayerSample().createRoot(getStage())))
                .and(getNode(mediaRecorderSampleLink.prefix(), request ->
                        new MediaRecorderSample().createRoot(getStage())))
                .and(getNode(mediaRecorderAndPlayerSampleLink.prefix(), request ->
                        new MediaRecorderAndPlayerSample().createRoot(getStage())))
                .and(getNode(scrollPaneLink.prefix(), request ->
                        new HTMLScrollPaneSample().createRoot()))
                .filter(LinkHeaderFilter.create(links))
                .filter(Filters.FullscreenFilter(true));
    }
}


