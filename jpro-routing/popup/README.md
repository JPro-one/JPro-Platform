# JPro Routing Popup

`jpro-routing-popup` provides popups and loading overlays that work in [JPro Routing](../README.md)
applications, on both desktop and in the browser.

## Dependency

```groovy
dependencies {
    implementation("one.jpro.platform:jpro-routing-popup:0.7.2")
}
```

## Usage

Add the popup container transformer once, at the end of your route definition. It installs the
overlay layer that popups are rendered into:

```java
route.transform(PopupAPI.createPopupContainerTransformer());
```

Then, from anywhere in the scene graph (`node` is any node in the application):

```java
// open / close a popup
PopupAPI.openPopup(node, SimplePopups.infoPopup("Info", "Saved successfully."));
PopupAPI.closePopup(anyNodeInsidePopup);

// show a loading overlay until a future completes
FXFuture<Result> result = PopupAPI.showLoadingScreen(node, someFXFuture);
```

`openPopup` and `closePopup` locate the popup container by walking up from the given node, so the
node must be attached to the application's scene graph.

## API

`PopupAPI`
- `createPopupContainerTransformer()` — the route transformer that installs the overlay layer.
- `openPopup(Node contextHolder, Node popup)` — show `popup` in the overlay.
- `closePopup(Node anyNodeInPopup)` — close the popup containing the given node.
- `showLoadingScreen(Node contextHolder, FXFuture<T> future)` — show a loading overlay until
  `future` completes; returns the same future for chaining.
- `registerPopupContainer(Pane container)` — register a custom container (advanced).

`SimplePopup` — a prebuilt popup (`StackPane`) with a title bar, content area and button area;
constructible directly via `new SimplePopup(title, content, buttons, closable)`.

`SimplePopups` — factories for common cases, e.g. `infoPopup(String title, String infoText)`.
