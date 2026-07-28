# JPro Sticky

`jpro-sticky` pins JavaFX nodes to the scrolling viewport, either **sticky** (scrolls with the
content until it reaches an edge, then stays pinned) or **fixed** (always pinned to the viewport).
It mirrors the CSS `position` property for nodes rendered by JPro.

On the web the pinning runs as a **compositor scroll-timeline override**, so scrolling stays smooth
with no JavaFX layout pass per scroll event. On the desktop the mode is a no-op and the node keeps
its normal flow positioning, so the same code runs in both targets.

## Installation

### Gradle

```groovy
dependencies {
    implementation("one.jpro.platform:jpro-sticky:0.7.2")
}
```

### Maven

```xml
<dependency>
    <groupId>one.jpro.platform</groupId>
    <artifactId>jpro-sticky</artifactId>
    <version>0.7.2</version>
</dependency>
```

**Module name:** `requires one.jpro.platform.sticky;`

## Quick start

Pin a node with the convenience methods on `Scroll`:

```java
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import one.jpro.platform.sticky.Scroll;

Scroll.setStickyPosition(header);                     // sticky, pinned to the top edge
Scroll.setFixedPosition(fab, Pos.BOTTOM_RIGHT, 24);   // fixed bottom-right corner, 24px inset
Scroll.setFixedBar(banner, Side.BOTTOM);              // fixed full-width bar at the bottom
Scroll.setFixedFullscreen(overlay);                   // fixed, filling the viewport
Scroll.clearScrollPosition(header);                   // back to normal flow
```

The modes mirror CSS `position`:

| Mode                    | Behaviour                                                              | CSS equivalent     |
|-------------------------|-----------------------------------------------------------------------|--------------------|
| `ScrollPosition.STATIC` | Default flow positioning; scrolls with the content.                   | `position: static` |
| `ScrollPosition.STICKY` | Scrolls with the content until it reaches the edge, then stays pinned. | `position: sticky` |
| `ScrollPosition.FIXED`  | Pinned to the viewport; does not move while the page scrolls.         | `position: fixed`  |

Query the current mode with `Scroll.getScrollPosition(node)`, which returns `ScrollPosition.STATIC`
when none has been set. Applying a new mode always tears down the previous one first, so switching
sticky and fixed (or clearing) never leaks listeners.

## Sticky positioning

A sticky node rides along in normal flow until it hits its pinned edge, then holds there while its
**containing block** is still on screen. By default the containing block is the node's parent:

```java
Scroll.setStickyPosition(header);              // pin to the top, bounded by the parent
Scroll.setStickyPosition(header, Side.TOP, 8); // 8px below the top edge
```

Pass an explicit container to have the node release at that container's bottom instead. This is the
CSS pattern where a section sub-header stays pinned while its own section scrolls past, then lets go:

```java
Scroll.setStickyPosition(subHeader, Side.TOP, 48, section);
```

Sticky only pins to an edge (`Side.TOP`, `BOTTOM`, `LEFT`, `RIGHT`). Centering and stretching are
fixed-only.

## Fixed positioning

A fixed node is anchored to the viewport and never moves with the page. Reach for the convenience
methods first:

```java
Scroll.setFixedPosition(node);                        // top edge
Scroll.setFixedPosition(node, Side.BOTTOM, 16);       // 16px up from the bottom (a chip, not a bar)
Scroll.setFixedPosition(node, Pos.TOP_CENTER, 24);    // any of the 9 Pos anchors, inset from pinned edges
Scroll.setFixedBar(node, Side.BOTTOM);                // full-width bar pinned to an edge
Scroll.setFixedFullscreen(node);                      // stretched to fill the viewport
```

`setFixedPosition(node, Side, offset)` pins one edge and leaves the other axis at its natural size (a
chip). For a full-span bar use `setFixedBar`.

### The anchor model

Under the convenience methods sits `ScrollAnchor`, an immutable value object that resolves the
horizontal and vertical axes **independently**. Each axis takes one of: pin start, pin end, center,
or stretch (pin both edges). This is what lets a single model express corners, bars, toasts, and
overlays. Build one fluently from `ScrollAnchor.of()`:

```java
import one.jpro.platform.sticky.ScrollAnchor;

ScrollAnchor.of().bottom(0);                 // bottom edge
ScrollAnchor.of().bottom(24).right(24);      // bottom-right corner
ScrollAnchor.of().top(0).left(0).right(0);   // top bar: pinned top, stretched across the width
ScrollAnchor.of().top(16).centerX();         // toast: pinned near the top, horizontally centered
ScrollAnchor.of().all(0);                     // full-viewport overlay
```

Pass the anchor to `setFixedPosition`:

```java
Scroll.setFixedPosition(toast, ScrollAnchor.of().top(16).centerX());
```

Setting two conflicting things on one axis (for example `centerX()` and `left(0)`) throws
`IllegalStateException`. Setting a `CENTER` or `STRETCH` axis on a **sticky** node throws
`IllegalArgumentException`, since CSS sticky neither centers nor stretches.

## Canonical setter

The convenience methods all delegate to one canonical setter. It is the entry point for programmatic
or data-driven callers, such as applying a deserialized `ScrollPosition`:

```java
Scroll.setScrollPosition(node, ScrollPosition.STICKY, ScrollAnchor.of().top(0));
Scroll.setScrollPosition(node, ScrollPosition.FIXED,  ScrollAnchor.of().bottom(0).right(0));

// sticky with an explicit containing block:
Scroll.setScrollPosition(subHeader, ScrollPosition.STICKY, ScrollAnchor.of().top(48), section);
```

Edge-based overloads (`setScrollPosition(node, position, Side, offset)` and the bare
`setScrollPosition(node, position)`) cover the single-edge case.

## Native-scrolling page requirement

The web override tracks the page's own scroll, so the hosting page **must scroll natively**. Enable
native scrolling on the `<jpro-app>` tag and let the body scroll. Without this there is no scroll for
the compositor timeline to follow and nothing pins:

```html
<style>
  html, body {
    max-width: 100%;
    overflow-x: hidden;
    margin: 0;
  }
</style>

<jpro-app href="/app/default" nativescrolling="true" fxHeight="true" nativeZooming="true"></jpro-app>
```

## Scope and limits (v1)

- **Web only.** The pin is a browser compositor effect. On the desktop every mode is an inert no-op
  and the node stays in normal flow.
- **Native scrolling required.** See the section above.
- **Sticky is edge-only.** Center and stretch anchors are rejected for sticky; they are valid for
  fixed.

## Running the example

`ScrollSample` demonstrates the full surface: a sticky page header, a bounded sticky section
sub-header that releases at the section end, and fixed elements at every anchor kind (bottom bar,
corner FAB, centered toast, full-viewport frame). Several of them are click-counter buttons so
picking through the overlay is visible.

Because it needs a native-scrolling page, run it on the web with the JPro Gradle plugin:

```shell
./gradlew jpro-sticky:example:jproRun
```

Then open <http://localhost:8080/>.
