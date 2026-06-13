package one.jpro.platform.routing

import org.junit.platform.launcher.{LauncherSession, LauncherSessionListener}
import simplefx.core._

import scala.concurrent.Await

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
    Await.result(simplefx.cores.initializeCore(), (second))
  }
}
