# JPro Auth Routing

`jpro-auth-routing` combines [`jpro-auth-core`](../README.md) with
[`jpro-routing`](../../jpro-routing/README.md) so authentication can be wired into a `RouteApp`
with very little code.

The recommended entry point is **`RoutingAuth`** — a single, central configuration that owns the
user session, builds the login UI, serves the login page, and produces the route filters. The
lower-level building blocks it is built on (`UserSession`, `AuthUIProviders`, the filters) are
documented further down for advanced use.

## Dependency

```groovy
dependencies {
    implementation("one.jpro.platform:jpro-auth-routing:0.7.1")
}
```

`jpro-auth-core` is pulled in transitively.

## Quick start

Declare the login methods you want, bind to the app, and apply two transforms to your route:

```java
public class MyApp extends RouteApp {

    private RoutingAuth auth;

    @Override
    public Route createRoute() {
        auth = RoutingAuth.config()
                .google(GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET)   // add login methods
                .usernamePassword(userManager)
                .loginRedirect("/home")                           // where to land after login
                .build(this);                                     // call inside createRoute()

        return Route.empty()
                .and(Route.get("/", request -> Response.node(new PublicHome())))      // public
                .and(Route.empty()
                        .and(Route.get("/home", request -> Response.node(new Home(auth.getUser()))))
                        .transform(auth.requireLogin()))                              // protected
                .transform(auth.filter());   // serves /login and handles login callbacks
    }
}
```

That's the whole integration: one method per login option, `requireLogin()` on what you want
protected, and `filter()` on the whole route.

> **Why `auth` is a field and `build(...)` is called inside `createRoute()`:** OAuth2 providers
> need the JavaFX `Stage`, which only exists once the app has started — so build the configuration
> in `createRoute()`. Keeping `auth` in a field lets pages and `loginResponse` reach it.

## Login methods

| Method | Adds |
|---|---|
| `.google(clientId, clientSecret)` | "Sign in with Google" (OAuth2/OpenID) |
| `.google(clientId, clientSecret, redirectUri)` | as above, with an explicit redirect URI |
| `.oauth2(OpenIDAuthenticationProvider)` | a generic OAuth2/OpenID provider you configured |
| `.usernamePassword(UserManager, roles...)` | a username/password form |
| `.dummy(name, roles)` | a one-click fake login — for local testing |
| `.defaultUser(name, roles)` / `.defaultUser(User)` | auto-login as a fixed user, no UI |

You can add several; their UIs are stacked on the login screen.

## Public vs. protected pages

`requireLogin()` is a route filter you apply to whatever you want to protect. When the user is
logged in the wrapped route is used unchanged; otherwise the request is redirected to the login
page (and the protected page is never built while logged out).

- **Gate the whole app:** apply it to the entire route.
- **Mix public and protected:** put the guarded sub-route *after* your public routes — because
  `and` tries earlier routes first, the guard only ever sees requests the public routes didn't
  claim.

```java
return Route.empty()
        .and(publicRoutes)                                    // matched first → stay public
        .and(protectedRoutes.transform(auth.requireLogin()))  // gated, placed after
        .transform(auth.filter());
```

## The login page

`auth.filter()` serves the login page automatically at `loginUrl` (default `/login`) — you do
**not** need to declare a `/login` route. This makes adding auth to an existing app nearly free.

Customize it as needed:

| Config | Effect |
|---|---|
| `.loginUrl("/signin")` | move the login page (the gate redirects here) |
| `.loginResponse(r -> Response.node(new MyLoginPage()))` | render your own login page |
| `.loginRedirect("/home")` | where to navigate after a successful login |

A custom login page can still reuse the built-in provider buttons via `auth.loginScreen()`:

```java
auth = RoutingAuth.config()
        .google(CLIENT_ID, CLIENT_SECRET)
        .loginResponse(r -> Response.node(new VBox(new Label("Please sign in"), auth.loginScreen())))
        .build(this);
```

## Testing & desktop "local user"

Swap the configuration to log in without a real identity provider — handy for local development,
automated tests, or a desktop build that should always run as a local user:

```java
// one-click fake login button:
RoutingAuth.config().dummy("tester", Set.of("USER")).build(this);

// already signed in, no UI (e.g. desktop local user, real login on the web):
var config = RoutingAuth.config();
if (PlatformUtils.isDesktop()) config.defaultUser("localuser", Set.of("USER"));
else                           config.google(CLIENT_ID, CLIENT_SECRET);
RoutingAuth auth = config.build(this);
```

## RoutingAuth reference

Configuration (`RoutingAuth.config()` → `Builder`):

| Method | Default | Purpose |
|---|---|---|
| `google` / `oauth2` / `usernamePassword` / `dummy` / `defaultUser` | — | add a login method |
| `sessionName(String)` | `"app"` | namespaces the session storage |
| `loginUrl(String)` | `"/login"` | login page path |
| `loginResponse(Request → Response)` | combined login screen | what to show at the login page |
| `loginRedirect(String)` | `"/"` | where to go after a successful login |
| `onLogin(Consumer<User>)` | no-op | hook run after an interactive login (OAuth2 / username-password) |
| `onError(Throwable → Response)` | message node | how to render a failed OAuth2 login |
| `build(RouteApp)` | — | materialize the configuration |

Result (`RoutingAuth`):

| Method | Returns |
|---|---|
| `filter()` | the transform to apply to the whole route (serves the login page + handles callbacks) |
| `requireLogin()` | the transform that protects the route/sub-route it wraps |
| `loginScreen()` | the combined login UI node |
| `getUser()` / `isLoggedIn()` / `logout()` | current user state |
| `userSession()` | the underlying `UserSession` |

## Lower-level building blocks

`RoutingAuth` is a thin facade over these; use them directly only when you need finer control.

- **`UserSession`** — stores the authenticated `User` in the JPro session (as JSON under the key
  `"user"`). `getUser()` / `setUser(user)` / `isLoggedIn()` / `logout()`.
- **`AuthUIProvider`** — an authentication method: a UI node (`createAuthenticationNode()`) plus a
  route filter (`createFilter()`).
- **`AuthUIProviders`** — factories for providers: `createGoogle`, `createOAuth2`,
  `createBasicProvider`, `dummy(user, session)`, and `combine(...)` to merge several.
- **`AuthBasicFilter`** — route filter for username/password authentication
  (`create(provider, credentials, onSuccess, onError)`, `authorize(node, provider)`).
- **`AuthBasicOAuth2Filter`** — route filter for OAuth2/OpenID
  (`create(...)` overloads, `authorize(node, provider[, credentials])`).
- **`AuthRestrictionFilter`** — `create(authProvider, userSession)`: shows the login node in place
  for unauthenticated users (an alternative to `requireLogin()`'s redirect behavior).

See the [`jpro-auth-core` README](../README.md) for providers, credentials and `AuthAPI`.

## Runnable example

```bash
./gradlew jpro-auth:example:run -Psample=routing-auth
```

A self-contained desktop demo (no network): a public home page and a protected `/secret` page,
with a dummy one-click login and a username/password form (`admin` / `password`).
See `jpro-auth/example/.../proto/RoutingAuthExample.java`.
