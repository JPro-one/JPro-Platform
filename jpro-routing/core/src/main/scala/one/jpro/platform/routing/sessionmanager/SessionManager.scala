package one.jpro.platform.routing.sessionmanager

import com.jpro.webapi.WebAPI
import one.jpro.jmemorybuddy.JMemoryBuddyLive
import javafx.beans.property.{ObjectProperty, SimpleObjectProperty}
import javafx.collections.{FXCollections, ObservableList}
import one.jpro.platform.routing.{HistoryEntry, Request, Response, ResponseResult, RouteNode, SessionManagerContext, Page}
import org.jetbrains.annotations.Nullable
import org.slf4j.{Logger, LoggerFactory}
import simplefx.all._
import simplefx.core._
import simplefx.experimental._
import one.jpro.platform.utils.OpenLink

import java.net.URI
import java.util.function.Consumer


/**
 * Manages the navigation of a routing application: the current URL and page,
 * the history, and programmatic navigation via {@code gotoURL}. There is one
 * SessionManager per application; obtain it via {@code RouteApp.getSessionManager()}
 * or {@code LinkUtil.getSessionManager(node)}. In the browser it is backed by the
 * browser history, on desktop by an in-memory history.
 */
trait SessionManager { THIS =>

  private lazy val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  SessionManagerContext.setContext(webApp, this)
  webApp.sessionManager = this

  def webApp: RouteNode

  val MAX_REDIRECTS = 10
  val getHistoryBackward: ObservableList[HistoryEntry] = FXCollections.observableArrayList()
  val currentHistoryProperty: ObjectProperty[HistoryEntry] = new SimpleObjectProperty(null)
  val getHistoryForwards: ObservableList[HistoryEntry] = FXCollections.observableArrayList()

  @Bind var historyBackward: List[HistoryEntry] = getHistoryBackward.toBindable
  @Bind var historyCurrent : HistoryEntry = currentHistoryProperty.toBindable
  @Bind var historyForward : List[HistoryEntry] = getHistoryForwards.toBindable

  @Bind var url: String = null
  @Bind var page: Page = null

  /** Returns the currently shown page, or `null` before the first navigation. */
  @Nullable def getPage(): Page = page
  /** Returns the URL currently shown, or `null` before the first navigation. */
  @Nullable def getURL(): String = url

  /** Navigates back in the history. */
  def goBack(): Unit
  /** Navigates forward in the history. */
  def goForward(): Unit
  def isExternal(x: String): Boolean = x.startsWith("http")

  var redirectCounter = 0
  def redirect(to: String, from: String): Response = {
    redirectCounter += 1
    if(redirectCounter > MAX_REDIRECTS) {
      logger.error(s"Too many redirects (more than 10). Last redirect was from $from to $to")
      return Response.error(new Exception(s"Too many redirects (more than 10). Last redirect was from $from to $to"))
    }
    if(redirectCounter > 1) {
      logger.info(s"Redirecting from $from to $to (redirect #$redirectCounter)")
    } else {
      logger.info(s"Redirecting from $from to $to")
    }
    gotoURL(to)
  }

  /**
   * Navigates to the given URL. External URLs (starting with "http") leave the
   * application; other URLs are resolved against the current one and routed,
   * pushing a new history entry.
   */
  def gotoURL(url: String): Response = {
    if(isExternal(url)) {
      logger.info(s"Opening external link: $url")
      if(WebAPI.isBrowser) {
        this.asInstanceOf[SessionManagerWeb].webAPI.executeScript(s"""window.location.href = "$url";""")
      } else {
        SessionManager.externalLinkImpl.accept(url)
      }
      Response.redirect(url)
    } else {
      gotoURL(url,true)
    }
  }
  /** Navigates to the given URL, optionally without pushing a new history entry. */
  def gotoURL(url: String, pushState: Boolean = true): Response = {
    val url2 = SessionManager.mergeURLs(THIS.url, url)
    try {
      logger.debug(s"goto: $url2")
      val request = getRequest(url2)
      val newView = if(page != null && page.handleRequest(request)) Response(FXFuture(page)) else {
        webApp.getRoute()(request)
      }
      Response.fromFuture(newView.future.map { response =>
        assert(response != null, s"Response for $url2 was null")
        this.url = url2
        gotoURL(url2, response, pushState)
      })
    } catch {
      case ex: Exception =>
        logger.error(s"Error while loading the path $url2", ex)
        Response.error(ex)
    }
  }
  def gotoURL(_url: String, x: ResponseResult, pushState: Boolean): Response

  /** Builds a {@link Request} for the given URL, carrying the current page's content as the previous view. */
  def getRequest(url: String): Request = {
    val node = if(page == null) null else page.realContent
    Request.fromString(url, node)
  }

  /** Performs the initial navigation for the application; called once by the router on startup. */
  def start(): Response

  def markPageCollectable(page: Page): Unit = {
    JMemoryBuddyLive.markCollectable(s"Page url: ${page.url} title: ${page.title}", page.realContent)
  }
  def markPageCollectable(oldView: Page, newView: Page): Unit = {
//    logger.debug(s"depths: ${pageDepth(oldView)} - ${pageDepth(newView)}")
    if(oldView.realContent != newView.realContent) {
//      logger.debug(s"nodes: ${oldView.realContent} - ${newView.realContent}")
      JMemoryBuddyLive.markCollectable(s"Page url: ${oldView.url} title: ${oldView.title}", oldView.realContent)
    }
    if(oldView.subPage() != null && newView.subPage != null) {
      markPageCollectable(oldView.subPage(), newView.subPage())
    }
  }
  def pageDepth(x: Page): Int = {
    if(x.subPage() == null) 1 else 1 + pageDepth(x.subPage())
  }
}

object SessionManager {
  /** Returns the SessionManager for the environment: browser-backed under JPro, in-memory on desktop. */
  def getDefault(app: RouteNode, stage: Stage): SessionManager = {
    if(WebAPI.isBrowser) new SessionManagerWeb(app, WebAPI.getWebAPI(stage))
    else new SessionManagerDesktop(app)
  }

  def mergeURLs(orig: String, next: String): String = try {
    if(orig == null) next
    else URI.create(orig).resolve(next).toString
  } catch {
    case e: Exception =>
      throw new Exception(s"Error while merging $orig and $next", e)
  }

  def setExternalLinkImpl(f: Consumer[String]): Unit = externalLinkImpl = f
  var externalLinkImpl: Consumer[String] = { url =>
    OpenLink.openURL(url)
  }
}
