package one.jpro.platform.mdfx.example;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import one.jpro.platform.css.DynamicCSSUtil;
import one.jpro.platform.mdfx.MarkdownView;
import one.jpro.platform.mdfx.extensions.YoutubeExtension;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * MarkdownView example.
 *
 * @author Besmir Beqiri
 */
public class MarkdownViewSample extends Application {

    private static final Logger logger = LoggerFactory.getLogger(MarkdownViewSample.class);
    private static final String SAMPLE_MD = "/one/jpro/platform/mdfx/example/sample.md";

    private static final String STYLE_NONE = "None";
    private static final String STYLE_DARK = "Dark";
    private static final String STYLE_SEPIA = "Sepia";
    private static final String STYLE_BLUE = "Blue";

    private static final String CUSTOM_CSS_TEMPLATE = """
            /* Override MDFX default variables */
            /* Uncomment and modify the values below */

            /*
            * {
                -mdfx-font-color: black;
                -mdfx-link-color: blue;
                -mdfx-border-color-1: #888;
                -mdfx-bg-color-1: #ccc;
                -mdfx-bg-color-2: #ddd;
                -mdfx-bg-color-3: #eee;
                -mdfx-bq-color-border: #4488cc;
                -mdfx-bq-color-background: #0000ff0c;
            }

            .markdown-text {
                -fx-font-family: ARIAL;
                -fx-font-size: 16;
            }

            .markdown-code-block {
                -mdfx-code-theme: "/one/jpro/platform/mdfx/themes/github-light-default.json";
            }
            */
            """;

    private static final String DARK_CSS = """
            * {
                -mdfx-font-color: #d4d4d4;
                -mdfx-link-color: #6cb6ff;
                -mdfx-border-color-1: #444;
                -mdfx-bg-color-1: #2a2a2a;
                -mdfx-bg-color-2: #333;
                -mdfx-bg-color-3: #3a3a3a;
                -mdfx-bq-color-border: #6cb6ff;
                -mdfx-bq-color-background: #6cb6ff18;
            }
            .markdown-view {
                -fx-background-color: #1e1e1e;
            }
            .markdown-code-block {
                -mdfx-code-theme: "/one/jpro/platform/mdfx/themes/github-dark-default.json";
            }
            """;

    private static final String SEPIA_CSS = """
            * {
                -mdfx-font-color: #5b4636;
                -mdfx-link-color: #8b4513;
                -mdfx-border-color-1: #c4a882;
                -mdfx-bg-color-1: #e8d5b7;
                -mdfx-bg-color-2: #efe0c9;
                -mdfx-bg-color-3: #f5eadb;
                -mdfx-bq-color-border: #8b4513;
                -mdfx-bq-color-background: #8b451318;
            }
            .markdown-view {
                -fx-background-color: #f4ecd8;
            }
            .markdown-text {
                -fx-font-family: "Georgia";
            }
            """;

    private static final String BLUE_CSS = """
            * {
                -mdfx-font-color: #1a2a3a;
                -mdfx-link-color: #0066cc;
                -mdfx-border-color-1: #7ba7cc;
                -mdfx-bg-color-1: #c8ddf0;
                -mdfx-bg-color-2: #d8e8f5;
                -mdfx-bg-color-3: #e8f0fa;
                -mdfx-bq-color-border: #0066cc;
                -mdfx-bq-color-background: #0066cc12;
            }
            .markdown-view {
                -fx-background-color: #f0f6fc;
            }
            """;

    @Override
    public void start(Stage stage) {
        stage.setTitle("JPro Markdown View");
        Scene scene = new Scene(createRoot(stage), 1200, 700);
        stage.setScene(scene);
        stage.show();
    }

    public Parent createRoot(Stage stage) {
        // Markdown source editor
        TextArea mdTextArea = new TextArea();
        mdTextArea.setMinWidth(300);
        Optional.ofNullable(getClass().getResource(SAMPLE_MD))
                .ifPresent(mdResource -> {
                    try {
                        mdTextArea.setText(IOUtils.toString(mdResource, StandardCharsets.UTF_8));
                    } catch (IOException ex) {
                        logger.error("Error reading markdown sample", ex);
                    }
                });

        // Markdown view
        var markdownExtensions = MarkdownView.defaultExtensions();
        markdownExtensions.add(YoutubeExtension.create());

        final var markdownView = new MarkdownView("", markdownExtensions) {
            @Override
            public void setLink(Node node, String link, String description) {
                node.setCursor(Cursor.HAND);
                node.setOnMouseClicked(e -> logger.info("link: " + link));
            }

            @Override
            public Node generateImage(String url) {
                if (url.equals("node://colorpicker")) {
                    return new ColorPicker();
                } else {
                    return super.generateImage(url);
                }
            }
        };
        markdownView.getStyleClass().add("markdown-view");
        markdownView.mdStringProperty().bind(mdTextArea.textProperty());

        final var scrollPane = new ScrollPane(markdownView);
        scrollPane.setFitToWidth(true);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        // CSS style selector
        ComboBox<String> styleSelector = new ComboBox<>(
                FXCollections.observableArrayList(STYLE_NONE, STYLE_DARK, STYLE_SEPIA, STYLE_BLUE));
        styleSelector.setValue(STYLE_NONE);

        // Custom CSS editor
        TextArea cssTextArea = new TextArea(CUSTOM_CSS_TEMPLATE);
        cssTextArea.setStyle("-fx-font-family: monospace; -fx-font-size: 13;");
        VBox.setVgrow(cssTextArea, Priority.ALWAYS);

        // Apply CSS when style selection changes
        styleSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            String css = switch (newVal) {
                case STYLE_DARK -> DARK_CSS;
                case STYLE_SEPIA -> SEPIA_CSS;
                case STYLE_BLUE -> BLUE_CSS;
                default -> "";
            };
            cssTextArea.setText(css.isEmpty() ? CUSTOM_CSS_TEMPLATE : css);
        });

        // Apply CSS dynamically when textarea changes
        cssTextArea.textProperty().addListener((obs, oldVal, newVal) -> {
            DynamicCSSUtil.setCssString(markdownView, newVal);
        });

        Label styleLabel = new Label("Style:");
        HBox styleBar = new HBox(8, styleLabel, styleSelector);
        styleBar.setStyle("-fx-alignment: center-left; -fx-padding: 4;");

        VBox cssPanel = new VBox(4, styleBar, cssTextArea);
        cssPanel.setMinWidth(300);

        return new HBox(mdTextArea, scrollPane, cssPanel);
    }
}
