package example.colors;

import example.filters.SimpleContainer;
import example.filters.SimpleHamburgerMenu;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import one.jpro.platform.routing.*;
import one.jpro.platform.routing.dev.DevFilter;
import one.jpro.platform.routing.dev.StatisticsFilter;
import one.jpro.platform.routing.filter.container.ContainerFilter;

import java.util.List;
import java.util.regex.Pattern;

import static one.jpro.platform.routing.Route.getNode;
import static one.jpro.platform.routing.Route.redirect;

/**
 * Creates a simple application that displays different colors using the routing functions.
 * Users can navigate between different colors via provided URL links.
 * Each color page displays the color and provides a link to another color page.
 *
 * @author Florian Kirmaier
 */
public class ColorsApp extends RouteApp {

    // A regex pattern for matching color hex codes in the URL.
    static Pattern colorPattern = Pattern.compile("/color/([0-9a-fA-F]{6})");

    public Route createRoute() {
        return Route.empty()
                .and(redirect("/", "/green"))
                .and(getNode("/green", (r) -> gen("Green", "/red", Color.GREEN)))
                .and(getNode("/red", (r) -> gen("Red", "/blue", Color.RED)))
                .and(getNode("/blue", (r) -> gen("Blue", "/yellow", Color.BLUE)))
                .and(getNode("/yellow", (r) -> gen("Yellow", r.resolve("/color/00ff00"), Color.YELLOW)))
                .and(r -> {
                    var matcher = colorPattern.matcher(r.getPath());
                    if (matcher.matches()) {
                        var colorStr = matcher.group(1);
                        var color = Color.web(colorStr);
                        return Response.node(gen("#" + colorStr, r.resolve("/red"), color));
                    } else {
                        return Response.empty();
                    }
                })
                .path("/colors",
                        Route.empty()
                                .and(getNode("/green", (r) -> gen("Green", r.resolve("/red"), Color.GREEN)))
                                .and(getNode("/red", (r) -> gen("Red", r.resolve("/green"), Color.RED))))
                .filter(Filters.FullscreenFilter(true))
                .filter(RouteUtils.sideTransitionFilter(1))
                .filter(DevFilter.create())
                .filter(StatisticsFilter.create())
                .filter(ContainerFilter.create(() -> new SimpleContainer()))
                .filter(ContainerFilter.create(() -> new SimpleHamburgerMenu(List.of(
                        new SimpleHamburgerMenu.Link("Green", "/green"),
                        new SimpleHamburgerMenu.Link("Red", "/red"),
                        new SimpleHamburgerMenu.Link("Blue", "/blue"),
                        new SimpleHamburgerMenu.Link("Yellow", "/yellow")
                ))));
    }

    /**
     * Generates a Node representing a color display page.
     *
     * @param title    the title to display on the page
     * @param nextLink the URL of the next color page to link to
     * @param color    the Color object representing the color to display
     * @return the node representing the color page
     */
    public static Node gen(String title, String nextLink, Color color) {
        StackPane result = new StackPane();
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 36px;");
        result.getChildren().add(label);
        result.setStyle("-fx-background-color: " + toHexString(color) + ";");

        StackPane linkArea = new StackPane();
        LinkUtil.setLink(linkArea, nextLink);
        result.getChildren().add(linkArea);
        return result;
    }

    /**
     * Converts a Color object to a hex string representation.
     *
     * @param value The Color object to convert
     * @return String The hex string representation of the color
     */
    public static String toHexString(Color value) {
        return "#" + (format(value.getRed()) + format(value.getGreen()) + format(value.getBlue()) + format(value.getOpacity())).toUpperCase();
    }

    /**
     * Formats a double value representing a color component into a hex string.
     *
     * @param value the double value of the color component
     * @return the formatted hex string of the color component
     */
    private static String format(double value) {
        String in = Integer.toHexString((int) Math.round(value * 255));
        if (in.length() == 1) {
            return "0" + in;
        } else {
            return in;
        }
    }
}
