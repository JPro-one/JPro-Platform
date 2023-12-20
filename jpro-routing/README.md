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

An additional type is Filter.
A Filter is a function from a Route to a new Route.
This allows interesting features like authentication or analytics.
```
Filter: Route => Route
```

### Advantages

With these basic types, we get a lot of features:

** Composable Routes **
It's possible to combine Routes to create new Routes.
This can be done with `Route.and`, combining two Routes to a new Route.
If the first Route doesn't match, the second Route is tried.
This way we can reuse existing JavaFx Applications/Routes and combine them to a new Application.

** Authentication **
It's simple to add authentication as a filter to your application.
This way it's possible to hide certain pages if the user is not logged in - or doesn't have the right permissions.
Checkout jpro-auth-routing for a OAuth2 implementation.

** Utility Overlays **
It's possible to add utility overlays to your application.
Check out the DevFilter in the module jpro-routing-dev.
For example, the DevFilter and StatisticsFilter are very useful for development.

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
Route.redirect(String from, String to)
Route.get(String path, Route route)
Route.getView(String path, View route)
```

Methods of Route:
```
route1.and(route2)
route1.when(Request->Boolean condition, Request->Route)
route1.whenFuture(Request->FXFuture<Boolean> condition, Request->Route)
route1.filter(Filter filter)
route1.filterWhen(Request->Boolean condition, Filter filter)
route1.filterWhenFuture(Request->FXFuture<Boolean> condition, Filter filter)
```

#### Response

Response static methods:
```
Response.empty()
Response.node(Node node)
Response.view(View view)
Response.redirect(String path)
```

#### Request

Methods of Request:
```
String request.getPath()
String request.getDomain()
int request.getPort()
String request.getQueryParameter(String name)
Map<String,String> request.getQueryParameters()
```
