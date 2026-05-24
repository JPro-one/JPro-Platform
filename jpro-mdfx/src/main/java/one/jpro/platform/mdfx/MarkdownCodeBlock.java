package one.jpro.platform.mdfx;

import javafx.css.*;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import tm4javafx.richtext.StyleHelper;
import tm4javafx.richtext.StyleProvider;
import tm4javafx.richtext.TextFlowModel;
import tm4javafx.richtext.ThemeSettings;
import tm4java.grammar.IGrammarSource;
import tm4java.theme.IThemeSource;

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
    private static final String DEFAULT_CODE_THEME = "/one/jpro/platform/mdfx/themes/github-light-default.json";

    private static final Map<String, String> LANGUAGE_TO_GRAMMAR = new HashMap<>();

    static {
        LANGUAGE_TO_GRAMMAR.put("java", "grammars/java.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("javascript", "grammars/javascript.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("js", "grammars/javascript.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("python", "grammars/python.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("py", "grammars/python.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("css", "grammars/css.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("html", "grammars/html.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("xml", "grammars/xml.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("json", "grammars/json.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("yaml", "grammars/yaml.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("yml", "grammars/yaml.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("typescript", "grammars/typescript.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("ts", "grammars/typescript.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("c", "grammars/c.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("cpp", "grammars/cpp.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("c++", "grammars/cpp.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("go", "grammars/go.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("golang", "grammars/go.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("rust", "grammars/rust.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("rs", "grammars/rust.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("ruby", "grammars/ruby.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("rb", "grammars/ruby.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("swift", "grammars/swift.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("sql", "grammars/sql.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("bash", "grammars/shellscript.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("sh", "grammars/shellscript.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("shell", "grammars/shellscript.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("groovy", "grammars/groovy.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("dart", "grammars/dart.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("dockerfile", "grammars/dockerfile.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("docker", "grammars/dockerfile.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("markdown", "grammars/markdown.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("md", "grammars/markdown.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("php", "grammars/php.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("kotlin", "grammars/java.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("kt", "grammars/java.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("http", "grammars/http.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("rest", "grammars/http.tmLanguage.json");
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

    private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
    static {
        List<CssMetaData<? extends Styleable, ?>> list = new ArrayList<>(StackPane.getClassCssMetaData());
        list.add(CODE_THEME);
        STYLEABLES = Collections.unmodifiableList(list);
    }

    private final StyleableStringProperty codeTheme = new SimpleStyleableStringProperty(CODE_THEME, this, "codeTheme", DEFAULT_CODE_THEME);
    private final TextFlow textFlow;
    private final String code;
    private final String language;
    private TextFlowModel textFlowModel;

    public MarkdownCodeBlock(String code, String language) {
        this.code = code;
        this.language = language;

        getStyleClass().add(DEFAULT_STYLE_CLASS);

        textFlow = new TextFlow();
        textFlow.getStyleClass().add("code-text-flow");
        getChildren().add(textFlow);

        codeTheme.subscribe(v -> updateContent());
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
            styleProvider.setGrammar(
                    IGrammarSource.fromResource(MarkdownCodeBlock.class, grammarResource)
            );
            styleProvider.setTheme(
                    IThemeSource.fromResource(MarkdownCodeBlock.class, codeTheme.get())
            );

            textFlowModel = new TextFlowModel();
            textFlowModel.setTextFlow(textFlow);
            textFlowModel.setStyleProvider(styleProvider);
            textFlowModel.setText(code);

            // Workaround, until https://github.com/mkpaz/tm4javafx/pull/4 is merged.
            trimTrailingNewline(textFlow);

            ThemeSettings settings = styleProvider.getThemeSettings();
            StyleHelper.applyThemeSettings(textFlow, settings);
        } catch (Exception e) {
            showPlainText(code);
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
