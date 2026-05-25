package one.jpro.platform.mdfx;

import javafx.css.*;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import one.jpro.platform.utils.CopyUtil;
import tm4javafx.richtext.StyleHelper;
import tm4javafx.richtext.StyleProvider;
import tm4javafx.richtext.TextFlowModel;
import tm4javafx.richtext.ThemeSettings;
import tm4java.grammar.IGrammarSource;
import tm4java.parser.ContentType;
import tm4java.theme.IThemeSource;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Displays syntax-highlighted code using TextMate grammars.
 */
public class MarkdownCodeBlock extends StackPane {

    private static final String DEFAULT_STYLE_CLASS = "markdown-code-block";
    private static final String SCROLLABLE_STYLE_CLASS = "scrollable";
    private static final String COPY_BUTTON_STYLE_CLASS = "code-copy-button";
    private static final String DEFAULT_CODE_THEME = "/one/jpro/platform/mdfx/themes/github-light-default.json";

    /**
     * Mutable map from fenced-block language (lowercased) to the TextMate grammar resource path or URI.
     * Mutate to add or override languages, e.g.:
     * {@code languageToGrammar().put("nim", "/com/example/grammars/nim.tmLanguage.json")}.
     */
    public static Map<String, String> languageToGrammar() {
        return LANGUAGE_TO_GRAMMAR;
    }

    private static final Map<String, String> LANGUAGE_TO_GRAMMAR = new HashMap<>();

