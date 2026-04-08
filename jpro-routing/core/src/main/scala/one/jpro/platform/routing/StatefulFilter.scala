package one.jpro.platform.routing

import org.slf4j.{Logger, LoggerFactory}

/**
 * Base class for filters that hold per-session state — a container Node, a
 * cache, a session token, an animation, etc. Such filters have meaningful
 * object identity and MUST be constructed once at session-startup time
 * (typically inside `RouteApp.createRoute()` or in a field on a Site object),
 * not inside per-request route lambdas like `filterWhen` / `filterWhenFuture`.
 *
 * Constructing a `StatefulFilter` while a per-request lambda is on the stack
 * is reported via [[StatefulFilterContext.violationHandler]] — by default
 * this throws an [[IllegalStateException]] (fail-fast). Tests can flip the
 * handler to a no-op or a logger to relax this.
 *
 * The check is best-effort: it relies on the framework wrapping its
 * user-callback invocations with [[StatefulFilterContext.runInRequestLambda]].
 * `Route.filterWhen` / `Route.filterWhenFuture` do this automatically.
 */
abstract class StatefulFilter extends Filter {
  StatefulFilterContext.checkConstruction(getClass)
}

object StatefulFilterContext {
  private val logger: Logger = LoggerFactory.getLogger(classOf[StatefulFilter])

  // Most routing happens on the FX thread; a single mutable flag is enough.
  // Saved/restored in runInRequestLambda so legitimate nesting works.
  private var inRequestLambda: Boolean = false

  /**
   * How violations are reported. Default: fail fast with an
   * `IllegalStateException`. Replaceable for tests or for relaxed modes.
   */
  @volatile var violationHandler: String => Unit = msg => {
    throw new IllegalStateException(msg)
  }

  /**
   * True if the current call stack contains a per-request route lambda
   * (one that was wrapped via [[runInRequestLambda]]).
   */
  def isInRequestLambda: Boolean = inRequestLambda

  /**
   * Marks the dynamic extent of `body` as "inside a per-request lambda".
   * `StatefulFilter` constructors invoked from within `body` will trigger
   * the violation handler.
   *
   * Saves and restores the flag so nested invocations are handled.
   */
  def runInRequestLambda[T](body: => T): T = {
    val prev = inRequestLambda
    inRequestLambda = true
    try body finally inRequestLambda = prev
  }

  /** Java-friendly variant of [[runInRequestLambda]]. */
  def runInRequestLambda[T](body: java.util.function.Supplier[T]): T = {
    val prev = inRequestLambda
    inRequestLambda = true
    try body.get() finally inRequestLambda = prev
  }

  private[routing] def checkConstruction(filterClass: Class[_]): Unit = {
    if (inRequestLambda) {
      violationHandler(
        s"Stateful filter ${filterClass.getName} was constructed inside a per-request route lambda. " +
        "This creates a new instance on every request, defeating any per-session state " +
        "(e.g. ContainerFilter's container reuse). Hoist the filter outside of " +
        "filterWhen / filterWhenFuture lambdas — typically as a field on your RouteApp " +
        "or a Site object — and reference the field from inside the lambda."
      )
    }
  }

  /**
   * Test helper: runs `body` with violations captured silently. Returns the
   * list of warning messages that would have fired. Restores the previous
   * handler afterwards. Not thread-safe.
   */
  def captureViolations[T](body: => T): (T, List[String]) = {
    val prev = violationHandler
    val captured = scala.collection.mutable.ListBuffer.empty[String]
    violationHandler = msg => captured += msg
    try (body, captured.toList) finally violationHandler = prev
  }

  /**
   * Test helper: runs `body` with violations turned into warnings (no
   * exception). Useful when you want to construct a stateful filter inside
   * a lambda intentionally — e.g. in a fixture — without aborting the test.
   */
  def withWarnOnly[T](body: => T): T = {
    val prev = violationHandler
    violationHandler = msg => logger.warn(msg)
    try body finally violationHandler = prev
  }
}
