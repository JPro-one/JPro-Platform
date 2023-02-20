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

/**
 * Launcher class to switch example applications via routing.
 *
 * @author Florian Kirmaier
 * @author Besmir Beqiri
 */
public class Main extends RouteApp {

    private static final String MEDIA_PLAYER_SAMPLE_PATH = "/media/player";
    private static final String MEDIA_RECORDER_SAMPLE_PATH = "/media/recorder";
    private static final String MEDIA_RECORDER_AND_PLAYER_SAMPLE_PATH = "/media/recorder_player";
    private static final String SCROLLPANE_PATH = "/scrollpane";

    @Override
    public Route createRoute() {
        var links = new ArrayList<LinkHeaderFilter.Link>();
        links.add(new LinkHeaderFilter.Link("MediaPlayer", MEDIA_PLAYER_SAMPLE_PATH));
        links.add(new LinkHeaderFilter.Link("MediaRecorder", MEDIA_RECORDER_SAMPLE_PATH));
        links.add(new LinkHeaderFilter.Link("MediaRecorderAndPlayer", MEDIA_RECORDER_AND_PLAYER_SAMPLE_PATH));
        links.add(new LinkHeaderFilter.Link("Scrollpane", SCROLLPANE_PATH));

        getScene().setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        return Route.empty()
                .and(redirect("/", MEDIA_PLAYER_SAMPLE_PATH))
                .and(getNode(MEDIA_PLAYER_SAMPLE_PATH, (r) ->
                        new MediaPlayerSample().createRoot(getStage())))
                .and(getNode(MEDIA_RECORDER_SAMPLE_PATH, (r) ->
                        new MediaRecorderSample().createRoot(getStage())))
                .and(getNode(MEDIA_RECORDER_AND_PLAYER_SAMPLE_PATH, (r) ->
                        new MediaRecorderAndPlayerSample().createRoot(getStage())))
                .and(getNode(SCROLLPANE_PATH, (r) ->
                        new HTMLScrollPaneSample().createRoot()))
                .filter(LinkHeaderFilter.create(links))
                .filter(Filters.FullscreenFilter(true));
    }
}


