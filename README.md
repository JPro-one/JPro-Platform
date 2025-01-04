# JPro Platform
![Build](https://github.com/jpro-one/jpro-platform/actions/workflows/main.yml/badge.svg)
[![JPro supported](https://img.shields.io/badge/JPro-supported-brightgreen.svg)](https://www.jpro.one/)

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

All release builds are now available on [Maven Central](https://central.sonatype.com/search?namespace=one.jpro.platform).

To use the snapshot builds, you need the following repository:
- For Maven:

```xml
<repositories>
  <repository>
    <id>jpro-sandec-repository</id>
    <url>https://sandec.jfrog.io/artifactory/repo/</url>
  </repository>
</repositories>
```
- For Gradle:

```groovy
repositories {
    maven {
        url "https://sandec.jfrog.io/artifactory/repo"
    }
}
```

## JPro Auth
Rely on this library to add sophisticated authentication and authorization to your **JPro/JavaFX** applications.
Finely control access with a degree of customization that can accommodate even the most complex security requirements.

### Auth Core Module
This library provides core functionality via simple API to authenticate, authorize and manage user roles and attributes
in order to check whether a user is authorized to access a specific resource. It also provides a JWT (JSON Web Token)
and OAuth2 (and to some extent OpenID Connect) implementation.

#### Maven configuration
```xml
<dependencies>
  <dependency>
    <groupId>one.jpro.platform</groupId>
    <artifactId>jpro-auth-core</artifactId>
    <version>0.5.1-SNAPSHOT</version>
  </dependency>
</dependencies>
```

#### Gradle configuration
```groovy
dependencies {
    implementation("one.jpro.platform:jpro-auth-core:0.5.1-SNAPSHOT")
}
```

## JPro SceneGraph
Creates human and AI friendly String representations of JavaFX SceneGraphs.

#### Maven configuration
```xml
<dependencies>
  <dependency>
    <groupId>one.jpro.platform</groupId>
    <artifactId>jpro-scenegraph</artifactId>
    <version>0.5.1-SNAPSHOT</version>
  </dependency>
</dependencies>
```

#### Gradle configuration
```groovy
dependencies {
    implementation("one.jpro.platform:jpro-scenegraph:0.5.1-SNAPSHOT")
}
```

## JPro File
This library provides a simple way to pick, drop, upload and download files in **JPro/JavaFX** applications.

#### Maven configuration
```xml
<dependencies>
  <dependency>
    <groupId>one.jpro.platform</groupId>
    <artifactId>jpro-file</artifactId>
    <version>0.5.1-SNAPSHOT</version>
  </dependency>
</dependencies>
```

#### Gradle configuration
```groovy
dependencies {
    implementation("one.jpro.platform:jpro-file:0.5.1-SNAPSHOT")
}
```

#### Lunch the examples
* As desktop application
1) Run the text editor sample 
   ```shell
   ./gradlew jpro-file:example:run -Psample=text-editor
   ```
2) Run the file uploader sample 
   ```shell
   ./gradlew jpro-file:example:run -Psample=file-uploader
   ```
* As JPro application
1) Run the text editor sample
   ```shell
   ./gradlew jpro-file:example:jproRun -Psample=text-editor
   ```
2) Run the file uploader sample
   ```shell
   ./gradlew jpro-file:example:jproRun -Psample=file-uploader
   ```

## JPro Image Manager
This library makes very easy to manage the process of loading and caching images, allowing efficient retrieval 
and processing in **JPro/JavaFX** applications. It allows you to load images from the classpath, the file system, 
or from the web. The images can be encoded in various formats, such as PNG, JPEG and transformed to fit and scale
to the desired size.

#### Maven configuration
```xml
<dependencies>
  <dependency>
    <groupId>one.jpro.platform</groupId>
    <artifactId>jpro-image-manager</artifactId>
    <version>0.5.1-SNAPSHOT</version>
  </dependency>
</dependencies>
```

#### Gradle configuration
```groovy
dependencies {
    implementation("one.jpro.platform:jpro-image-manager:0.5.1-SNAPSHOT")
}
```

## [JPro Mail](https://github.com/JPro-one/jpro-platform/tree/main/jpro-mail)
This library provides a simple way to send emails in **JPro/JavaFX** applications. It allows you to send emails
using SMTP and SMTPS protocols. It also provides a simple way to compose and send emails with attachments.

#### Maven configuration
```xml
<dependencies>
  <dependency>
    <groupId>one.jpro.platform</groupId>
    <artifactId>jpro-mail</artifactId>
    <version>0.5.1-SNAPSHOT</version>
  </dependency>
</dependencies>
```

