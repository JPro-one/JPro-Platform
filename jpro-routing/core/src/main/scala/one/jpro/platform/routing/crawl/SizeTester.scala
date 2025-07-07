package one.jpro.platform.routing.crawl

import javafx.stage.Stage
import one.jpro.platform.routing.{Request, Route, RouteNode, View}
import one.jpro.platform.routing.crawl.AppCrawler.{CrawlReportApp, routeToRouteNode}
import org.slf4j.{Logger, LoggerFactory}
import one.jpro.platform.scenegraph.SceneGraphSerializer
import java.util.function.Supplier
import simplefx.core._
import simplefx.all._

object SizeTester {

  private lazy val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  def testSize(minWidth: Int, maxWidth: Int, report: CrawlReportApp, appFactory: Supplier[Route]): Unit = {
    testSize(minWidth, maxWidth, report.pages, appFactory)
  }

  def testSize(minWidth: Int, maxWidth: Int, pages: java.util.List[String], appFactory: Supplier[Route]): Unit = {
    pages.forEach(pageURL => {
      logger.debug(s"Checking for leak for the url: $pageURL")
      val routeNode: RouteNode = inFX(routeToRouteNode(appFactory.get()))
      assert(routeNode != null, "The routeNode must not return null ")

      val view = routeNode.getSessionManager().gotoURL(pageURL).future.await
      inFX(routeNode.scene.root.applyCss())

      assert(view.isInstanceOf[View])

      //println(SceneGraphSerializer.serialize(routeNode))
      assert(routeNode.scene != null, "The routeNode must have a scene")


      val node = view.asInstanceOf[View].realContent
      waitUntil(node.scene != null)
      inFX(assert(node.scene != null, "The node must have a scene"))
      val nodeMinWidth = node.minWidth(-1)
      val nodeMaxWidth = node.maxWidth(-1)
      logger.debug("Page: " + pageURL + " minWidth: " + nodeMinWidth + " maxWidth: " + nodeMaxWidth)
      assert(nodeMinWidth <= minWidth, s"Url: $pageURL Node minWidth: ${nodeMinWidth} should be less than or equal to $minWidth")
      assert(nodeMaxWidth >= maxWidth, s"Url: $pageURL Node maxWidth: ${nodeMaxWidth} should be greater than or equal to $maxWidth")
      inFX(routeNode.scene.window.asInstanceOf[Stage].close())
    })
  }
}
