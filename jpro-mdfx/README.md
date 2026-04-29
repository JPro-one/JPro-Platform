# JPro MDFX

A Markdown renderer for JavaFX with syntax highlighting, based on [flexmark-java](https://github.com/vsch/flexmark-java) and [tm4javafx](https://github.com/mkpaz/tm4javafx).

## Quick Start

```java
import one.jpro.platform.mdfx.MarkdownView;

MarkdownView markdownView = new MarkdownView("# Hello\nSome **bold** text.");
```

The `mdString` property is bindable:

```java
TextArea editor = new TextArea();
MarkdownView view = new MarkdownView();
view.mdStringProperty().bind(editor.textProperty());
```

Module dependency: `requires one.jpro.platform.mdfx;`

## Custom Link and Image Handling

Override `setLink` and `generateImage` for custom behavior:

```java
MarkdownView view = new MarkdownView("content") {
    @Override
    public void setLink(Node node, String link, String description) {
        node.setCursor(Cursor.HAND);
        node.setOnMouseClicked(e -> getHostServices().showDocument(link));
    }

    @Override
    public Node generateImage(String url) {
        if (url.startsWith("custom://")) return new Label(url);
        return super.generateImage(url);
    }
};
```

Image extensions allow scheme-based handling:

```java
var extensions = MarkdownView.defaultExtensions();
extensions.add(YoutubeExtension.create()); // handles youtube:// URLs
extensions.add(new ImageExtension("myapp://", (url, view) -> new Label(url)));
MarkdownView view = new MarkdownView("content", extensions);
```

## CSS Styling

`MarkdownView` uses a user agent stylesheet — any stylesheet you add automatically overrides defaults:

```java
markdownView.getStylesheets().add(getClass().getResource("/my-style.css").toExternalForm());
```

### CSS Variables

```css
* {
    -mdfx-font-color: black;
    -mdfx-link-color: blue;
    -mdfx-border-color-1: #888;
    -mdfx-bg-color-1: #ccc;       /* table even row */
    -mdfx-bg-color-2: #ddd;       /* table odd row */
    -mdfx-bg-color-3: #eee;       /* table header */
    -mdfx-bq-color-border: #4488cc;
    -mdfx-bq-color-background: #0000ff0c;
}
```

### Key Style Classes

`.markdown-text`, `.markdown-bold`, `.markdown-italic`, `.markdown-strikethrough`, `.markdown-link`, `.markdown-heading-1` through `.markdown-heading-5`, `.markdown-code`, `.markdown-code-block`, `.markdown-table-cell`, `.markdown-normal-block-quote`

## Syntax Highlighting

Fenced code blocks are rendered with TextMate-based syntax highlighting using [tm4javafx](https://github.com/mkpaz/tm4javafx) (VSCode-compatible TextMate grammars).

**Supported languages:** java, javascript/js, python/py, css, html, xml, json, yaml, typescript/ts, c, cpp, go, rust, ruby, swift, sql, bash/sh, groovy, dart, dockerfile, markdown, php, kotlin, http/rest

### Code Highlighting Theme via CSS

The `-mdfx-code-theme` CSS property controls which TextMate theme is used for syntax highlighting. It takes a resource path to a theme JSON file:

```css
/* Light (default) */
.markdown-code-block {
    -mdfx-code-theme: "/one/jpro/platform/mdfx/themes/github-light-default.json";
}

/* Dark */
.markdown-code-block {
    -mdfx-code-theme: "/one/jpro/platform/mdfx/themes/github-dark-default.json";
}
```

Users can point to their own TextMate theme JSON resource.

## Supported Markdown Features

Headings (H1–H5), bold, italic, strikethrough, links, images, ordered/unordered lists, task lists, fenced code blocks with syntax highlighting, inline code, GFM tables, blockquotes, attributes.
