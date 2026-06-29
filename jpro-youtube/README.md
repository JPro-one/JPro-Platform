# JPro YouTube

`jpro-youtube` provides a `YoutubeNode` that embeds a YouTube video in a JPro/JavaFX application. In
the browser it uses a native YouTube `<iframe>`; on the desktop it falls back to a `WebView`.

## Dependency

```groovy
dependencies {
    implementation("one.jpro.platform:jpro-youtube:0.7.2")
}
```

## Usage

Construct the node with a YouTube video id (the part after `v=` in a watch URL):

```java
import one.jpro.platform.youtube.YoutubeNode;

// https://www.youtube.com/watch?v=dQw4w9WgXcQ  ->  videoId = "dQw4w9WgXcQ"
YoutubeNode video = new YoutubeNode("dQw4w9WgXcQ");
```

`YoutubeNode` is a `StackPane`. It keeps a 16:9 aspect ratio and resizes the embedded player with
the node, so add it to any layout and size it as usual.
