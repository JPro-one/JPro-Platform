# JPro Utils

Various utilities for JavaFX.
To use them, you need the following repository:
```
  maven {
    url "https://sandec.jfrog.io/artifactory/repo"
  }
```

## JPro Sessions
#### Explanation
This library provides a simple implementation of a session manager for JavaFX/JPro applications.
It remembers the user, based on a cookie.
A simple ObservableMap is used to store the session data.
This data is only accessible in the JPro Server, not in the browser - which can be important for some security reasons.

Maven
```
<dependency>
  <groupId>one.jpro.jproutils</groupId>
  <artifactId>jpro-sessions</artifactId>
  <version>0.2.1</version>
</dependency>
```
### Usage
```
import one.jpro.sessionmanager.SessionManager;
...
static SessionManager sessionManager = new SessionManager("myapp");
...
ObservableMap session = sm.getSession(WebAPI.getWebAPI(primaryStage));
session.put("key", "value");
session.get("key");
session.remove("key");
```
Notes:
 * The SessionManager should be created only once, and should be static.
 * The SessionManager needs a name, which is used to identify the application.
 Different applications should use different names.



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
  <groupId>one.jpro.jproutils</groupId>
  <artifactId>tree-showing</artifactId>
  <version>0.2.1</version>
</dependency>
```

#### Gradle
```
dependencies {
    implementation 'one.jpro.jproutils:tree-showing:0.2.1'
}
```
The module name is `one.jpro.jfxutils.tree-showing`

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

### Freeze Detector
This library allows you to track whether the JavaFX Application Thread is frozen for a given time.
This can be useful for debugging purposes, detecting deadlocks or other optimize performance.



Internal Notes:

```
# local build
./gradlew tree-showing:publishToMavenLocal jpro-sound:publishToMavenLocal jpro-sessions:publishToMavenLocal htmlscrollpane:publishToMavenLocal freeze-detector:publishToMavenLocal
# publish release
./gradlew tree-showing:publish jpro-sound:publish jpro-session:publish htmlscrollpane:publish freeze-detector:publish
```