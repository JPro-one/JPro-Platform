package example.filters;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import one.jpro.routing.LinkUtil;
import one.jpro.routing.Request;
import one.jpro.routing.filter.container.Container;

import java.util.List;

public class SimpleHamburgerMenu extends VBox implements Container {

    BooleanProperty showMenu = new SimpleBooleanProperty(false);
    Node topLinks;
    Node expandedLinks;

    HBox topBox;

    StackPane contentArea;

    Label burger = new Label("☰");

    public SimpleHamburgerMenu(List<Link> links) {
        setFillWidth(true);
        getStylesheets().add(getClass().getResource("/example/filters/SimpleHamburgerMenu.css").toString());

        widthProperty().addListener((observable, oldValue, newValue) -> {
            updateMenuElems();
        });

        topLinks = createTopLinks(links);
        expandedLinks = createExpandedLinks(links);

        topBox = new HBox();
        topBox.getStyleClass().add("top-box");

        contentArea = new StackPane();
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        showMenu.addListener((observable, oldValue, newValue) -> {
            updateContentArea();
        });
        contentProperty().addListener((observable, oldValue, newValue) -> {
            updateContentArea();
        });
        requestProperty().addListener((observable, oldValue, newValue) -> {
            showMenu.set(false);
        });

        getChildren().add(topBox);
        getChildren().add(contentArea);

        updateContentArea();

        burger.getStyleClass().add("simplehamburgermenu-burger");
        burger.setOnMouseClicked(e -> {
            toggleMenu();
        });
    }

    public void toggleMenu() {
        showMenu.set(!showMenu.get());
    }

    public void updateContentArea() {
        contentArea.getChildren().clear();
        if(getContent() != null) {
            contentArea.getChildren().add(getContent());
        }
        if(showMenu.get()) {
            burger.setText("X");
            contentArea.getChildren().add(expandedLinks);
        } else {
            burger.setText("☰");
        }
    }
    public void updateMenuElems() {
        if(getWidth() > 700) {
            showMenu.set(false);
            topBox.getChildren().clear();
            topBox.getChildren().add(topLinks);

        } else {
            topBox.getChildren().clear();
            topBox.getChildren().add(burger);
        }
    }

    public Node createTopLinks(List<Link> links) {
        HBox linkBox = new HBox();
        linkBox.getStyleClass().add("simplehamburgermenu-top-links");
        for(Link link : links) {
            Label label = new Label(link.text);
            label.getStyleClass().add("simplehamburgermenu-top-link");
            LinkUtil.setLink(label, link.link);
            requestProperty().addListener((observable, oldValue, newValue) -> {
                if(newValue != null) {
                    if(newValue.path().startsWith(link.link)) {
                        label.getStyleClass().add("selected");
                    } else {
                        label.getStyleClass().remove("selected");
                    }
                }
            });
            linkBox.getChildren().add(label);
        }
        HBox.setHgrow(linkBox, Priority.ALWAYS);
        return linkBox;
    }

    public Node createExpandedLinks(List<Link> links) {
        VBox linkBox = new VBox();
        boolean first = true;
        linkBox.getStyleClass().add("simplehamburgermenu-expanded-links");
        for(Link link : links) {
            if(!first) {
                linkBox.getChildren().add(new Separator());
            }
            first = false;

            Label label = new Label(link.text);
            label.getStyleClass().add("simplehamburgermenu-expanded-link");
            LinkUtil.setLink(label, link.link);
            requestProperty().addListener((observable, oldValue, newValue) -> {
                if(newValue != null) {
                    if(newValue.path().startsWith(link.link)) {
                        label.getStyleClass().add("selected");
                    } else {
                        label.getStyleClass().remove("selected");
                    }
                }
            });
            linkBox.getChildren().add(label);
        }
        StackPane.setAlignment(linkBox, javafx.geometry.Pos.TOP_LEFT);
        linkBox.setMaxHeight(Region.USE_PREF_SIZE);
        return linkBox;
    }


    // Link has text and link
    public static class Link {
        String text;
        String link;
        public Link(String text, String link) {
            this.text = text;
            this.link = link;
        }
    }



    /* Container implementation */
    ObjectProperty<Node> _contentProperty = new SimpleObjectProperty<>();

    @Override
    public ObjectProperty<Node> contentProperty() {
        return _contentProperty;
    }

    ObjectProperty<Request> _requestProperty = new SimpleObjectProperty<>();

    @Override
    public ObjectProperty<Request> requestProperty() {
        return _requestProperty;
    }
}

