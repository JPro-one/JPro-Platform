package one.jpro.platform.mdfx.example;

import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import one.jpro.platform.mdfx.MarkdownView;
import one.jpro.platform.mdfx.extensions.YoutubeExtension;
import org.apache.commons.io.IOUtils;
import org.scenicview.ScenicView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * MarkdownView example.
 *
 * @author Besmir Beqiri
 */
public class MarkdownViewSample extends Application {

    private static final Logger logger = LoggerFactory.getLogger(MarkdownViewSample.class);
    private static final String SAMPLE_CSS = "/one/jpro/platform/mdfx/example/mdfx-sample.css";
    private static final String SAMPLE_MD = "/one/jpro/platform/mdfx/example/sample.md";

    @Override
    public void start(Stage stage) {
        stage.setTitle("JPro Markdown View");
        Scene scene = new Scene(createRoot(stage), 1000,600);
        stage.setScene(scene);
        stage.show();

        // Start ScenicView
        // ScenicView.show(scene);
    }

    public Parent createRoot(Stage stage) {
        TextArea textArea = new TextArea();
        textArea.setMinWidth(350);

        Optional.ofNullable(getClass().getResource(SAMPLE_MD))
                .ifPresent(mdResource -> {
                    logger.info("Markdown sample: " + mdResource);

                    try {
                        String mdfxString = IOUtils.toString(mdResource, StandardCharsets.UTF_8);
                        textArea.setText(mdfxString);
                    } catch (IOException ex) {
                        logger.error("Error reading markdown sample", ex);
                    }
                });

        var markdownExtensions = MarkdownView.defaultExtensions();
        markdownExtensions.add(YoutubeExtension.create());

        final var markdownView = new MarkdownView("", markdownExtensions) {
            @Override
            protected List<String> getDefaultStylesheets() {
                Optional<String> defaultStylesheet = Optional.ofNullable(getClass().getResource(SAMPLE_CSS))
                        .map(URL::toExternalForm);
                return defaultStylesheet.map(List::of).orElseGet(List::of);
            }

            @Override
            public void setLink(Node node, String link, String description) {
                logger.info("setLink: " + link);
                node.setCursor(Cursor.HAND);
                node.setOnMouseClicked(e -> logger.info("link: " + link));
            }

            @Override
            public Node generateImage(String url) {
                if(url.equals("node://colorpicker")) {
                    return new ColorPicker();
                } else {
                    return super.generateImage(url);
                }
            }
        };
        markdownView.mdStringProperty().bind(textArea.textProperty());

        final var scrollPane = new ScrollPane(markdownView);
        scrollPane.setFitToWidth(true);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        return new HBox(textArea, scrollPane);
    }
}
