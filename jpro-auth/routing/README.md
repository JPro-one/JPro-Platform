# JPro Auth Routing

`jpro-auth-routing` combines [`jpro-auth-core`](../README.md) with
[`jpro-routing`](../../jpro-routing/README.md), so authentication can be wired into a `RouteApp`
as a single route filter. It adds the login UI, the session storage, and the route filters that
handle the authentication callback.

## Dependency

```groovy
dependencies {
    implementation("one.jpro.platform:jpro-auth-routing:0.7.1")
}
```

`jpro-auth-core` is pulled in transitively, so you don't need to add it explicitly.

## Concepts

| Type | Purpose |
|---|---|
| `UserSession` | Stores the authenticated `User` in the JPro session (as JSON under the key `"user"`). |
| `AuthUIProvider` | An authentication method: a UI node (`createAuthenticationNode()`) plus a route filter (`createFilter()`). |
| `AuthUIProviders` | Factories for ready-made providers (`createGoogle`, `createOAuth2`, `createBasicProvider`, `combine`). |
| `AuthBasicFilter` | Route filter for username/password authentication. |
| `AuthBasicOAuth2Filter` | Route filter for OAuth2 / OpenID authentication. |
| `AuthRestrictionFilter` | Makes an entire route subtree accessible only to authenticated users. |
| `GoogleLoginButton` | A `Button` pre-styled with Google's "Sign in with Google" branding. |

## UserSession

`UserSession` wraps the routing session map and is the single source of truth for "who is logged
in". Create it once in `createRoute()` from the `SessionManager`, choosing the browser or desktop
session depending on the runtime:

```java
var session = WebAPI.isBrowser() ? sessionManager.getSession(getWebAPI())
                                 : sessionManager.getSession("user-session");
UserSession userSession = new UserSession(session);

userSession.setUser(user);   // store after a successful login
userSession.getUser();       // null when not logged in
userSession.isLoggedIn();    // convenience for getUser() != null
userSession.logout();        // clears the stored user
```

The filters below call `setUser(...)` for you; you typically only call `getUser()`/`isLoggedIn()`
when building routes and `logout()` from a sign-out action.

## Login UI: AuthUIProviders

An `AuthUIProvider` bundles the login UI with the matching filter. Use the factories:

```java
// Google "Sign in with Google" button
AuthUIProvider google = AuthUIProviders.createGoogle(openidAuthProvider, userSession);

// Generic OAuth2/OpenID, default "Login" button
AuthUIProvider oauth2 = AuthUIProviders.createOAuth2(openidAuthProvider, userSession);

// Generic OAuth2/OpenID with a custom button
AuthUIProvider oauth2custom =
        AuthUIProviders.createOAuth2(openidAuthProvider, userSession, () -> new Button("Sign in"));

// Username/password form (text fields + login button)
AuthUIProvider basic = AuthUIProviders.createBasicProvider(basicAuthProvider, userSession);

// Offer several methods on one page
AuthUIProvider combined = AuthUIProviders.combine(google, basic);
```

Render the UI with `provider.createAuthenticationNode()`.

> Note: `createBasicProvider` performs the login inside its UI node (it calls `authenticate` and
> `userSession.setUser(...)` directly), so its `createFilter()` returns `Transformer.empty()`. The
> OAuth2 providers, by contrast, rely on a redirect callback and therefore need their filter
> applied to the route (see below).

## Route filters

### AuthBasicFilter

```java
static Transformer create(BasicAuthenticationProvider authProvider,
                          UsernamePasswordCredentials credentials,
                          Function<User, Response> onSuccess,
                          Function<Throwable, Response> onError)

static void authorize(Node node, BasicAuthenticationProvider basicAuthProvider)
```

`create(...)` returns a filter that triggers when the request path equals the provider's
`getAuthorizationPath()`. It authenticates the `credentials`, then runs `onSuccess`/`onError`.
`authorize(node, provider)` navigates to that authorization path (e.g. from a button action).

### AuthBasicOAuth2Filter

```java
// 1. OpenID provider, no session storage
static Transformer create(OpenIDAuthenticationProvider openidAuthProvider,
                          Function<User, Response> onSuccess,
                          Function<Throwable, Response> onError)

// 2. OpenID provider, store user in session (recommended)
static Transformer create(OpenIDAuthenticationProvider openidAuthProvider,
                          UserSession userSession,
                          Function<User, Response> onSuccess,
                          Function<Throwable, Response> onError)

// 3. Full control: explicit provider, optional session, explicit credentials
static Transformer create(OAuth2AuthenticationProvider authProvider,
                          @Nullable UserSession userSession,
                          OAuth2Credentials credentials,
                          Function<User, Response> onSuccess,
                          Function<Throwable, Response> onError)

// Start the flow from a UI node:
static void authorize(Node node, OpenIDAuthenticationProvider openidAuthProvider)
static void authorize(Node node, OAuth2AuthenticationProvider authProvider, OAuth2Credentials credentials)
```

The `create(...)` filter triggers when the request matches the credentials' `redirectUri`
(via `Request.matchesSoft`). It exchanges the authorization code for tokens, stores the resulting
`User` in the `UserSession` (when one is given), and runs your `onSuccess`/`onError` callback.

`authorize(...)` builds the provider's authorization URL and starts the flow. In the browser JPro
redirects automatically; on the desktop it calls `gotoURL(...)` so the route reaches the redirect
URI. Overloads 1/2 take the provider's pre-configured credentials; overload with explicit
`OAuth2Credentials` is for advanced cases.

### AuthRestrictionFilter

```java
static Transformer create(AuthUIProvider authProvider, UserSession userSession)
```

Wraps a route so that unauthenticated users see `authProvider.createAuthenticationNode()` instead
of the protected content; authenticated users pass through.

## Example: OAuth2 with Google

```java
public class GoogleLoginApp extends RouteApp {

    private static final SessionManager sessionManager = new SessionManager("google-login-app");
    private UserSession userSession;

    @Override
    public Route createRoute() {
        var session = WebAPI.isBrowser() ? sessionManager.getSession(getWebAPI())
                                         : sessionManager.getSession("user-session");
        userSession = new UserSession(session);

        var googleAuthProvider = AuthAPI.googleAuth()
                .clientId("your-client-id")
                .clientSecret("your-client-secret")
                .redirectUri("/auth/google")
                .create(getStage());

        var uiProvider = AuthUIProviders.createGoogle(googleAuthProvider, userSession);

        return Route.empty()
                .and(Route.get("/", request -> Response.node(uiProvider.createAuthenticationNode())))
                .when(request -> userSession.isLoggedIn(), Route.empty()
                        .and(Route.get("/user/signed-in", request -> Response.node(new SignedInPage(this)))))
                .transform(AuthBasicOAuth2Filter.create(googleAuthProvider, userSession,
                        user -> Response.redirect("/user/signed-in"),
                        error -> Response.node(new ErrorPage(error))));
    }
}
```

You can register several OAuth2 providers (Google, Microsoft, Keycloak) by applying one
`AuthBasicOAuth2Filter.create(...)` transform per provider, each matching its own `redirectUri`.
See the `jpro-auth/example` module for runnable samples.
