# JFX Utils

Various utilities for JavaFX.
To use them, you need the following repository:
```
  maven {
    url "https://sandec.jfrog.io/artifactory/repo"
  }
```

## TreeShowing
#### Motivation
In JavaFX, when a node could be collected - it's often prevented by ongoing animation or background tasks.
For this reason, it's often necessary to stop the animation or background task, 
when a node is no longer used.
Internally in JavaFX, the property "treeShowing" is used, to check whether a Node is still used.
But as an end-user, this property is not accessible in their application.
With this project, we want to make the property available to the common JavaFX Developer - allowing them to write bug-free and leak-free applications.

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
# local build
./gradlew tree-showing:publishToMavenLocal jpro-sound:publishToMavenLocal
# publish release
./gradlew tree-showing:publish jpro-sound:publish
```