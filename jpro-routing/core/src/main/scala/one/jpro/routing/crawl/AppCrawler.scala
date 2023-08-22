package one.jpro.routing.crawl

import one.jpro.routing._
import one.jpro.routing.sessionmanager.DummySessionManager
import one.jpro.routing.{Redirect, RouteNode, SessionManagerContext, View}
import simplefx.all._
import simplefx.core._
import simplefx.experimental._

import java.io.File
import java.util.function.Supplier
import scala.collection.JavaConverters._

object AppCrawler {
  case class LinkInfo(url: String, description: String)

  case class ImageInfo(url: String, description: String)

  case class CrawlReportPage(path: String, links: List[LinkInfo], pictures: List[ImageInfo], title: String, description: String)

  case class CrawlReportApp(pages: List[String], reports: List[CrawlReportPage], deadLinks: List[String])

  def crawlPage(page: View): CrawlReportPage = {
    var foundLinks: List[LinkInfo] = Nil
    var images: List[ImageInfo] = Nil

    var visitedNodes: Set[Node] = Set()

    def crawlNode(x: Node): Unit = {
      if(x == null) return
      if (visitedNodes.contains(x)) return
      visitedNodes += x
      if (x.getProperties.containsKey("link")) {
        val link = x.getProperties.get("link").asInstanceOf[String]
        if(link != null) {
          var desc = x.getProperties.get("description").asInstanceOf[String]
          if(desc == null) desc = ""
          foundLinks ::= LinkInfo(link, desc)
        }
      }

      if (x.isInstanceOf[Parent]) {
        x.asInstanceOf[Parent].childrenUnmodifiable.map(x => crawlNode(x))
      }
      if (x.isInstanceOf[Labeled]) {
        crawlNode(x.asInstanceOf[Labeled].graphic)
      }
      if (x.isInstanceOf[ScrollPane]) {
        crawlNode(x.asInstanceOf[ScrollPane].content)
      }
      if (x.isInstanceOf[ListView[_]]) {
        val lview = x.asInstanceOf[ListView[Any]]
        if(lview.getItems != null) {
          lview.getItems.asScala.zipWithIndex.map { case (item,index) =>
            val factory = lview.cellFactoryProperty().get()
            if(factory != null) {
              val cell: ListCell[Any] = factory.call(lview)
              cell.setItem(item)
              cell.updateIndex(index)
              cell.updateListView(lview)
              cell.layout()
              crawlNode(cell)
            }
          }
        }
      }
      if (x.isInstanceOf[Region]) {
        val region = x.asInstanceOf[Region]
        var rimages = List.empty[Image]
        if(region.border != null && region.border.getImages != null) rimages :::= region.border.getImages.asScala.map(_.getImage).toList
        if(region.background != null && region.background.getImages != null) rimages :::= region.background.getImages.asScala.map(_.getImage).toList
        rimages.foreach{ image =>
          val imgURL = getImageURL(image)
          if(imgURL != null) {
            images ::= ImageInfo(imgURL,region.accessibleRoleDescription)
          }
        }
      }
      if(x.isInstanceOf[ImageView]) {
        val view = x.asInstanceOf[ImageView]
        if(view.image != null) {
          val url = getImageURL(view.image)
          val description = view.accessibleRoleDescription
          if(url != null) {
            images ::= ImageInfo(url,description)
          }
        }
      }
    }

    val scene = new Scene(new Group())
    val content = page.realContent.asInstanceOf[Parent]
    SessionManagerContext.setContext(content, new DummySessionManager)
    scene.setRoot(content)
    page.realContent.applyCss()
    crawlNode(page.realContent)

    CrawlReportPage(page.url, foundLinks.reverse, images.reverse, page.title, page.description)
  }

  def crawlApp(prefix: String, createApp: Supplier[RouteNode]): CrawlReportApp = {
    var toIndex = Set[String]("/")
    var indexed = Set[String]()
    var redirects = Set[String]()
    var deadLinks = Set[String]()
    var reports: List[CrawlReportPage] = List()

    def isOwnLink(x: String): Boolean = x.startsWith(prefix) || x.startsWith("/")
    while (!toIndex.isEmpty) {
      val crawlNext = toIndex.head
      toIndex -= crawlNext
      indexed += crawlNext
      val result = inFX {
        val r = createApp.get().route(crawlNext)
        if(r == null) FXFuture.unit(null)
        else r
      }.await
      result match {
        case Redirect(url) =>
          redirects += crawlNext
          if (!indexed.contains(url) && !toIndex.contains(url)) {
            if (isOwnLink(url)) {
              toIndex += url
            }
          }
        case view: View =>
          view.url = crawlNext
          try {
            val newReport = inFX(crawlPage(view))
            reports = newReport :: reports
            def simplifyLink(x: String) = {
              if(x.startsWith(prefix)) x.drop(prefix.length) else x
            }
            newReport.links.filter(x => isOwnLink(x.url)).map { link =>
              val url = simplifyLink(link.url)
              if (!indexed.contains(url) && !toIndex.contains(url)) {
                toIndex += url
              }
            }
          } catch {
            case e: Throwable =>
              println("Error crawling page: " + crawlNext)
              deadLinks += crawlNext
              e.printStackTrace()
          }
        case null =>
          deadLinks += crawlNext
      }
    }

    CrawlReportApp((indexed -- redirects -- deadLinks).toList, reports.reverse, deadLinks.toList)
  }

  def getImageURL(x: Image): String = {
    if(x.getUrl == null) return null;
    val url = simplifyAndEncode(x.getUrl())
    if(url.startsWith("http")) {
      url
    } else {
      "/app/default/resourcesencoded/" + url
    }
  }


  private val cpTriggers = List[String]("jar!","classes", "main")
  private val local = new File("").getAbsoluteFile.toURI.toURL.toString
  private val home = new File(System.getProperty("user.home")).getAbsoluteFile.toURI.toURL.toString
  def fixFile(x: String) = "file://" + x.drop("file:".length)
  private val shortcuts = List[(String,String)](
    local -> "local://",
    "jar:" + fixFile(local) -> "jar:local://",
    home -> "home://",
    "jar:" + fixFile(home) -> "jar:home://",
  )
  def simplifyAndEncode(x: String) = encodeSlashes(simplifyURL(x))
  def encodeSlashes(x: String): String = {
    x.replaceAllLiterally("/1", "/11")
      .replaceRepeatedly("//", "/1/")
  }
  implicit class ExtStr(val x: String) extends AnyVal {
    def replaceRepeatedly(oldString: String, newString: String): String = {
      val r = x.replaceAllLiterally(oldString,newString)
      if(r != x) r.replaceRepeatedly(oldString,newString) else r
    }
  }
  private def simplifyURL(x: String): String = {
    cpTriggers.collectFirst{
      Function.unlift{ (cpTrigger: String) =>
        val split = x.split(cpTrigger)
        if(split.length > 1) {
          val cp = split.last
          val idea = "cp://" + cp
          val url = classOf[javafx.scene.Node].getResource(cp)
          if (url != null && x == url.toString) {
            Some(s"cp://$cp")
          } else None
        } else None
      }}.getOrElse {
      shortcuts.collectFirst{ Function.unlift{ case (long,short) =>
        if(x.startsWith(long)) { Some(short + x.drop(long.length))}
        else None
      }}.getOrElse(x)
    }
  }
}
