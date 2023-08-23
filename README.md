# JPro Platform
![Build](https://github.com/jpro-one/jpro-utils/actions/workflows/linux.yml/badge.svg)

The JPro Platform represents the foundation of cross-platform application development,
seamlessly integrating the power of JavaFX with the limitless potential of web-based applications.
By offering specialized modules and a dedicated API, JPro ensures developers can harness the strengths of JavaFX
while deploying applications that run beautifully on the web.

### Key Features:
* **Modular Components**: Begin your development journey with an assortment of pre-configured modules designed to 
bolster various functionalities and UI/UX standards. Each module encapsulates reusability, encouraging developers to 
integrate them across a multitude of projects.
* **Web-Ready JavaFX Applications**: JPro empowers developers to run JavaFX applications directly in the web browser,
eliminating the boundary between desktop and web applications.
* **Cross-Platform API**: The JPro Web API provides a simplified interface that meshes with JavaFX constructs, effortlessly
catering to the specifics of web-based deployments.

### Benefits:
* **Unified Development Paradigm**: JPro provides a cohesive development environment, ensuring consistent behavior 
between JavaFX and web deployments.
* **Accelerated Deployment**: The combination of ready-made modules and a user-friendly API paves the way for rapid
development and deployment.
* **Economic Efficiency**: The ability to cater to both JavaFX and web platforms using a singular codebase leads to
significant savings in development time, resources, and costs.
____

To use them, you need the following repository:
```
  maven {
    url "https://sandec.jfrog.io/artifactory/repo"
  }
```

## JPro Auth
Rely on `jpro-auth` module to add sophisticated authentication and authorization to your JPro/JavaFX applications.
Finely control access with a degree of customization that can accommodate even the most complex security requirements.

#### Maven configuration
```maven
<dependency>
  <groupId>one.jpro.platform</groupId>
  <artifactId>jpro-auth</artifactId>
   <version>0.2.4-SNAPSHOT</version>
</dependency>
```

#### Gradle configuration
```gradle
dependencies {
    implementation 'one.jpro.platform:jpro-auth:0.2.4-SNAPSHOT'
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
  <groupId>one.jpro.platform</groupId>
  <artifactId>jpro-sessions</artifactId>
   <version>0.2.2</version>
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

## Media
#### Maven
```
<dependency>
  <groupId>one.jpro.platform</groupId>
  <artifactId>jpro-media</artifactId>
  <version>0.2.3</version>
</dependency>
```

#### Gradle
```
dependencies {
    implementation 'one.jpro.platform:jpro-media:0.2.3'
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

#### Maven
```
<dependency>
  <groupId>one.jpro.platform</groupId>
  <artifactId>tree-showing</artifactId>
  <version>0.2.2</version>
</dependency>
```

#### Gradle
```
dependencies {
    implementation 'one.jpro.platform:tree-showing:0.2.3'
}
```
The module name is `jpro.utils.treeshowing`

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
./gradlew publishToMavenLocal
# publish release
./gradlew publish
```