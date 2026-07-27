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

For everyday use, pin a node with the convenience methods:

```java
import javafx.geometry.Side;
import javafx.scene.Node;
import one.jpro.platform.scroll.Scroll;

Node header = createHeader();
Scroll.setStickyPosition(header);                  // sticky, pinned to the top edge
Scroll.setFixedPosition(fab, Side.BOTTOM, 24);     // fixed, 24px up from the bottom
Scroll.clearScrollPosition(header);                // back to normal flow
```

The available modes are:

| Mode                    | Behaviour                                                                 | CSS equivalent      |
|-------------------------|---------------------------------------------------------------------------|---------------------|
| `ScrollPosition.STATIC` | Default flow positioning; scrolls with the content.                       | `position: static`  |
| `ScrollPosition.STICKY` | Scrolls with the content until it reaches the edge, then stays pinned.    | `position: sticky`  |
| `ScrollPosition.FIXED`  | Pinned to the viewport; does not move while the page scrolls.             | `position: fixed`   |

### Canonical setter

The convenience methods delegate to a single canonical setter, which is handy for programmatic or
data-driven callers (for example applying a deserialized `ScrollPosition`):

```java
Scroll.setScrollPosition(node, ScrollPosition.STICKY, Side.TOP, 0);
```

Query the current mode with `Scroll.getScrollPosition(node)`, which returns `ScrollPosition.STATIC`
when none has been set.
