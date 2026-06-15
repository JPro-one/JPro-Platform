package one.jpro.platform.routing.crawl

import javafx.application.Platform
import javafx.scene.control.Label
import one.jpro.platform.routing.{Response, Route}
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

import java.util.concurrent.{CountDownLatch, TimeUnit}

/**
 * crawlRoute must bootstrap the SimpleFX core itself, so a caller that only started the FX toolkit
 * (no inFX / no FXCoreInitListener) does not hit "Core was first triggered from thread ...".
 * Run in isolation (e.g. --tests TestCrawlInitializesCore) to exercise the cold-start path.
 */
class TestCrawlInitializesCore {

  @Test
  def crawlRouteWorksWithoutSimpleFXSetup(): Unit = {
    // Start the FX toolkit only (deliberately NOT via inFX, so SimpleFX stays cold).
    val latch = new CountDownLatch(1)
    try Platform.startup(() => latch.countDown())
    catch { case _: IllegalStateException => latch.countDown() }
    assertTrue(latch.await(15, TimeUnit.SECONDS), "FX toolkit did not start")

    val route = Route.empty().and(Route.get("/", _ => Response.node(new Label("home"))))
    val report = AppCrawler.crawlRoute("http://localhost", () => route)
    assertTrue(report.pages.contains("/"), "crawl did not discover \"/\"")
  }
}
