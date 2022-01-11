#JFX Utils

Various utilities for JavaFX.
To use them, you need the following repository:
```
  maven {
    url "https://sandec.jfrog.io/artifactory/repo"
  }
```

## TreeShowing
####Motivation
In JavaFX, you often want to stop an animation when a node is no longer used.
Internally in JavaFX, the property "treeShowing" can be used. But as an end user, no elegant options are available.
This leads to various incomplete solutions and memory leaks in applications.

With this project, we want to make the property available to the common JavaFX Developer.

This is also useful for other cleanup-scenarios, like stopping background tasks.

Maven
```
<dependency>
  <groupId>one.jpro.jfxutils</groupId>
  <artifactId>treeshowing</artifactId>
  <version>0.1.0</version>
</dependency>
```

#### Gradle
```
dependencies {
    implementation "one.jpro.jfxutils:treeshowing:0.1.0"
}
```
The module name is `one.jpro.jfxutils.treeshowing`

#### Typical Usage:
```
Timeline myTimeline = new Timeline();
myTimeline.setCycleCount(Timeline.INDEFINITE);
Node node = <MyNode>
treeShowing = TreeShowing.treeShowing(node);
treeShowing.addListener((p,o,showing) -> {
  if(showing) {
    myTimeline.start();
  } else {
    myTimeline.stop();
  }
});
if(treeShowing.get()) {
  myTimeline.start();
}
```

Internal Notes:
```
./gradlew tree-showing:publish
```