    private static final String BUNDLED_GRAMMARS = "/one/jpro/platform/mdfx/grammars/";
    static {
        LANGUAGE_TO_GRAMMAR.put("java",       BUNDLED_GRAMMARS + "java.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("javascript", BUNDLED_GRAMMARS + "javascript.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("js",         BUNDLED_GRAMMARS + "javascript.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("python",     BUNDLED_GRAMMARS + "python.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("py",         BUNDLED_GRAMMARS + "python.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("css",        BUNDLED_GRAMMARS + "css.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("html",       BUNDLED_GRAMMARS + "html.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("xml",        BUNDLED_GRAMMARS + "xml.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("json",       BUNDLED_GRAMMARS + "json.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("yaml",       BUNDLED_GRAMMARS + "yaml.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("yml",        BUNDLED_GRAMMARS + "yaml.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("typescript", BUNDLED_GRAMMARS + "typescript.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("ts",         BUNDLED_GRAMMARS + "typescript.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("c",          BUNDLED_GRAMMARS + "c.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("cpp",        BUNDLED_GRAMMARS + "cpp.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("c++",        BUNDLED_GRAMMARS + "cpp.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("go",         BUNDLED_GRAMMARS + "go.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("golang",     BUNDLED_GRAMMARS + "go.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("rust",       BUNDLED_GRAMMARS + "rust.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("rs",         BUNDLED_GRAMMARS + "rust.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("ruby",       BUNDLED_GRAMMARS + "ruby.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("rb",         BUNDLED_GRAMMARS + "ruby.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("swift",      BUNDLED_GRAMMARS + "swift.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("sql",        BUNDLED_GRAMMARS + "sql.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("bash",       BUNDLED_GRAMMARS + "shellscript.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("sh",         BUNDLED_GRAMMARS + "shellscript.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("shell",      BUNDLED_GRAMMARS + "shellscript.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("groovy",     BUNDLED_GRAMMARS + "groovy.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("dart",       BUNDLED_GRAMMARS + "dart.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("dockerfile", BUNDLED_GRAMMARS + "dockerfile.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("docker",     BUNDLED_GRAMMARS + "dockerfile.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("markdown",   BUNDLED_GRAMMARS + "markdown.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("md",         BUNDLED_GRAMMARS + "markdown.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("php",        BUNDLED_GRAMMARS + "php.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("kotlin",     BUNDLED_GRAMMARS + "java.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("kt",         BUNDLED_GRAMMARS + "java.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("http",       BUNDLED_GRAMMARS + "http.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("rest",       BUNDLED_GRAMMARS + "http.tmLanguage.json");
    }

    // CSS-styleable property
    private static final CssMetaData<MarkdownCodeBlock, String> CODE_THEME =
            new CssMetaData<>("-mdfx-code-theme", StyleConverter.getStringConverter(), DEFAULT_CODE_THEME) {
                @Override
                public boolean isSettable(MarkdownCodeBlock node) {
                    return !node.codeTheme.isBound();
                }

                @Override
                public StyleableProperty<String> getStyleableProperty(MarkdownCodeBlock node) {
                    return node.codeTheme;
                }
            };

    private static final CssMetaData<MarkdownCodeBlock, Boolean> SCROLLABLE =
            new CssMetaData<>("-mdfx-scrollable", StyleConverter.getBooleanConverter(), false) {
                @Override
                public boolean isSettable(MarkdownCodeBlock node) {
                    return !node.scrollable.isBound();
                }

                @Override
                public StyleableProperty<Boolean> getStyleableProperty(MarkdownCodeBlock node) {
                    return node.scrollable;
                }
            };

    private static final CssMetaData<MarkdownCodeBlock, Boolean> ADD_COPY_BUTTON =
            new CssMetaData<>("-mdfx-add-copy-button", StyleConverter.getBooleanConverter(), false) {
                @Override
                public boolean isSettable(MarkdownCodeBlock node) {
                    return !node.addCopyButton.isBound();
                }

                @Override
                public StyleableProperty<Boolean> getStyleableProperty(MarkdownCodeBlock node) {
                    return node.addCopyButton;
                }
            };

    private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
    static {
        List<CssMetaData<? extends Styleable, ?>> list = new ArrayList<>(StackPane.getClassCssMetaData());
        list.add(CODE_THEME);
        list.add(SCROLLABLE);
        list.add(ADD_COPY_BUTTON);
        STYLEABLES = Collections.unmodifiableList(list);
    }

    private final StyleableStringProperty codeTheme = new SimpleStyleableStringProperty(CODE_THEME, this, "codeTheme", DEFAULT_CODE_THEME);
    private final StyleableBooleanProperty scrollable = new SimpleStyleableBooleanProperty(SCROLLABLE, this, "scrollable", false);
    private final StyleableBooleanProperty addCopyButton = new SimpleStyleableBooleanProperty(ADD_COPY_BUTTON, this, "addCopyButton", false);

    private final TextFlow textFlow;
    private final ScrollPane scrollPane;
    private Button copyButton;
    private final String code;
    private final String language;
    private TextFlowModel textFlowModel;

    public MarkdownCodeBlock(String code, String language) {
        this.code = code;
        this.language = language;

        getStyleClass().add(DEFAULT_STYLE_CLASS);

        textFlow = new TextFlow();
        textFlow.getStyleClass().add("code-text-flow");

        scrollPane = new ScrollPane();
        scrollPane.getStyleClass().add("code-scroll-pane");
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(false);
        scrollPane.setFocusTraversable(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.viewportBoundsProperty().subscribe(bounds -> updateTextFlowMinWidth());

        updateScrollableState();

        scrollable.subscribe(v -> updateScrollableState());
        addCopyButton.subscribe(v -> updateScrollableState());
        codeTheme.subscribe(v -> updateContent());
    }

    public final boolean isScrollable() {
        return scrollable.get();
    }

    public final void setScrollable(boolean scrollable) {
        this.scrollable.set(scrollable);
    }

    public final BooleanProperty scrollableProperty() {
        return scrollable;
    }

    public final boolean isAddCopyButton() {
        return addCopyButton.get();
    }

    public final void setAddCopyButton(boolean addCopyButton) {
        this.addCopyButton.set(addCopyButton);
    }

    public final BooleanProperty addCopyButtonProperty() {
        return addCopyButton;
    }

    private void updateScrollableState() {
        getStyleClass().remove(SCROLLABLE_STYLE_CLASS);
        getChildren().clear();

        if (isScrollable()) {
            getStyleClass().add(SCROLLABLE_STYLE_CLASS);
            scrollPane.setContent(textFlow);
            getChildren().add(scrollPane);
        } else {
            scrollPane.setContent(null);
            getChildren().add(textFlow);
        }

        if (isAddCopyButton()) {
            getChildren().add(getOrCreateCopyButton());
        }

        updateTextFlowMinWidth();
    }

    private Button getOrCreateCopyButton() {
        if (copyButton == null) {
            copyButton = new Button("Copy");
            copyButton.getStyleClass().add(COPY_BUTTON_STYLE_CLASS);
            copyButton.setFocusTraversable(false);
            CopyUtil.setCopyOnClick(copyButton, code == null ? "" : code);
            StackPane.setAlignment(copyButton, Pos.TOP_RIGHT);
            StackPane.setMargin(copyButton, new Insets(8));
        }
        return copyButton;
    }

    private void updateTextFlowMinWidth() {
        if (isScrollable()) {
            textFlow.setMinWidth(scrollPane.getViewportBounds().getWidth());
            textFlow.setMinHeight(scrollPane.getViewportBounds().getHeight());
        } else {
            textFlow.setMinWidth(Region.USE_COMPUTED_SIZE);
            textFlow.setMinHeight(Region.USE_COMPUTED_SIZE);
        }
    }

    private void updateContent() {
        if (code == null || code.isEmpty()) {
            textFlow.getChildren().clear();
            return;
        }

        String grammarResource = language != null && !language.isEmpty()
                ? LANGUAGE_TO_GRAMMAR.get(language.toLowerCase()) : null;

        if (grammarResource != null) {
            highlightWithTextMate(code, grammarResource);
        } else {
            showPlainText(code);
        }
    }

    private void highlightWithTextMate(String code, String grammarResource) {
        try {
            StyleProvider styleProvider = new StyleProvider();
            styleProvider.setGrammar(loadGrammar(grammarResource));
            styleProvider.setTheme(loadTheme(codeTheme.get()));

            textFlowModel = new TextFlowModel();
            textFlowModel.setTextFlow(textFlow);
            textFlowModel.setStyleProvider(styleProvider);
            textFlowModel.setText(code);

            // Workaround, until https://github.com/mkpaz/tm4javafx/pull/4 is merged.
            trimTrailingNewline(textFlow);

            ThemeSettings settings = styleProvider.getThemeSettings();
            StyleHelper.applyThemeSettings(textFlow, settings);
            if (settings != null) {
                StyleHelper.addOrReplaceStyle(scrollPane, "-fx-background", settings.getBackgroundColor());
                StyleHelper.addOrReplaceStyle(scrollPane, "-fx-background-color", settings.getBackgroundColor());
            }
        } catch (Exception e) {
            showPlainText(code);
        }
    }

    private static IGrammarSource loadGrammar(String path) throws IOException {
        URL url = findResource(path);
        if (url == null) throw new IOException("Grammar resource not found on classpath: " + path);
        return IGrammarSource.fromString(ContentType.getByExtension(path), readAll(url));
    }

    private static IThemeSource loadTheme(String path) throws IOException {
        URL url = findResource(path);
        if (url == null) throw new IOException("Theme resource not found on classpath: " + path);
        return IThemeSource.fromString(ContentType.getByExtension(path), readAll(url));
    }

    /** Accepts an absolute classpath path ({@code "/foo/bar.json"}) or an absolute URI ({@code "file:…"}, {@code "https:…"}). */
    private static URL findResource(String pathOrUri) throws IOException {
        URI uri = null;
        try { uri = URI.create(pathOrUri); } catch (IllegalArgumentException ignore) {}
        if (uri != null && uri.isAbsolute()) return uri.toURL();
        URL u = MarkdownCodeBlock.class.getResource(pathOrUri);
        if (u != null) return u;
        return Thread.currentThread().getContextClassLoader().getResource(pathOrUri.substring(1));
    }

    private static String readAll(URL url) throws IOException {
        try (var in = url.openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void showPlainText(String code) {
        textFlow.getChildren().clear();
        if (code.endsWith("\n")) {
            code = code.substring(0, code.length() - 1);
        }
        Text text = new Text(code);
        textFlow.getChildren().add(text);
    }

    private void trimTrailingNewline(TextFlow textFlow) {
        if (textFlow.getChildren().isEmpty()) return;
        var last = textFlow.getChildren().get(textFlow.getChildren().size() - 1);
        if (last instanceof Text textNode) {
            String t = textNode.getText();
            if (t.endsWith("\n")) {
                textNode.setText(t.substring(0, t.length() - 1));
            }
        }
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return STYLEABLES;
    }
}
