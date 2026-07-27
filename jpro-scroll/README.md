# JPro Scroll

`jpro-scroll` provides scroll-aware positioning for JavaFX nodes rendered by JPro. It lets you pin
a node to the scrolling viewport — either **sticky** (scrolls with the content until it reaches an
edge, then stays pinned) or **fixed** (always pinned to the viewport) — mirroring the CSS `position`
property. On the web the pinning is realised through a compositor override, so scrolling stays
smooth without a JavaFX layout pass per scroll event. As a desktop application the mode is a no-op
and the node keeps its normal flow positioning.

## Dependency

```groovy
dependencies {
    implementation("one.jpro.platform:jpro-scroll:0.7.2")
}
```

## Usage

Attach a positioning mode to any node with `Scroll.setPosition`:

```java
import javafx.scene.Node;
import one.jpro.platform.scroll.Scroll;
import one.jpro.platform.scroll.ScrollPosition;

Node header = createHeader();
Scroll.setPosition(header, ScrollPosition.STICKY);
```

The available modes are:

| Mode                    | Behaviour                                                                 | CSS equivalent      |
|-------------------------|---------------------------------------------------------------------------|---------------------|
| `ScrollPosition.STATIC` | Default flow positioning; scrolls with the content.                       | `position: static`  |
| `ScrollPosition.STICKY` | Scrolls with the content until it reaches the edge, then stays pinned.    | `position: sticky`  |
| `ScrollPosition.FIXED`  | Pinned to the viewport; does not move while the page scrolls.             | `position: fixed`   |

Query the current mode with `Scroll.getPosition(node)`, which returns `ScrollPosition.STATIC` when
none has been set.
