package one.jpro.platform.youtube;

import com.jpro.webapi.HTMLView;
import javafx.geometry.Orientation;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import com.jpro.webapi.WebAPI;
import javafx.scene.web.WebView;

/**
 * A Node that displays a Youtube Video.
 */
public class YoutubeNode extends StackPane {

    protected int PREF_HEIGHT = 340;
    protected int PREF_WIDTH = 560;

    protected double ratio = 16.0/9.0;

    /**
     * Creates a new YoutubeNode with the given videoId.
     * @param videoId the id of the video to display
     */
    public YoutubeNode(String videoId) {

        setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        //setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        if(WebAPI.isBrowser()) {
            var htmlView = new HTMLView();
            htmlView.setContent("<iframe src=\"https://www.youtube.com/embed/"+videoId+"\" frameborder=\"0\" allow=\"accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture\" allowfullscreen></iframe>");
            getChildren().add(htmlView);
            WebAPI.getWebAPI(this, webAPI -> {
                var elem = webAPI.getHTMLViewElement(htmlView);
                widthProperty().addListener((observable, oldValue, newValue) -> {
                    webAPI.executeScript(elem.getName()+".firstElementChild.width = "+newValue.intValue()+";");
                });
                heightProperty().addListener((observable, oldValue, newValue) -> {
                    webAPI.executeScript(elem.getName()+".firstElementChild.height = "+newValue.intValue()+";");
                });
                webAPI.executeScript(elem.getName()+".firstElementChild.width = "+getWidth()+";");
                webAPI.executeScript(elem.getName()+".firstElementChild.height = "+getHeight()+";");
            });
        } else {
            var webView = new WebView();
            webView.getEngine().load("https://www.youtube.com/embed/"+videoId);
            getChildren().add(webView);
        }
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override
    protected double computePrefWidth(double height) {
        if(height <= -1) {
            return PREF_WIDTH;
        }
        return height * ratio;
    }

    @Override
    protected double computePrefHeight(double width) {
        if(width <= -1) {
            return PREF_HEIGHT;
        }
        return width / ratio;
    }
}
