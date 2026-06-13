## JPro Routing

### Introduction

JPro Routing is a framework
to write JavaFX Applications.

JPro Routing is suited for both Desktop and Web Applications!

With JPro Routing, your application is split into several Pages.
Based on a Request, a Response like a Page is chosen.

The function from a Request to a Page is called a Route.
The request contains information like the path, the query parameters, and the domain.

The Response can be a Page or a Redirect.
Additionally, a Response can be empty or represent a value in the future.

These are the basic types in Pseudo Code:
```
Route: Request => Response
Request: Request(path, query parameters, domain)
Response: Page | Redirect | Empty
```

An additional type is Transformer.
A Transformer is a function from a Route to a new Route.
This allows interesting features like authentication or analytics.
```
Transformer: Route => Route
```

### Advantages

With these basic types, we get a lot of features:

** Composable Routes **
It's possible to combine Routes to create new Routes.
This can be done with `Route.and`, combining two Routes to a new Route.
If the first Route doesn't match, the second Route is tried.
This way we can reuse existing JavaFx Applications/Routes and combine them to a new Application.

** Authentication **
It's simple to add authentication as a transformer to your application.
This way it's possible to hide certain pages if the user is not logged in - or doesn't have the right permissions.
Checkout jpro-auth-routing for a OAuth2 implementation.

** Utility Overlays **
It's possible to add utility overlays to your application.
Check out the DevTransformer in the module jpro-routing-dev.
For example, the DevTransformer and StatisticsTransformer are very useful for development.

### Hello World

Checkout our sample project for a small application showing the features of JPro Routing:
https://github.com/JPro-one/jpro-routing-sample

This is a minimal Hello World Application:

```
public class HelloRoute extends RouteApp {

    @Override
    public Route createRoute() {
        return Route.empty()
            .and(Route.redirect("/", "/hello"))
            .and(Route.get("/hello", (r) -> Response.node(new Label("Hello Routing!"))));
    }
}
```


### Basic API

#### Route

Route static methods:
```
Route.empty()
Route.redirect(String path, String to)
Route.get(String path, Request -> Response handler)
```

Methods of Route:
```
route1.and(route2)
route1.path(String prefix, Route route)              // match a path prefix; the prefix is stripped for the nested route
route1.domain(String domain, Route route)
route1.when(Request -> Boolean condition, Route then)
route1.when(Request -> Boolean condition, Route then, Route else)
route1.whenFuture(Request -> FXFuture<Boolean> condition, Route then)
route1.whenFuture(Request -> FXFuture<Boolean> condition, Route then, Route else)
route1.transform(Transformer transformer)
route1.transformWhen(Request -> Boolean condition, Request -> Transformer transformer)
route1.transformWhenFuture(Request -> Boolean condition, Request -> FXFuture<Transformer> transformer)
```

#### Response

Response static methods:
```
Response.empty()
Response.node(Node node)
Response.page(Page page)
Response.redirect(String to)
Response.error(Exception ex)
Response.fromFuture(FXFuture<Response> future)
Response.fromResult(ResponseResult result)
```

#### Request

Methods of Request:
```
String           request.getPath()
String           request.getDomain()
String           request.getProtocol()
int              request.getPort()
Optional<String> request.getQueryParameter(String name)     // Scala Option
String           request.getQueryParameterOrElse(String name, String default)
Map<String,String> request.getQueryParameters()
```

### Setting Links

You usually want to use a link to switch from one page to another.
You can set a link on a Node with the methods in LinkUtil.

```
LinkUtil.setLink(Node node, String url)
LinkUtil.setLink(Node node, String url, String description)
LinkUtil.setExternalLink(Node node, String url)              // opens in a new tab / external browser
```

It's also possible to change the current link with code.

When you have a Node, you can use the following method to change the current link:
```
LinkUtil.gotoPage(Node node, String url)
```

You can also access the SessionManager and change the current link.
The session manager can be accessed with the following method:
```
RouteApp.getSessionManager()
LinkUtil.getSessionManager(node)
sessionmanager.gotoURL(String)
```

Be aware that the methods using the Node only work if the Node is part of
the SceneGraph of the corresponding application.

It is recommended to use `LinkUtil.setLink` when possible because of the following reasons:
1. It supports SEO for webpages
2. The user gets automatic feedback when hovering over the link


### Fullscreen and Scrolling

It is possible to configure a page to be either fullscreen or scrollable.
If it's fullscreen, the whole available area is always used - but the content might be cut off.
If it's scrollable, the content is never cut off - but you usually get a scrollbar.

On the browser, scrollable uses the native scrolling of the browser.

This can be configured with the following methods
1. Override the method `Page.fullscreen()` in your Page
2. Use the transformer `Transformers.fullscreen(boolean)` to set it for a Route


