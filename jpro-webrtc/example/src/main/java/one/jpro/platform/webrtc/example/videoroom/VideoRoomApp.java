package one.jpro.platform.webrtc.example.videoroom;

import one.jpro.platform.routing.*;
import one.jpro.platform.webrtc.example.videoroom.page.OverviewPage;
import one.jpro.platform.webrtc.example.videoroom.page.VideoRoomPage;
import simplefx.experimental.parts.FXFuture;

import java.util.regex.Pattern;

import static one.jpro.platform.routing.RouteUtils.viewFromNode;

public class VideoRoomApp extends RouteApp {

    static Pattern roomPattern = Pattern.compile("/room/([0-9a-fA-F]*)");

    @Override
    public Route createRoute() {

        getScene().getStylesheets().add("/one/jpro/platform/webrtc/example/videoroom/videoroom.css");

        // / -> overview
        // /room/id -> room
        return Route.empty()
                .and(RouteUtils.getNode("/", (r) -> new OverviewPage()))
                .and(r -> {
                    System.out.println("path: " + r.path());
                    var matcher = roomPattern.matcher(r.path());
                    if(matcher.matches()) {
                        var roomID = matcher.group(1);
                        return Response.node(new VideoRoomPage(roomID, getWebAPI()));
                    } else {
                        return Response.empty();
                    }
                })
                .filter(Filters.FullscreenFilter(true));
    }
}
