package one.jpro.platform.routing.crawl

import de.sandec.jmemorybuddy.JMemoryBuddy
import one.jpro.platform.routing.crawl.AppCrawler.CrawlReportApp
import one.jpro.platform.routing.{RouteNode, View}
import org.slf4j.{Logger, LoggerFactory}
import simplefx.cores.default.inFX

import java.util.function.Supplier

object MemoryTester {

  private lazy val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  def testForLeaks(x: CrawlReportApp, appFactory: Supplier[RouteNode]): Unit = {
    x.pages.map { pageURL =>
      logger.debug(s"Checking for leak for the url: $pageURL")
      JMemoryBuddy.memoryTest(checker1 => {
        JMemoryBuddy.memoryTest(checker2 => {
          val factory = inFX(appFactory.get())
          assert(factory != null, "The appFactory must not return null")
          val view = inFX(appFactory.get().route(pageURL)).await

          checker2.setAsReferenced(factory)
          checker2.assertCollectable(view) // Hm?
          if(view.isInstanceOf[View]) {
            checker2.assertCollectable(inFX(view.asInstanceOf[View].realContent))
          }
          checker1.assertCollectable(factory)
        })
      })
    }
  }
}
