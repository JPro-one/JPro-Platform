package one.jpro.platform.routing.crawl

import TestUtils.{Page1, Page2}
import javafx.scene.control.Label
import one.jpro.platform.routing.crawl.{AppCrawler, MemoryTester}
import one.jpro.platform.routing.{Route, RouteNode}
import simplefx.cores.default.inFX
import simplefx.util.Predef.intercept
import org.junit.jupiter.api.Test

class TestMemoryTester {

  @Test
  def simpleTest(): Unit = {
    def app = new RouteNode(null) {
      setRoute(Route.empty()
        .and(get("/", r => new Page1))
        .and(get("/page2", r => new Page2))
        .and(get("/page4", r => new Page2)))
    }
    val result = AppCrawler.crawlApp("http://localhost", () => app)
    MemoryTester.testForLeaks(result, () => app)
  }

  @Test
  def simpleFailingTest(): Unit = {
    val page2 = new Page2
    def app = new RouteNode(null) {
      setRoute(Route.empty()
        .and(get("/", r => new Page1))
        .and(get("/page2", r => page2))
        .and(get("/page4", r => new Page2)))
    }
    val result = AppCrawler.crawlApp("http://localhost", () => app)
    intercept[Throwable](MemoryTester.testForLeaks(result, () => app))
  }

  @Test
  def simpleFailingTest2(): Unit = {
    var node2 = new Label()

    def app = new RouteNode(null) {
      setRoute(Route.empty()
        .and(get("/", r => new Page1))
        .and(get("/page2", r => viewFromNode(node2)))
        .and(get("/page4", r => new Page2)))
    }

    val result = AppCrawler.crawlApp("http://localhost", () => app)
    intercept[Throwable](MemoryTester.testForLeaks(result, () => app))
  }

  @Test
  def simpleFailingTest3(): Unit = {
    val app = inFX(new RouteNode(null) {
      setRoute(Route.empty()
        .and(get("/", r => new Page1))
        .and(get("/page2", r => new Page2))
        .and(get("/page4", r => new Page2)))
    })
    val result = AppCrawler.crawlApp("http://localhost", () => app)
    intercept[Throwable](MemoryTester.testForLeaks(result, () => app)) // fails because the webapp is not collectable
  }

}
