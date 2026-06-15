# JPro Playwright

Utilities and conventions for writing [Playwright](https://playwright.dev/java/) end-to-end tests against JPro/JavaFX applications running in a real browser.

A JPro app renders JavaFX into the browser and relays events back to the JVM over a WebSocket. Because of that round-trip — and because JavaFX controls are not native DOM elements — a few things (especially keyboard input) need a specific recipe. This module packages the recipes that work and documents the rules.

> **Status: experimental.** The helper API may change between releases. The conventions documented here are the durable part — they hold whether you use these helpers or drive Playwright directly.

## Dependency

A test-only library (it brings Playwright and JUnit 5 transitively).

Gradle:

```groovy
dependencies {
    testImplementation("one.jpro.platform:jpro-playwright:0.7.1")
}
```

Maven:

```xml
<dependency>
    <groupId>one.jpro.platform</groupId>
    <artifactId>jpro-playwright</artifactId>
    <version>0.7.1</version>
    <scope>test</scope>
</dependency>
```

## Quick start

Extend `JProPlaywrightTest`, start the app's JPro server, and drive it with Playwright.

```java
public class MyAppTest extends JProPlaywrightTest {

    static final File LOGS = new File(projectRoot(), "my-app/logs");

    @BeforeAll
    static void start() throws Exception {
        startServer(LOGS, gradleCommand(":my-app:jproStart"));
    }

    @AfterAll
    static void stop() throws Exception {
        stopAndAssertNoServerErrors(LOGS, gradleCommand(":my-app:jproStop"));
    }

    Page page;

    @BeforeEach
    void open() {
        page = newPage();
        page.navigate(BASE_URL + "/textinput");
        waitForRunning(page);            // wait until jpro-app data-status="running"
    }

    @AfterEach
    void close() {
        page.context().close();          // close the context, not just the page
    }

    @Test
    void typing() {
        JProInput.typeInto(page, "#jpro-textfield", "hello");
        JProInput.awaitText(page, "#jpro-echo", "hello");
    }
}
```

This module's own tests in `jpro-playwright/src/test` run against the `jpro-playwright:example` app and are the canonical worked examples.

## Selecting nodes: the `jpro-` prefix and mirrorCSSToDOM

A JavaFX `id`/`styleClass` is selectable only with DOM mirroring on, and always carries a `jpro-` prefix.

1. **Enable mirroring.** Add `jpro.mirrorCSSToDOM = true` to the app's `jpro.conf` (on the classpath). It is **off by default** — without it, `id`/`styleClass` are never sent to the browser and selectors match nothing.
2. **Use the `jpro-` prefix.** `setId("textfield")` → `"#jpro-textfield"`; style class `foo` → `".jpro-foo"`.

If a selector matches nothing, check these two first.

## Keyboard input

Use `JProInput` — it applies the sequence that works against a JPro control.

```java
JProInput.typeInto(page, "#jpro-field", "hello");   // focus (click), settle, then type
JProInput.awaitText(page, "#jpro-echo", "hello");   // poll for the round-trip result
```

The rules behind it, all observed against the example app:

- **Type with the real keyboard, after focusing.** `typeInto` clicks the field, waits briefly, then types via `page.keyboard()`.
- **`Locator.fill()` does not work.** Playwright rejects it: *"Element is not an `<input>`, `<textarea>`, `<select>` or `[contenteditable]`"* — a JPro control is none of those.
- **Don't assert immediately; poll.** Each keystroke round-trips to the JVM and back, so use `awaitText` (or Playwright's auto-retrying `assertThat(locator).hasText(...)`).

**Commit (Enter):** JavaFX `onAction` handlers and `TextFormatter` converters fire on Enter, not per keystroke:

```java
JProInput.typeInto(page, "#jpro-field", "world");
JProInput.commit(page);                              // Enter
JProInput.awaitText(page, "#jpro-result", "WORLD");
```

**Special keys:** `JProInput.focus(page, selector)` then `JProInput.press(page, "Backspace" | "ArrowLeft" | "Control+A" | ...)`.

## Waiting for round-trips

Every interaction is asynchronous, so wait rather than read once.

