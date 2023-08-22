package one.jpro.routing.crawl

import AppCrawler.CrawlReportApp
import de.sandec.jmemorybuddy.JMemoryBuddy
import one.jpro.routing.{RouteNode, View}
import simplefx.cores.default.inFX

import java.util.function.Supplier

object MemoryTester {

  def testForLeaks(x: CrawlReportApp, appFactory: Supplier[RouteNode]): Unit = {
    x.pages.map { pageURL =>
      println("Checking for leak for the url: " + pageURL)
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
