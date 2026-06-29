# JPro Auth Routing

Add Google, OAuth2 or username/password login to a JavaFX/JPro `RouteApp` in a few lines, through
one central, swappable configuration: **`RoutingAuth`**.

```groovy
implementation("one.jpro.platform:jpro-auth-routing:0.7.2")
```

## All you need

```java
public class MyApp extends RouteApp {
    private RoutingAuth auth;

    @Override
    public Route createRoute() {
        auth = RoutingAuth.config()
                .google(GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET)   // add login methods
                .usernamePassword(userManager)
                .loginRedirect("/home")
                .build(this);

        return Route.empty()
                .and(Route.get("/", r -> Response.node(new PublicHome())))        // public
                .and(Route.get("/home", r -> Response.node(new Home(auth.getUser())))
                        .transform(auth.requireLogin()))                          // protected
                .transform(auth.install());   // serves /login + handles login callbacks
    }
}
```

One method per login option, `requireLogin()` on what to protect, `install()` on the whole route —
and `/login` is served for you. That's the whole integration; everything below is just variations.

> Build inside `createRoute()` (OAuth2 needs the started `Stage`), and keep `auth` in a field so
> your pages can read it.

## Recipes

**Mix public and protected** — place guarded routes *after* public ones (`and` tries earlier routes first):
```java
Route.empty().and(publicRoutes).and(secret.transform(auth.requireLogin())).transform(auth.install());
```

**Customize the login page** — `loginResponse` decides what `/login` shows; reuse the built-in buttons via `auth.loginScreen()`:
```java
.loginResponse(r -> Response.node(new VBox(new Label("Sign in"), auth.loginScreen())))
```

**Fake login for tests, or a desktop local user:**
```java
.dummy("tester", Set.of("USER"))            // one-click fake-login button
.defaultUser("localuser", Set.of("USER"))   // already signed in, no UI
```

**Current user:** `auth.getUser()`, `auth.isLoggedIn()`, `auth.logout()`.

## Options

Configure with `RoutingAuth.config()`:

| | |
|---|---|
| `.google(id, secret)` | "Sign in with Google" (add `redirectUri` as a 3rd arg to override) |
| `.oauth2(provider)` | a generic OAuth2/OpenID provider you configured |
| `.usernamePassword(userManager, roles…)` | a username/password form |
| `.dummy(name, roles)` | one-click fake login — for testing |
| `.defaultUser(name, roles)` · `.defaultUser(user)` | auto-login as a fixed user, no UI |
| `.loginUrl(path)` | login page path — default `/login` |
| `.loginResponse(r -> Response)` | what `/login` shows — default: the combined login screen |
| `.loginRedirect(path)` | where to go after a successful login — default `/` |
| `.sessionName(name)` | session-storage namespace — default `app` |
| `.onLogin(user -> void)` | hook run after an interactive login |
| `.onError(err -> Response)` | how to render a failed OAuth2 login |
| `.build(app)` | materialize the config (call inside `createRoute()`) |

The resulting `RoutingAuth`:

| | |
|---|---|
| `install()` | transform for the **whole** route — serves `/login` and handles login callbacks |
| `requireLogin()` | transform for a route/sub-route — protects it (redirects to the login page) |
| `loginScreen()` | the combined login UI node, for embedding in a custom login page |
| `getUser()` · `isLoggedIn()` · `logout()` | current user state |
| `userSession()` | the underlying `UserSession` |

## Under the hood

`RoutingAuth` is a thin facade over `UserSession`, `AuthUIProviders`
(`createGoogle` / `createOAuth2` / `createBasicProvider` / `dummy` / `combine`) and the filters
`AuthBasicTransformer`, `AuthBasicOAuth2Transformer`, `AuthRestrictionTransformer`. Use those directly for finer
control — see the Javadoc and the [`jpro-auth-core` README](../README.md).

## Try it

```bash
./gradlew jpro-auth:example:run -Psample=routing-auth
```

A self-contained desktop demo (no network): a public home and a protected `/secret`, with a dummy
one-click login and a username/password form (`admin` / `password`).
