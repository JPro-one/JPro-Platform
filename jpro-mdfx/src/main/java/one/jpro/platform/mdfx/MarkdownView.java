package one.jpro.platform.mdfx;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import one.jpro.platform.mdfx.extensions.ImageExtension;
import one.jpro.platform.mdfx.impl.AdaptiveImage;
import one.jpro.platform.mdfx.impl.MDFXNodeHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MarkdownView extends VBox {

    private List<ImageExtension> extensions = new ArrayList<>();

    private final SimpleStringProperty mdString = new SimpleStringProperty("");

    public MarkdownView(String mdString) {
        this(mdString, defaultExtensions());
    }

    public MarkdownView(String mdString, List<ImageExtension> extensions) {
        // Check whether only one extension has the scheme null
        if(extensions.stream().filter(e -> e.getScheme() == null).count() > 1) {
            throw new IllegalArgumentException("Only one extension can have a scheme of null");
        }

        this.extensions = extensions;
        this.mdString.set(mdString);
        this.mdString.addListener((p,o,n) -> updateContent());
        Optional.ofNullable(MarkdownView.class.getResource("/one/jpro/platform/mdfx/mdfx.css"))
                .ifPresent(cssResource -> getStylesheets().add(cssResource.toExternalForm()));
        getDefaultStylesheets().forEach(getStylesheets()::add);
        updateContent();
    }

    public MarkdownView() {
        this("");
    }

    protected List<String> getDefaultStylesheets() {
        final var defaultStylesheets = new ArrayList<String>();
        Optional.ofNullable(MarkdownView.class.getResource("/one/jpro/platform/mdfx/mdfx-default.css"))
                .ifPresent(cssResource -> defaultStylesheets.add(cssResource.toExternalForm()));
        return defaultStylesheets;
    }

    private void updateContent() {
        MDFXNodeHelper content = new MDFXNodeHelper(this, mdString.getValue());
        getChildren().clear();
        getChildren().add(content);
    }

    public StringProperty mdStringProperty() {
        return mdString;
    }

    public void setMdString(String mdString) {
        this.mdString.set(mdString);
    }

    public String getMdString() {
        return mdString.get();
    }

    public boolean showChapter(int[] currentChapter) {
            return true;
    }

    public void setLink(Node node, String link, String description) {
        // TODO
        //com.jpro.web.Util.setLink(node, link, scala.Option.apply(description));
    }

    public Node generateImage(String url) {
        // Let's find an extension with a matching scheme
        // But be aware, that the url is sometimes relative without a scheme

        var res = extensions.stream()
                .filter(e -> e.getScheme() != null && url.startsWith(e.getScheme()))
                .findFirst();

        if(res.isEmpty()) {
            res = extensions.stream()
                    .filter(e -> e.getScheme() == null)
                    .findFirst();
        }

        return res.get().getFunction().apply(url, this);
    }


    public static List<ImageExtension> defaultExtensions() {
        return new ArrayList<>(List.of(DEFAULT_IMAGE_EXTENSION));
    }

    static ImageExtension DEFAULT_IMAGE_EXTENSION = new ImageExtension(null, (url, view) -> {
        if(url.isEmpty()) {
            return new Group();
        } else {
            Image img = new Image(url, false);
            AdaptiveImage r = new AdaptiveImage(img);

            // The TextFlow does not limit the width of its node based on the available width
            // As a workaround, we bind to the width of the MarkDownView.
            r.maxWidthProperty().bind(view.widthProperty());

            return r;
        }
    });
}
