### 0.3.3-SNAPSHOT (TBD)


----------------------

### 0.3.2 (June 12, 2024)

#### Improvements
* Improved error handling in the `jpro-sipjs` module.

#### Bugfixes
* Fixed URI normalization for local addresses ikn the `jpro-auth-core` module.
* Fixed a NPE thrown if the session only has redirects in the `jpro-routing-core` module.
* Routing refreshes no longer adds a new entry to the browser's history.
* Fixed hangup button in the SipJS Demos. If an error happens, getting a screen, it is now logged.
Screen sharing button "toggles" now it's state - making it possible to switch back to video.

----------------------

### 0.3.1 (May 14, 2024)

#### Bugfixes
* Fixed the retrival of the domain from the request in the Request class in the `jpro-routing-core` module. Now `domain`
in the route is correctly matched with the request domain.
* Fix the server url when `useLoopbackIpAddress` OAuth2 option is used.

----------------------

### 0.3.0 (May 6, 2024)

#### Improvements
* Updated **JPro** to version `2024.2.0`.
* Updated **JavaFX** dependencies to version `17.0.11`.
* Updated **SimpleFX** dependencies to version `3.2.33`.
* Some minor SipJS improvements.

----------------------

### 0.2.17 (May 1, 2024)

#### Features
* Added `runAsync` method to the `CommandRunner` class in the `jpro-internal-util` module. This method can be used
  to run a command asynchronously. This is useful for long-running commands that should not block the main thread.

----------------------

### 0.2.16 (April 8, 2024)

#### Features
* Added `setPrintToConsole` method to CommandRunner class in the `jpro-internal-util` module. This method can be used
  to set the print output of the command runner to the console. This is useful for debugging purposes.

----------------------

### 0.2.15 (March 21, 2024)

#### Improvements
* Updated **SimpleFX** dependencies to version `3.2.32`.
* Added `ensemble` running command in the documentation.

#### Features
* Added native support to `jpro-media` module for the Linux `arm64` architecture.
* Added `getAttribute(String key)` method to the User class in `jpro-auth-core` module. This will help developers to
easily retrieve attributes from the user object. Add also `getEmail()` method to simplify the retrieval of the email
from the attribute.
* Added `UserSession` class to the authentication filters to simplify the User storage and retrieval process.

#### Bugfixes
* Minor fixes and documentation improvements.

----------------------

### 0.2.14 (March 5, 2024)

#### Changes
* Renamed `jpro-sessions` module to `jpro-session`.

----------------------

### 0.2.13 (February 14, 2024)

#### Features
* Added `jpro-internal-util` module to the platform. This module contains internal utilities that are used by other
  modules in the platform. `PlatformUtils` moved from `jpro-internal-openlink` to this module in the process.

----------------------
### 0.2.12 (February 7. 2024)

#### Improvements
* Updated **SimpleFX** to version `3.2.30`.

#### Features
* Added the `jpro-scenegraph` module, which can be used to create a human and AI friendly 
  String representation of a scene graph.

----------------------
### 0.2.11 (January 22, 2024)

#### Features
* Added `basic` authentication API to the `jpro-auth-core` module. This API can be used to authenticate users
  with a username and password. Checkout the `jpro-auth-core` module documentation for more details or the provided
  example on how to use it.
* Added `AuthFilter` to the `jpro-auth-routing` module. This filter can be used to authenticate users with a 
  username and password. Checkout the `jpro-auth-routing` module documentation for more details or the provided
  example on how to use it.

#### Changes
* Renamed authentication filter classes in the `jpro-auth-routing` module to be more consistent with the rest of the
  platform. Specifically, the `OAuth2Filter` class was renamed to `AuthOAuth2Filter` and the `JWTFilter` class was
  renamed to `AuthJWTFilter`.

#### Improvements
* Updated JPro dependencies to version `2024.1.0`.
* Updated **JavaFX** dependencies to version `17.0.10`.
* Improved the `jpro-auth-core` module documentation.

----------------------
### 0.2.10 (December 29, 2023)

#### Features
* Added `IncrementalLoading` to the routing module. Checkout the routing documentation for more details.
* In the `jpro-auth-core` module added Google specific implementation for token introspection.

#### Improvements
* The statistics and dev filter now have backgrounds and a slightly better design.
* Improved the FreezeDetector module.
* Added ContainerVBox which can be used to create a `ContainerFilter`.
* Simplify the authorization process for a given OAuth2 authentication provider via the `jpro-auth-routing` module
by calling the `OAuth2Filter.authorize` method.

----------------------
### 0.2.9 (December 21, 2023)

#### Changes
* Moved Methods from RouteUtils to Route - for more consistent APIs.
* Removed the methods Route.getView and Route.getNode for more consistent APIs.

