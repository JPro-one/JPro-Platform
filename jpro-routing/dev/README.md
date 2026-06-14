# JPro Routing Dev

`jpro-routing-dev` provides development-time overlays for [JPro Routing](../README.md)
applications. Apply them as transformers on your route and remove (or conditionally apply) them in
production builds.

## Dependency

```groovy
dependencies {
    implementation("one.jpro.platform:jpro-routing-dev:0.7.1")
}
```

## Usage

```java
route
    .transform(DevTransformer.create())          // dev toolbar
    .transform(StatisticsTransformer.create());  // live performance statistics (browser)
```

### DevTransformer

`DevTransformer.create()` returns a `Transformer` that wraps every page with a development toolbar:

- back / forward / refresh navigation and an editable URL bar,
- a [ScenicView](https://github.com/JonathanGiles/scenic-view) launcher,
- a "pages uncollected" counter (backed by JMemoryBuddy) and a force-GC button,
- live CSS reloading via [CSSFX](https://github.com/McFoggy/cssfx).

### StatisticsTransformer

`StatisticsTransformer.create()` returns a `Transformer` that, when running in the browser, shows
live statistics: latency, bytes sent/received, number of synchronized / created / collected nodes,
and load times.

> Both are intended for development only. Apply them conditionally (e.g. behind a debug flag) so
> they don't ship in production.