#### Gradle configuration
```groovy
dependencies {
    implementation("one.jpro.platform:jpro-mail:0.5.1-SNAPSHOT")
}
```

## [JPro Media](https://github.com/JPro-one/jpro-platform/tree/main/jpro-media)
This library is designed for audio and video playback and recording within JavaFX applications.
It seamlessly operates on both desktop and mobile devices, as well as in web browsers via **JPro**, 
all while utilizing the same codebase.

#### Maven configuration
```xml
<dependencies>
  <dependency>
    <groupId>one.jpro.platform</groupId>
    <artifactId>jpro-media</artifactId>
    <version>0.5.1-SNAPSHOT</version>
  </dependency>

  <dependency>
      <groupId>org.bytedeco</groupId>
      <artifactId>javacv-platform</artifactId>
      <version>1.5.10</version>
      <!-- use compile scope when running/deploying with JPro,-->
      <!-- since the platform related libraries are no more needed-->
      <!-- <scope>compile</scope>-->
  </dependency>
</dependencies>
```

#### Gradle configuration
```groovy
plugins {
    id 'org.bytedeco.gradle-javacpp-platform' version "1.5.10"
}

dependencies {
    implementation("one.jpro.platform:jpro-media:0.5.1-SNAPSHOT")
    implementation "org.bytedeco:javacv-platform:1.5.10" // use compileOnly configuration when running/deploying with JPro
}
```

## JPro Routing
A minimalistic routing library for **JPro/JavaFX** applications. It allows you to define routes and to navigate between
them. Pages are indexed by Google and the current link is updated in the browser. It works on desktop and mobile devices.
Currently, there are three routing modules available:

1. **jpro-routing-core**: the core module, which is required by all other modules.

#### Maven configuration
```xml
<dependencies>
  <dependency>
    <groupId>one.jpro.platform</groupId>
    <artifactId>jpro-routing-core</artifactId>
    <version>0.5.1-SNAPSHOT</version>
  </dependency>
</dependencies>
```

#### Gradle configuration
```groovy
dependencies {
    implementation("one.jpro.platform:jpro-routing-core:0.5.1-SNAPSHOT")
}
```

2. **jpro-routing-dev**: a module that provides a development environment for routing.

#### Maven configuration
```xml
<dependencies>
  <dependency>
    <groupId>one.jpro.platform</groupId>
    <artifactId>jpro-routing-dev</artifactId>
    <version>0.5.1-SNAPSHOT</version>
  </dependency>
</dependencies>
```

#### Gradle configuration
```groovy
dependencies {
    implementation("one.jpro.platform:jpro-routing-dev:0.5.1-SNAPSHOT")
}
```

3. **jpro-routing-popup**: a module that provides a popup window while using routing.

#### Maven configuration
```xml
<dependencies>
  <dependency>
    <groupId>one.jpro.platform</groupId>
    <artifactId>jpro-routing-popup</artifactId>
    <version>0.5.1-SNAPSHOT</version>
  </dependency>
</dependencies>
```

#### Gradle configuration
```groovy
dependencies {
    implementation("one.jpro.platform:jpro-routing-popup:0.5.1-SNAPSHOT")
}
```

## JPro Markdown (formerly MDFX)
This library allows you to render Markdown formatted content in your **JPro/JavaFX** applications.

#### Maven configuration
```xml
<dependencies>
  <dependency>
    <groupId>one.jpro.platform</groupId>
    <artifactId>jpro-mdfx</artifactId>
    <version>0.5.1-SNAPSHOT</version>
  </dependency>
</dependencies>
```

#### Gradle configuration
```groovy
dependencies {
    implementation("one.jpro.platform:jpro-mdfx:0.5.1-SNAPSHOT")
}
```

## JPro Session
This library provides a simple implementation of a session manager for **JavaFX/JPro** applications.
It remembers the user, based on a cookie. A simple ObservableMap is used to store the session data.
This data is only accessible in the JPro Server, not in the browser - which can be important for some security reasons.

#### Maven configuration
```xml
<dependencies>
  <dependency>
    <groupId>one.jpro.platform</groupId>
    <artifactId>jpro-session</artifactId>
    <version>0.5.1-SNAPSHOT</version>
  </dependency>
</dependencies>
```

#### Gradle configuration
```groovy
dependencies {
    implementation("one.jpro.platform:jpro-session:0.5.1-SNAPSHOT")
}
```

## JPro Utils
This library offers essential tools for various functionalities to enhance the development of **JPro/JavaFX** applications.

#### Maven configuration
```xml
<dependency>
  <dependency>
    <groupId>one.jpro.platform</groupId>
    <artifactId>jpro-utils</artifactId>
    <version>0.5.1-SNAPSHOT</version>
  </dependency>
</dependency>
```

