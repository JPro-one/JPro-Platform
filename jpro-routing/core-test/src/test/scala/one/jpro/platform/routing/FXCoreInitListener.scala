package one.jpro.platform.routing

import org.junit.platform.launcher.{LauncherSession, LauncherSessionListener}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Initializes the SimpleFX core once, before any test class is loaded. SimpleFX's
 * package object runs `initializeTheCore` (which must run on the FX thread) in its
 * static initializer; `initializeCore()` bootstraps that correctly from any thread.
 * Doing it here makes the suite independent of which test class touches SimpleFX first.
 *
 * Registered via META-INF/services/org.junit.platform.launcher.LauncherSessionListener.
 */
class FXCoreInitListener extends LauncherSessionListener {
  override def launcherSessionOpened(session: LauncherSession): Unit = {
    // Nothing from simplefx.core may be touched here: on this (non-FX) thread it would race the initialization
    Await.result(simplefx.cores.initializeCore(), 30.seconds)
  }
}
