# JPro Playwright

Utilities and conventions for writing [Playwright](https://playwright.dev/java/) end-to-end tests against JPro/JavaFX applications running in a real browser.

JPro renders a JavaFX scene graph into the browser and relays events back to the JVM over a WebSocket. That round-trip, plus the fact that JavaFX controls are not native DOM elements, makes a handful of things non-obvious — especially keyboard input. This module captures the working patterns as reusable helpers and documents the rules an agent or developer needs to avoid getting stuck.

## Quick start

A test extends `JProPlaywrightTest`, starts the app's JPro server, and drives it with Playwright.

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
        waitForRunning(page);                 // wait until jpro-app data-status="running"
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

JavaFX ids and style classes are reachable as CSS selectors only when DOM mirroring is enabled, and they are always prefixed with `jpro-`.

Two rules, both easy to miss:

1. **Enable mirroring.** Add `jpro.mirrorCSSToDOM = true` to the app's `jpro.conf` (on the classpath, e.g. `src/main/resources/jpro.conf`). It is **off by default** — without it, JavaFX `id` and `styleClass` are never sent to the browser and your selectors match nothing.
2. **Use the `jpro-` prefix.** A node with `setId("textfield")` renders as DOM `id="jpro-textfield"` → select it with `"#jpro-textfield"`. A style class `foo` renders as `class="jpro-foo"` → select it with `".jpro-foo"`. The prefix is applied unconditionally to every non-empty id/class.

If a selector unexpectedly matches nothing, check these two first — it is almost always one of them.

## Simulating keyboard input

Typing into a JPro control requires a focus-and-settle step and the real keyboard, not `fill()`. Use `JProInput`.

A JavaFX text control rendered by JPro is **not** a native DOM `<input>`. When the control gains focus, JPro wires up a hidden input element in the browser and routes keystrokes from it to the JavaFX node on the server. This is the single biggest source of flaky/stuck tests, so the rules are:

- **Focus first, then wait, then type.** Click the field and wait (~300ms) for the hidden input to be wired up *before* typing. Keystrokes sent during that window are silently dropped.
- **Drive the real keyboard.** Use `page.keyboard().type()` / `press()`. **Do not use `Locator.fill()`** — it sets a DOM value directly, which for a JPro control never reaches JavaFX, so the text silently never appears on the server.
- **Never assert immediately.** Every keystroke is a browser → WebSocket → JVM → re-render → browser round-trip. Poll for the expected value.

`JProInput` bakes all of this in:

```java
// click #jpro-field, wait for the hidden input to settle, type via the real keyboard
JProInput.typeInto(page, "#jpro-field", "hello");

// poll the bound label until the round-trip lands (throws with the last value seen on timeout)
JProInput.awaitText(page, "#jpro-echo", "hello");
```

### Committing input (Enter / focus loss)

JavaFX commits input on Enter or focus loss, not on every keystroke — `onAction` handlers and `TextFormatter` converters run only then.

```java
JProInput.typeInto(page, "#jpro-field", "world");
JProInput.commit(page);                          // presses Enter
JProInput.awaitText(page, "#jpro-result", "WORLD");
```

### Special keys

Use Playwright key syntax via `JProInput.press` (or `page.keyboard().press`): `"Enter"`, `"Backspace"`, `"ArrowLeft"`, `"Control+A"`, etc. Call `JProInput.focus(page, selector)` once to focus-and-settle, then issue individual `press` calls.

## Waiting and round-trips

Because every interaction is asynchronous, the helpers wait and poll rather than assert instantly.

- `waitForRunning(page)` — block until the app's `jpro-app` element reports `data-status="running"`; call it after every `navigate` before interacting.
- `JProInput.awaitText(page, selector, expected)` — poll an element's text until it equals `expected`.
- `PlaywrightTimeouts.withTimeout(budget, unit, label, op)` — wrap any Playwright wait in a 4-tier diagnostic budget: silent under 50%, warns past 50%, fails as "too slow" past 100%, fails as "never completed" past 200% (the op is given 2× the budget).
- `JProServerUtils.waitFor(...)` / `waitForAlive(...)` — the same tiered polling for non-Playwright conditions and server health.

## Server lifecycle

The base class starts and stops a JPro server with an arbitrary command and fails the test on any server-side error — it is build-tool agnostic.

- `startServer(logsDir, command...)` runs `command` from the project root, clears `logsDir` first, and blocks until `/status/alive` returns 200. `PORT` comes from `-Djpro.test.port` or a free port in 9100–9200; `BASE_URL` is derived from it.
- `stopServer(command...)` stops it.
- `stopAndAssertNoServerErrors(logsDir, stopCommand...)` stops it, dumps the logs (so they appear on CI even on failure), then fails if any log line looks like an exception/error (`ServerLogAssertions`).

Build the command with the helpers, which inject the port for you:

- **Gradle** — `gradleCommand(":my-app:jproStart")` → `./gradlew :my-app:jproStart -Pjpro.test.port=<PORT>`. The app's module wires the port to the JPro plugin:
  ```groovy
  jpro {
      port = project.hasProperty('jpro.test.port') ? project.property('jpro.test.port') as int : 8080
      openURLOnStartup = false
  }
  ```
- **Maven** — `mavenCommand("-pl", "my-app", "jpro:run")` → `mvn ... -Djpro.test.port=<PORT>` (using `mvnw` if present). The POM binds the JPro plugin's port to `${jpro.test.port}`.

Or pass any command yourself: `startServer(logsDir, "docker", "compose", "up", ...)` — whatever brings the server up on `PORT`.

## Catching browser-side errors

`BrowserErrorCollector` turns silent JS failures into test failures.

Construct it right after creating the page; it records `console.error`, uncaught exceptions, and network failures. Call `assertNoErrors()` in teardown, optionally after `ignoreMatching(...)` to filter known-benign noise.

```java
BrowserErrorCollector errors = new BrowserErrorCollector(page);
// ... drive the test ...
errors.assertNoErrors();
```

## Running the tests

The Playwright tests drive a real Chromium, which must be installed once.

```bash
./gradlew :jpro-playwright:installPlaywright   # one-time: downloads Chromium + system deps
./gradlew :jpro-playwright:test                # starts the example app and runs the tests
```

The test JVM does not auto-download browsers (`PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1`); run `installPlaywright` first. Pass `-Djpro.test.headless=false` to watch the browser.

## Gotchas and pitfalls

The mistakes that are easy to make without knowing how JPro renders — each with the symptom it produces and the fix.

These are the things that look correct, compile fine, and then fail in a way that doesn't point at the cause. If a test misbehaves, scan this list first.

- **Using `Locator.fill()` / `Locator.type()` to enter text.** *Symptom:* the field looks filled in a screenshot but the server never sees the text; bound labels and `onAction` never fire. *Why:* `fill()` sets the DOM value of the hidden input directly — it bypasses the keystroke path to JavaFX. *Fix:* use `JProInput.typeInto(...)` (or `page.keyboard().type(...)`).

- **Typing immediately after clicking a field.** *Symptom:* the first characters are missing, or nothing is typed at all; flaky between runs. *Why:* focusing a JPro control asynchronously wires up a hidden input over the WebSocket; keystrokes sent before it settles are dropped. *Fix:* `JProInput.typeInto(...)` waits ~300ms after focusing; if still flaky on a loaded machine, raise the settle delay.

- **Asserting on a value right after acting.** *Symptom:* assertion reads the old value and fails, but the same check passes when you add a `Thread.sleep`. *Why:* every interaction is a browser → server → re-render → browser round-trip with variable latency. *Fix:* poll with `JProInput.awaitText(...)` instead of reading once.

- **Forgetting `jpro.mirrorCSSToDOM = true`.** *Symptom:* every selector matches nothing; `waitForRunning` and locators time out even though the app clearly works in the browser. *Why:* mirroring is **off by default**, so JavaFX `id`/`styleClass` are never sent to the DOM. *Fix:* add it to the app's `jpro.conf` on the classpath.

- **Omitting the `jpro-` prefix in selectors.** *Symptom:* `#textfield` matches nothing; `#jpro-textfield` works. *Why:* JPro prefixes every mirrored id/class with `jpro-`. *Fix:* select `setId("x")` as `"#jpro-x"` and `getStyleClass().add("y")` as `".jpro-y"`.

- **No `jpro/html/defaultpage` in the app's resources.** *Symptom:* navigating to a route returns a static-file 404 / "Couldn't get resource for: 'jpro/html/...'" in the server log, and the app never boots. *Why:* `defaultpage` is the HTML shell JPro serves for app routes; without it the request falls through to the static-file handler. *Fix:* add `src/main/resources/jpro/html/defaultpage` containing a `<jpro-app href="/app/default" .../>` (see the `example` module).

- **Expecting a `TextFormatter` / `onAction` to react per keystroke.** *Symptom:* uppercase/validation/commit logic never runs while typing, so the asserted result never appears. *Why:* JavaFX commits on Enter or focus-loss, not per keystroke. *Fix:* call `JProInput.commit(page)` (Enter) before asserting the committed value.

- **Interacting before `waitForRunning(page)`.** *Symptom:* clicks/typing land on a half-loaded app and are lost; intermittent failures right after `navigate`. *Fix:* always `waitForRunning(page)` after every `navigate`, before the first interaction.

- **Wrong `logsDir` passed to `stopAndAssertNoServerErrors`.** *Symptom:* the assertion fails with "No 'Server started' marker found" even on a clean run. *Why:* it sanity-checks that the directory is the current run's log dir. *Fix:* point it at the served module's `logs/` (e.g. `new File(projectRoot(), "my-app/logs")`), not the test module's.

- **Running the tests without installing Chromium.** *Symptom:* `Playwright.create()` / launch fails at startup. *Why:* the test JVM has browser auto-download disabled. *Fix:* run `./gradlew :jpro-playwright:installPlaywright` once.