#### Improvements
* Added Documentation for `jpro-routing` modules.

----------------------
### 0.2.8 (December 19, 2023)

#### Changes
* Changed the types in the Routing. In most cases these are still compatible because utility methods are used.
  This made the API easier to use by hiding complexity which is only needed in rare cases. 
  - Before: `Route: Request => FXFuture[Response]`
  - Now: `Route: Request => Response` where `Response: FXFuture[ResponseResult]`

* Fixed parts of the `Route.when` api, which had unexpected argument types.
* Removed `ResponseUtils.redirect` and replaced it with `Response.redirect` and also added `Response.fromNode`

#### Improvements
* Updated JPro to version `2023.3.3`.
* Simplified the creation and usage of all OpenID based OAuth2 authentication providers in the `jpro-auth-core` module.
* Added `login` example for JPro Auth modules. This example shows how to use the JPro Auth modules to authenticate
  users in a JPro application. Simplify the launch of examples in the process.

#### Features
* Added `jpro-auth-routing` module to the platform that provides the API to simplify the combination of  routing 
  capabilities during the authentication process. Simultaneously, `jpro-auth` module was renamed to `jpro-auth-core`
  and everything under `one.jpro.platform.auth` package was moved to `one.jpro.platform.auth.core` package.

#### Bugfixes
* Fixed web implementation of the `mediaView` **freezing** after the node was removed from the scene and then added 
  back.

----------------------
### 0.2.7 (December 8, 2023)

#### Improvements
* Updated JPro to version `2023.3.2`.
* Updated **SimpleFX** to version `3.2.27`.
* Improve documentation for the `jpro-media` module.

#### Features
* Added `jpro-file` module to the platform. For more information, navigate to the 
  [jpro-file](https://github.com/JPro-one/JPro-Platform/tree/main/jpro-file#readme) module.
* Added `jpro-webartc` module to the platform. Provides an API to use WebRTC in JPro running application.
* Added `jpro-youtube` module to the platform. Makes easy to embed a YouTube video by simply using `YoutubeNode`.
* Added `ensemble` module to publish platform examples.
* Added Image and YouTube extensions to the `jpro-mdfx` module.
* Added Domain Filters to the `jpro-routing-core` module. This is a simple example on how to use it:

```java
public class DomainExample extends RouteApp {
  @Override
  public Route createRoute() {
    return Route.empty()
            .domain("127.0.0.1", request -> FXFuture.unit(new Redirect("http://localhost:" +
                    request.port() + request.path() + toQueryString(request.queryParameters()))));
  }
}
```

#### Bugfixes
* In `jpro-media` module, correctly reassign a media engine to the 
  [WebMediaView](https://github.com/JPro-one/JPro-Platform/blob/main/jpro-media/src/main/java/one/jpro/platform/media/WebMediaView.java) 
  by calling `setMediaEngine` method when running via JPro.
* In `jpro-media` module, correctly resize the video element in the
  [WebMediaView](https://github.com/JPro-one/JPro-Platform/blob/main/jpro-media/src/main/java/one/jpro/platform/media/WebMediaView.java)
  after calling `setMediaEngine` method when running via JPro.

#### Changes
* In the `jpro-auth` module, the package named `one.jpro.platform.auth.oath2` was correctly renamed to 
`one.jpro.platform.auth.oauth2`.

----------------------
### 0.2.6 (September 28, 2023)

#### Improvements
* Updated JPro to version `2023.3.0`.
* Update **SimpleFX** to version `3.2.25`.

#### Features
* Add `deleteCache` script for Windows platform. This script is used to delete the jars related to JPro Platform
  modules from Gradle and Maven locally on Windows.

#### Bugfixes
* Provide a more compatible solution to open URls on Windows platform via the OpenLink internal module.

----------------------
### 0.2.5 (September 28, 2023)

#### Improvements
* Improved **Null** checks for URLs and logging in the OpenLink internal module.

#### Bugfixes
* ScrollPane hotfix applied in the `jpro-scrollpane` module.

----------------------
### 0.2.4 (September 27, 2023)

* Migrating everything from `JPro Utils` to the platform. Change module prefixes to `one.jpro.platform` 
  in the process.
* Importing `jpro-routing` projects under the platform.
* Add `jpro-image-manager` module to the platform.
* Add `jpro-mdfx` module to the platform.
* Add `jpro-internal-openlink` module to the platform.
* Add `deleteCache` script for Linux/macOS platform. This script is used to delete the jars related to JPro Platform
  modules from Gradle and Maven locally on Linux/macOS.

#### Features
* Added new **StatisticsFilter** to the `jpro-routing-core` module.
* Java version compatibility for the Openlink module is now set to `11`.

#### Bugfixes
* Fixed routing on the desktop platform.