### History API and defaultpage
When Routing is used in a browser, the history API is used to navigate between pages.
This has the effect of not reloading the application when navigating between pages.
This improves the performance and user experience and ensures the same behavior as a native application.

Currently, it's required to set a resource called `jpro/html/defaultpage` in the resources -
checkout our sample project: https://github.com/JPro-one/jpro-routing-sample



### Pages

`Response.node(...)` is the quickest way to show something — but wrapping content in a `Page`
gives it metadata and lifecycle:

```java
public class HomePage extends Page {
    @Override public String title() { return "Home"; }
    @Override public String description() { return "The landing page of this application."; }
    @Override public Node content() { return new Label("Welcome!"); }
}
// in the route:
Route.get("/", r -> Response.page(new HomePage()))
```

- `title` and `description` are used for the browser tab and for SEO (and by the AppCrawler, see below)
- `fullscreen()` — override to control fullscreen vs scrollable per page
- `onClose()` — called when the user navigates away from the page
- `handleRequest(Request)` — return true to handle URL changes inside the page yourself
  (e.g. for tab navigation within one page, without recreating it)

### Built-in Transformers

A `Transformer` wraps a `Route` and can decorate every page. The library ships several:

```java
route
    .transform(Transformers.fullscreen(true))                  // force fullscreen
    .transform(Transformers.titleAndDescription("My App", "..."))    // default title/description
    .transform(Transformers.stylesheets(new String[]{"/css/main.css"})) // wrap pages with stylesheets
    .transform(Transformers.styleClasses(new String[]{"dark-theme"}))   // wrap pages with style classes
    .transform(Transformers.errorPage())                             // show exceptions instead of failing
    .transform(Transformers.notFoundPage())                          // fallback page for unmatched paths
    .transform(RouteUtils.transition(0.5))                // fade between pages
    .transform(RouteUtils.sideTransition(0.5))            // slide between pages
```

- `Transformers.errorPage((request, exception) -> Response...)` customizes error rendering.
- `Transformers.notFoundPage(route)` takes any `Route` as the fallback.
- `Transformers.stylesheets`/`styleClasses` also accept an `ObservableList` — mutate it at
  runtime (e.g. switching a theme, or mobile vs desktop) and the pages update immediately.

#### Custom Containers

To wrap every page in your own layout (navigation menu, header, footer), implement the
`Container` interface (a Node with a `contentProperty` and a `requestProperty`) and apply it with:

```java
route.transform(ContainerTransformer.fromContainer(() -> new MyMenuContainer()));
```

See `SimpleHamburgerMenu` in the example project for a responsive menu implemented this way.

### Popups

The module `one.jpro.platform:jpro-routing-popup` provides popups that work in routed
applications, on desktop and in the browser:

```java
// once, at the end of the route definition:
route.transform(PopupAPI.createPopupContainerTransformer())

// anywhere in the application (node = any node in the scene graph):
PopupAPI.openPopup(node, SimplePopups.infoPopup("Info", "Saved successfully."));
PopupAPI.closePopup(anyNodeInsidePopup);

// show a loading overlay until a future completes:
PopupAPI.showLoadingScreen(node, someFXFuture);
```

`SimplePopup` is a prebuilt popup with title, content and buttons; `SimplePopups` contains
factories for common cases.

### Development Tools

The module `one.jpro.platform:jpro-routing-dev` provides overlays for development:

```java
route
    .transform(DevTransformer.create())          // dev toolbar
    .transform(StatisticsTransformer.create())   // performance statistics (browser)
```

- **DevTransformer** — a toolbar with back/forward/refresh, an editable URL bar, a ScenicView
  launcher, a "pages uncollected" memory counter, and a force-GC button.
- **StatisticsTransformer** — live statistics when running in the browser: latency, bytes
  sent/received, synchronized/created/collected nodes, and load times.

Remove these transformers for production builds (or apply them conditionally).

### Additional Features

#### Incremental Loading

It's possible to load parts of the application incrementally.
This is especially useful when optimizing the start time for websites.

```
import one.jpro.platform.routing.performance.IncrementalLoading;

...
parent.getChildren().add(IncrementalLoading.loadNode(yourNode));
...
```

When this is done - JPro sends one Node at a time to the client.
This allows the client to render the frame as soon as possible - ensuring early visible content for the user.

### Server-Side Features (crawling, SEO, sitemaps, HTTP)

Because a `Route` is a plain function, the whole application can be traversed without a browser.
This enables a set of server-side and tooling features — `AppCrawler` (page/link/image
discovery), `SitemapGenerator`, `MemoryTester` (leak-testing every page from a unit test),
`SizeTester`, and `RouteHTTP` (serving the app and its `/sitemap.xml`).

> **Note:** these features are still evolving and not yet documented in detail. See the
> `crawl` and `server` packages and the `core-test` module for current usage.
