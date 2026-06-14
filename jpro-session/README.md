# JPro Session

`jpro-session` provides a simple server-side session manager for JPro/JavaFX applications. A session
is an `ObservableMap<String, String>` that persists across requests, keyed by a cookie in the
browser or by an explicit key on the desktop.

## Dependency

```groovy
dependencies {
    implementation("one.jpro.platform:jpro-session:0.7.1")
}
```

## Usage

Create one `SessionManager` per application (the name namespaces the on-disk storage), then obtain
the session — by `WebAPI` in the browser, or by an explicit key on the desktop:

```java
SessionManager sessionManager = new SessionManager("my-app");

ObservableMap<String, String> session = WebAPI.isBrowser()
        ? sessionManager.getSession(getWebAPI())     // browser: keyed by cookie
        : sessionManager.getSession("user-session"); // desktop: explicit key

session.put("user", userJson);
String user = session.get("user");
```

Because the session is observable, you can listen for changes. `getSession(String)` must be called
on the JavaFX Application Thread and throws `SessionException` otherwise.

## API

`SessionManager`
- `SessionManager(String appName)` — store sessions under a per-app default directory.
- `SessionManager(File baseDirectory, String cookieName)` — custom storage directory and cookie name.
- `getSession(WebAPI webAPI)` — the session for the current browser client (cookie-based).
- `getSession(String sessionKey)` — the session for an explicit key (desktop).
- `getFolder()` — the base directory used for session storage.

`SessionException` — thrown when the session directory can't be created or a session is accessed
off the FX thread.

> `jpro-auth-routing`'s `UserSession` is a thin wrapper around such a session map.
