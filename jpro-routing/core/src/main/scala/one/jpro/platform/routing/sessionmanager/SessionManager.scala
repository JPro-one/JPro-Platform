package one.jpro.platform.routing.sessionmanager

import com.jpro.webapi.WebAPI
import one.jpro.jmemorybuddy.JMemoryBuddyLive
import javafx.beans.property.{ObjectProperty, SimpleObjectProperty}
import javafx.collections.{FXCollections, ObservableList}
import one.jpro.platform.routing.{HistoryEntry, Response, ResponseResult, RouteNode, View}
import org.slf4j.{Logger, LoggerFactory}
import simplefx.all._
import simplefx.core._
import simplefx.experimental._
import one.jpro.platform.utils.OpenLink

import java.net.URI
import java.util.function.Consumer


trait SessionManager { THIS =>

  private lazy val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  def webApp: RouteNode

  var ganalytics = false
  var gtags = false
  var trackingID = ""

  val getHistoryBackward: ObservableList[HistoryEntry] = FXCollections.observableArrayList()
  val currentHistoryProperty: ObjectProperty[HistoryEntry] = new SimpleObjectProperty(null)
  val getHistoryForwards: ObservableList[HistoryEntry] = FXCollections.observableArrayList()

  @Bind var historyBackward: List[HistoryEntry] = getHistoryBackward.toBindable
  @Bind var historyCurrent : HistoryEntry = currentHistoryProperty.toBindable
  @Bind var historyForward : List[HistoryEntry] = getHistoryForwards.toBindable

  @Bind var url: String = null
  @Bind var view: View = null

  def goBack(): Unit
  def goForward(): Unit
  def isExternal(x: String): Boolean = x.startsWith("http")
  def gotoURL(url: String): Unit = {
    if(isExternal(url)) {
      if(WebAPI.isBrowser) {
        this.asInstanceOf[SessionManagerWeb].webAPI.executeScript(s"""window.location.href = "$url";""")
      } else {
        SessionManager.externalLinkImpl.accept(url)
      }
    } else {
      gotoURL(url,true,true)
    }
  }
  def gotoURL(url: String, pushState: Boolean = true, track: Boolean = true): Unit = {
    val url2 = SessionManager.mergeURLs(THIS.url, url)
    try {
      logger.debug(s"goto: $url")
      val newView = if(view != null && view.handleURL(url)) Response(FXFuture(view)) else {
        getView(url2)
      }
      newView.future.map { response =>
        assert(response != null, s"Response for $url2 was null")
        this.url = url2
        gotoURL(url2, response, pushState, track)
      }
    } catch {
      case ex: Exception =>
        logger.error(s"Error while loading the path $url2", ex)
    }
  }

  def gotoURL(_url: String, x: ResponseResult, pushState: Boolean, track: Boolean): Unit

  def getView(url: String): Response = {
    val node = if(view == null) null else view.realContent
    webApp.route(url, node)
  }

  def start(): Unit

  def markViewCollectable(view: View): Unit = {
    JMemoryBuddyLive.markCollectable(s"Page url: ${view.url} title: ${view.title}", view.realContent)
  }
  def markViewCollectable(oldView: View, newView: View): Unit = {
//    logger.debug(s"depths: ${viewDepth(oldView)} - ${viewDepth(newView)}")
    if(oldView.realContent != newView.realContent) {
//      logger.debug(s"nodes: ${oldView.realContent} - ${newView.realContent}")
      JMemoryBuddyLive.markCollectable(s"Page url: ${oldView.url} title: ${oldView.title}", oldView.realContent)
    }
    if(oldView.subView() != null && newView.subView != null) {
      markViewCollectable(oldView.subView(), newView.subView())
    }
  }
  def viewDepth(x: View): Int = {
    if(x.subView() == null) 1 else 1 + viewDepth(x.subView())
  }
}

object SessionManager {
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
