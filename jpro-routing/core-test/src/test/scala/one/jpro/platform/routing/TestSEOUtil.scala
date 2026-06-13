package one.jpro.platform.routing

import javafx.application.Platform
import javafx.scene.control.Label
import org.junit.jupiter.api.{BeforeAll, Test}
import simplefx.cores.default.inFX

import java.util.concurrent.CountDownLatch

object TestSEOUtil {
  // inFX initializes the SimpleFX core on the FX thread; bare Platform.startup
  // would leave the core uninitialized and poison it for the whole suite
  @BeforeAll
  def init(): Unit = inFX {
    Platform.setImplicitExit(false)
  }
}

class TestSEOUtil {

  @Test
  def testH1DoesNotThrowOutsideJPro(): Unit = {
    val latch = new CountDownLatch(1)
    Platform.runLater(() => {
      val label = new Label("Hello")
      SEOUtil.h1(label)
      latch.countDown()
    })
    latch.await()
  }

  @Test
  def testWrapDoesNotThrowOutsideJPro(): Unit = {
    val latch = new CountDownLatch(1)
    Platform.runLater(() => {
      val label = new Label("Hello")
      SEOUtil.wrap("div", label)
      latch.countDown()
    })
    latch.await()
  }
}
