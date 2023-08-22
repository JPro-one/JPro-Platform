package one.jpro.routing.sessionmanager

import java.net.URL
import java.net.URLDecoder
import one.jpro.routing.{Redirect, Response, RouteNode, View, HistoryEntry}
import com.jpro.webapi.{InstanceCloseListener, ScriptResultListener, WebAPI, WebCallback}
import one.jpro.routing.{Redirect, Response, RouteNode, View}
import simplefx.all._
import simplefx.core._
import simplefx.experimental._
import simplefx.util.ReflectionUtil


class SessionManagerDesktop(val webApp: RouteNode) extends SessionManager { THIS =>


  def goBack(): Unit = {
    historyForward = historyCurrent :: historyForward
    historyCurrent = historyBackward.head
    historyBackward = historyBackward.tail
    gotoURL(historyCurrent.path, false, true)
  }

  def goForward(): Unit = {
    assert(!historyForward.isEmpty, "Can't go forward, there is no entry in the forward history!")
    historyBackward = historyCurrent :: historyBackward
    historyCurrent = historyForward.head
    historyForward = historyForward.tail
    gotoURL(historyCurrent.path, false, true)
  }

  def gotoURL(_url: String, x: Response, pushState: Boolean, track: Boolean): Unit = {
    x match {
      case Redirect(url) => 
        println(s"redirect: ${_url} -> $url")
        gotoURL(url)
      case view: View =>
        val oldView = this.view
        this.view = view
        view.sessionManager = this
        view.url = url

        isFullscreen = view.fullscreen
        container.children = List(view.realContent)
        scrollpane.vvalue = 0.0
        if(oldView != null && oldView != view) {
          oldView.onClose()
          oldView.sessionManager = null
          markViewCollectable(oldView, view)
        }
        THIS.view = view

        if(pushState ) {
          historyForward = Nil
          if(historyCurrent != null) {
            historyBackward = historyCurrent :: historyBackward
          }
          historyCurrent = HistoryEntry(url, view.title)
        }
    }
  }
  val container = new StackPane()
    val scrollpane: ScrollPane = if(System.getProperty("routing.scrollpane") != null) {
    ReflectionUtil.callNew(System.getProperty("routing.scrollpane"))().asInstanceOf[ScrollPane]
  } else new ScrollPane()

  {
    scrollpane.fitToWidth = true
    scrollpane.content <-- container
    scrollpane.fitToHeight <-- isFullscreen
    scrollpane.style = "-fx-padding: 0;"
    scrollpane.background = Background.EMPTY
    scrollpane.vbarPolicy <-- (if(isFullscreen) ScrollPane.ScrollBarPolicy.NEVER else ScrollPane.ScrollBarPolicy.AS_NEEDED)
  }

  webApp <++ scrollpane
  @Bind var isFullscreen = true
  onceWhen(webApp.scene != null && webApp.scene.window != null) --> {
    val window = webApp.scene.window
    if(window.isInstanceOf[Stage]) {
      window.asInstanceOf[Stage].title <-- (if(view != null) view.title else "")
    }
  }

  def start() = {
    gotoURL("/", pushState = true)
  }
}
