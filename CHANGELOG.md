### 0.2.8

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

### 0.2.6 (September 28, 2023)

#### Improvements
* Updated JPro to version `2023.3.0`.
* Update **SimpleFX** to version `3.2.25`.

#### Features
* Add `deleteCache` script for Windows platform. This script is used to delete the jars related to JPro Platform
  modules from Gradle and Maven locally on Windows.

#### Bugfixes
* Provide a more compatible solution to open URls on Windows platform via the OpenLink internal module.

### 0.2.5 (September 28, 2023)

#### Improvements
* Improved **Null** checks for URLs and logging in the OpenLink internal module.

#### Bugfixes
* ScrollPane hotfix applied in the `jpro-scrollpane` module.

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