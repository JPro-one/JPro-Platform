package one.jpro.platform.example;

import atlantafx.base.theme.CupertinoLight;
import one.jpro.platform.htmlscrollpane.example.HTMLScrollPaneSample;
import one.jpro.platform.mdfx.example.MarkdownViewSample;
import one.jpro.platform.media.example.MediaPlayerSample;
import one.jpro.platform.media.example.MediaRecorderAndPlayerSample;
import one.jpro.platform.media.example.MediaRecorderSample;
import one.jpro.platform.routing.Filters;
import one.jpro.platform.routing.Route;
import one.jpro.platform.routing.Response;
import one.jpro.platform.routing.RouteApp;
import one.jpro.platform.routing.extensions.linkheader.LinkHeaderFilter;
import one.jpro.platform.sessions.example.SessionManagerSample;

import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;

import static one.jpro.platform.routing.Route.get;
import static one.jpro.platform.routing.Route.redirect;
import static one.jpro.platform.routing.extensions.linkheader.LinkHeaderFilter.Link;

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
    private final Link markdownViewSampleLink = new Link("MarkdownView", "/mdfx");
    private final Link scrollPaneLink = new Link("ScrollPane", "/scrollpane");
    private final Link sessionManagerLink = new Link("SessionManager", "/sessionmanager");

    @Override
    public Route createRoute() {
        var links = new ArrayList<Link>();
        links.add(mediaPlayerSampleLink);
        links.add(mediaRecorderSampleLink);
        links.add(mediaRecorderAndPlayerSampleLink);
        links.add(markdownViewSampleLink);
        links.add(scrollPaneLink);
        links.add(sessionManagerLink);

        getScene().setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
        Optional.ofNullable(getClass().getResource("/one/jpro/platform/example/media/css/main.css"))
                .map(URL::toExternalForm)
                .ifPresent(cssResource -> getScene().getStylesheets().add(cssResource));

        return Route.empty()
                .and(redirect("/", mediaPlayerSampleLink.prefix()))
                .and(get(mediaPlayerSampleLink.prefix(), request ->
                        Response.node(new MediaPlayerSample().createRoot(getStage()))))
                .and(get(mediaRecorderSampleLink.prefix(), request ->
                        Response.node(new MediaRecorderSample().createRoot(getStage()))))
                .and(get(mediaRecorderAndPlayerSampleLink.prefix(), request ->
                        Response.node(new MediaRecorderAndPlayerSample().createRoot(getStage()))))
                .and(get(markdownViewSampleLink.prefix(), request ->
                        Response.node(new MarkdownViewSample().createRoot(getStage()))))
                .and(get(scrollPaneLink.prefix(), request ->
                        Response.node(new HTMLScrollPaneSample().createRoot())))
                .and(get(sessionManagerLink.prefix(), request ->
                        Response.node(new SessionManagerSample().createRoot(getStage()))))
                .filter(LinkHeaderFilter.create(links))
                .filter(Filters.FullscreenFilter(true));
    }
}