- `waitForRunning(page)` — block until `jpro-app` reports `data-status="running"`; call after every `navigate`.
- `JProInput.awaitText(page, selector, expected)` — poll until the text matches (≈ Playwright's `assertThat(locator).hasText(expected)`).
- `PlaywrightTimeouts.withTimeout(budget, unit, label, op)` — wrap a Playwright wait in a 4-tier diagnostic budget (silent <50%, warn >50%, "too slow" >100%, "never completed" >200%; the op gets 2× the budget).

To assert something *didn't* happen (a label stayed empty), don't read once — that races the round-trip. Establish happens-before first: trigger an ordered probe (e.g. a click whose counter you `awaitText`); since the WebSocket is ordered, once the probe lands the earlier action was processed too.

## Server lifecycle

`startServer`/`stopServer` run an arbitrary command, so they're build-tool agnostic; the run fails on any server-side error.

- `startServer(logsDir, command...)` runs `command` from the project root, clears `logsDir`, and blocks until `/status/alive` returns 200. `PORT` comes from `-Djpro.test.port` or a free port in 9100–9200; `BASE_URL` derives from it.
- `stopAndAssertNoServerErrors(logsDir, command...)` stops it, dumps the logs (visible on CI even on failure), then fails if any log line looks like an exception (`ServerLogAssertions`).

`gradleCommand(":my-app:jproStart")` builds `./gradlew :my-app:jproStart -Pjpro.test.port=<PORT>`. The app's Gradle module wires the port to the JPro plugin:

```groovy
jpro {
    port = project.hasProperty('jpro.test.port') ? project.property('jpro.test.port') as int : 8080
    openURLOnStartup = false
}
```

For Maven or anything else, pass your own command (include `-Djpro.test.port`):

```java
startServer(LOGS, "mvn", "-pl", "my-app", "jpro:run", "-Djpro.test.port=" + PORT);
```

## Catching browser-side errors

`BrowserErrorCollector` turns silent JS failures into test failures. Construct it right after the page; it records `console.error`, uncaught exceptions, and network failures.

```java
BrowserErrorCollector errors = new BrowserErrorCollector(page);
// ... drive the test ...
errors.assertNoErrors();   // in teardown
```

## Taking screenshots

Tests run headless, so a screenshot is often the only way to see the app. `JProPlaywrightTest` writes a PNG and logs its absolute path:

```java
screenshot(page, "after-typing");                       // full app
screenshot(page.locator("#jpro-textfield"), "field");   // one element, by id
```

Files go to `build/playwright-screenshots/` (override with `-Djpro.test.screenshotDir=...`); both return the path. Most useful in a `catch`/teardown to snapshot a failure.

## Installing the browser

Playwright drives a real Chromium that must be installed once per project. Add an install task and make `test` depend on it, so CI provisions the browser with no separate step:

```groovy
tasks.register('installPlaywright', JavaExec) {
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'com.microsoft.playwright.CLI'
    args = ['install', '--with-deps', 'chromium']   // --with-deps installs Linux system libs (CI)
}

test {
    dependsOn 'installPlaywright'
    environment 'PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD', '1'   // already provisioned; don't re-download
}
```

Pass `-Djpro.test.headless=false` to watch the browser (read by `JProPlaywrightTest`).

## Gotchas

Mistakes that compile fine and then fail without pointing at the cause — scan here first.

- **`Locator.fill()` to enter text** → Playwright throws "Element is not an `<input>`/…"; a JPro control is not a DOM input. Use `JProInput.typeInto(...)`.
- **Typing right after clicking** → dropped/missing characters. `typeInto` focuses and waits first.
- **Asserting right after acting** → reads the stale value. Poll with `awaitText(...)`.
- **No `jpro.mirrorCSSToDOM = true`** → every selector matches nothing (it's off by default). Add it to `jpro.conf`.
- **Missing the `jpro-` prefix** → `#textfield` matches nothing; use `#jpro-textfield`.
- **No `jpro/html/defaultpage`** → routes return a static-file 404 ("Couldn't get resource for: 'jpro/html/…'") and the app never boots. Add `src/main/resources/jpro/html/defaultpage` with a `<jpro-app href="/app/default" .../>` (see the `example` module).
- **Expecting `onAction`/`TextFormatter` per keystroke** → they fire on Enter (JavaFX commit). Call `JProInput.commit(page)` first.
- **Interacting before `waitForRunning(page)`** → events land on a half-loaded app and are lost.
- **Wrong `logsDir`** → "No 'Server started' marker found" on a clean run. Point it at the served module's `logs/`.
- **Chromium not installed** → launch fails at startup. Run the `installPlaywright` task once.
