# JPro Utils

`jpro-utils` is a small collection of utilities for JPro/JavaFX applications that behave correctly
both on the desktop and in the browser.

## Dependency

```groovy
dependencies {
    implementation("one.jpro.platform:jpro-utils:0.7.2")
}
```

## What's inside

### PlatformUtils

Static platform checks for the desktop runtime:

```java
PlatformUtils.isDesktop();   // running on the JVM desktop (not in the browser)
PlatformUtils.isWindows();
PlatformUtils.isMac();
PlatformUtils.isLinux();
PlatformUtils.isAndroid();
PlatformUtils.isIOS();
PlatformUtils.isAarch64();
```

### UserPlatform

The end-user's OS, which might differ from the OS of the JPro server. A common use case is to detect
the correct modifier key (Cmd on macOS, Ctrl elsewhere):

```java
UserPlatform.isWindows(webAPI);
UserPlatform.isMac(webAPI);
UserPlatform.isMobile(webAPI);
KeyCode modifier = UserPlatform.getModifierKey(webAPI);

// Testing helpers:
UserPlatform.simulateNative();
UserPlatform.simulateWeb(platform, platformOld);
```

### OpenLink

Opens a URL in the user's default browser (desktop only; throws on unsupported platforms). In the
browser, use the routing/link APIs instead.

```java
OpenLink.openURL("https://www.jpro.one");
OpenLink.openURL(new URL("https://www.jpro.one"));
```

### CopyUtil

Makes a node copy text to the clipboard when clicked; works on desktop and in JPro.

```java
CopyUtil.setCopyOnClick(myLabel, "text to copy");
```

### TreeShowing

A `BooleanProperty` that reflects whether a node is currently attached to a showing scene/window —
handy for starting/stopping work tied to visibility and for avoiding leaks.

```java
BooleanProperty showing = TreeShowing.treeShowing(node);
boolean isShowing = TreeShowing.isTreeShowing(node);
```

### FreezeDetector

Detects when the JavaFX Application Thread is blocked for longer than a threshold — useful for
spotting deadlocks and long-running work on the FX thread. Must be constructed on the FX thread.

```java
new FreezeDetector(Duration.ofSeconds(1), (thread, frozenFor) ->
        System.out.println("FX thread frozen for " + frozenFor));
```

### CommandRunner

A thin wrapper around `ProcessBuilder` for running external processes and collecting their output,
with optional interactive mode, console echo, and masked secret arguments.

```java
CommandRunner runner = new CommandRunner("git", "status");
int exitCode = runner.run("git-status");   // see Javadoc for run/runAsync variants
String output = runner.getResponse();
```
