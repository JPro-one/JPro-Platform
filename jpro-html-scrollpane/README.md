# JPro HTML ScrollPane Skin

> **Experimental / prototype.** This module is an early prototype — the API and behavior may change,
> and it is not yet recommended for production use.

`jpro-html-scrollpane` provides `HTMLScrollPaneSkin`, a custom skin for the JavaFX `ScrollPane` that,
when running in the browser under JPro, scrolls its content using a native HTML scroll container
instead of the JavaFX scrollbars. This gives smooth, native scrolling (including momentum/touch) for
web deployments, while still working on the desktop.

## Dependency

```groovy
dependencies {
    implementation("one.jpro.platform:jpro-html-scrollpane:0.7.2")
}
```

## Usage

Apply the skin to a `ScrollPane`:

```java
ScrollPane scrollPane = new ScrollPane(content);
scrollPane.setSkin(new HTMLScrollPaneSkin(scrollPane));
```

An optional second constructor argument passes extra attributes to the underlying HTML container:

```java
scrollPane.setSkin(new HTMLScrollPaneSkin(scrollPane, "additional-attributes"));
```

The skin tracks the scroll pane's visibility (via `jpro-utils` `TreeShowing`) and cleans up the
embedded HTML view when the pane leaves the scene graph, avoiding the resource leaks that embedded
web content can otherwise cause.