#### Gradle configuration
```groovy
dependencies {
    implementation 'one.jpro.platform:jpro-utils:0.5.1-SNAPSHOT'
}
```

## JPro WebRTC
This library provides an API to use WebRTC in **JPro/JavaFX** applications. This technology allows for the direct 
exchange of audio, video, and data between web browsers or devices, facilitating features like video conferencing,
voice calling, and peer-to-peer file sharing directly in the web browser without requiring additional plugins or 
software. WebRTC is designed to be versatile and secure, employing end-to-end encryption to ensure privacy and data
integrity. Its integration into major browsers and its extensive API make it a popular choice for developers building
real-time communication capabilities into web applications, particularly in contexts where low latency and direct 
peer-to-peer communication are essential. For a Java and JavaFX developer, WebRTC offers a pathway to integrate 
real-time communication features into web applications, potentially enhancing user interaction and collaboration
capabilities.

#### Maven configuration
```xml
<dependencies>
  <dependency>
    <groupId>one.jpro.platform</groupId>
    <artifactId>jpro-webrtc</artifactId>
    <version>0.5.1-SNAPSHOT</version>
  </dependency>
</dependencies>
```

#### Gradle configuration
```groovy
dependencies {
    implementation("one.jpro.platform:jpro-webrtc:0.5.1-SNAPSHOT")
}
```

## JPro YouTube
This library makes it easy to embed a YouTube video in your **JPro/JavaFX** applications. It provides a simple API
to embed the video in a JavaFX node called `YoutubeNode`. The video can be played, paused, stopped, and muted via
the embedded controls. The video can be played in full-screen mode.

#### Maven configuration
```xml
<dependencies>
  <dependency>
    <groupId>one.jpro.platform</groupId>
    <artifactId>jpro-youtube</artifactId>
    <version>0.5.1-SNAPSHOT</version>
  </dependency>
</dependencies>
```

#### Gradle configuration
```groovy
dependencies {
    implementation("one.jpro.platform:jpro-youtube:0.5.1-SNAPSHOT")
}
```

### Usage
```
import one.jpro.platform.sessions.SessionManager;
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

## JPro HTML Scrollpane
Provides a skin implementation of a scrollpane for **JPro** applications only.

#### Maven configuration
```xml
<dependencies>
  <dependency>
    <groupId>one.jpro.platform</groupId>
    <artifactId>jpro-html-scrollpane</artifactId>
    <version>0.5.1-SNAPSHOT</version>
  </dependency>
</dependencies>
```

#### Gradle configuration
```groovy
dependencies {
    implementation("one.jpro.platform:jpro-html-scrollpane:0.5.1-SNAPSHOT")
}
```

## Launch the examples
To run the examples, you can use the following commands:

- As desktop application
```shell
./gradlew example:run

./gradlew jpro-auth:example:run -Psample=basic-login
./gradlew jpro-auth:example:run -Psample=google-login
./gradlew jpro-auth:example:run -Psample=oauth
./gradlew jpro-file:example:run -Psample=text-editor
./gradlew jpro-file:example:run -Psample=file-uploader
./gradlew jpro-mail:example:run -Psample=compose-mail
./gradlew jpro-media:example:run -Psample=media-player
./gradlew jpro-media:example:run -Psample=media-recorder
./gradlew jpro-media:example:run -Psample=media-recorder-and-player
./gradlew jpro-routing:example:run -Psample=colors
./gradlew jpro-routing:example:run -Psample=popup
./gradlew jpro-routing:example:run -Psample=test
```

- As JPro application
```shell
./gradlew example:jproRun

./gradlew jpro-auth:example:jproRun -Psample=basic-login
./gradlew jpro-auth:example:jproRun -Psample=google-login
./gradlew jpro-auth:example:jproRun -Psample=oauth
./gradlew jpro-file:example:jproRun -Psample=text-editor
./gradlew jpro-file:example:jproRun -Psample=file-uploader
./gradlew jpro-mail:example:jproRun -Psample=compose-mail
./gradlew jpro-media:example:jproRun -Psample=media-player
./gradlew jpro-media:example:jproRun -Psample=media-recorder
./gradlew jpro-media:example:jproRun -Psample=media-recorder-and-player
./gradlew jpro-routing:example:jproRun -Psample=colors
./gradlew jpro-routing:example:jproRun -Psample=popup
./gradlew jpro-routing:example:jproRun -Psample=test
./gradlew jpro-routing:example:jproRun -Psample=extensions
./gradlew jpro-webrtc:example:jproRun
./gradlew jpro-sipjs:example:jproRun
```

- Run the [ensemble](https://www.jfx-ensemble.com/) website
```shell
./gradlew ensemble:site:jproRun
```
