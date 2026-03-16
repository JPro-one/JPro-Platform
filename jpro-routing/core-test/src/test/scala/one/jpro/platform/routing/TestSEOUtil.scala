package one.jpro.platform.routing

import javafx.application.Platform
import javafx.scene.control.Label
import org.junit.jupiter.api.{BeforeAll, Test}

import java.util.concurrent.CountDownLatch

object TestSEOUtil {
  @BeforeAll
  def init(): Unit = {
    val latch = new CountDownLatch(1)
    try {
      Platform.startup(() => latch.countDown())
    } catch {
      case _: IllegalStateException =>
        // Toolkit already initialized
        latch.countDown()
    }
    latch.await()
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
