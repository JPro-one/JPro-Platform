package one.jpro.platform.routing.crawl

import javafx.scene.control.Label
import one.jpro.platform.routing.RouteUtils._
import one.jpro.platform.routing.crawl.TestUtils.{Page1, Page2}
import one.jpro.platform.routing.{Route, RouteNode}
import org.junit.jupiter.api.Test
import simplefx.cores.default.inFX
import simplefx.util.Predef.intercept

class TestMemoryTester {

  @Test
  def simpleTest(): Unit = {
    def app = new RouteNode(null) {
      setRoute(Route.empty()
        .and(getView("/", r => new Page1))
        .and(getView("/page2", r => new Page2))
        .and(getView("/page4", r => new Page2)))
    }
    val result = AppCrawler.crawlApp("http://localhost", () => app)
    MemoryTester.testForLeaks(result, () => app)
  }

  @Test
  def simpleFailingTest(): Unit = {
    val page2 = new Page2
    def app = new RouteNode(null) {
      setRoute(Route.empty()
        .and(getView("/", r => new Page1))
        .and(getView("/page2", r => page2))
        .and(getView("/page4", r => new Page2)))
    }
    val result = AppCrawler.crawlApp("http://localhost", () => app)
    intercept[Throwable](MemoryTester.testForLeaks(result, () => app))
  }

  @Test
  def simpleFailingTest2(): Unit = {
    var node2 = new Label()

    def app = new RouteNode(null) {
      setRoute(Route.empty()
        .and(getView("/", r => new Page1))
        .and(getView("/page2", r => viewFromNode(node2)))
        .and(getView("/page4", r => new Page2)))
    }

    val result = AppCrawler.crawlApp("http://localhost", () => app)
    intercept[Throwable](MemoryTester.testForLeaks(result, () => app))
  }

  @Test
  def simpleFailingTest3(): Unit = {
    val app = inFX(new RouteNode(null) {
      setRoute(Route.empty()
        .and(getView("/", r => new Page1))
        .and(getView("/page2", r => new Page2))
        .and(getView("/page4", r => new Page2)))
    })
    val result = AppCrawler.crawlApp("http://localhost", () => app)
    intercept[Throwable](MemoryTester.testForLeaks(result, () => app)) // fails because the webapp is not collectable
  }
}
