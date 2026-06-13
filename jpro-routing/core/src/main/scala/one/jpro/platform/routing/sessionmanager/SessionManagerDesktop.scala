package one.jpro.platform.routing.sessionmanager

import one.jpro.platform.routing._
import org.slf4j.{Logger, LoggerFactory}
import simplefx.all._
import simplefx.core._
import simplefx.util.ReflectionUtil

class SessionManagerDesktop(val webApp: RouteNode) extends SessionManager { THIS =>
  assert(webApp != null, "webApp must not be null!")

  private lazy val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  def goBack(): Unit = {
    historyForward = historyCurrent :: historyForward
    historyCurrent = historyBackward.head
    historyBackward = historyBackward.tail
    gotoURL(historyCurrent.path, false)
  }

  def goForward(): Unit = {
    assert(historyForward.nonEmpty, "Can't go forward, there is no entry in the forward history!")
    historyBackward = historyCurrent :: historyBackward
    historyCurrent = historyForward.head
    historyForward = historyForward.tail
    gotoURL(historyCurrent.path, false)
  }

  def gotoURL(_url: String, x: ResponseResult, pushState: Boolean): Response = {
    x match {
      case Redirect(url) =>
        logger.debug(s"redirect: ${_url} -> $url")
        redirect(url, _url)
      case page: Page =>
        redirectCounter = 0
        val oldView = this.page
        this.page = page
        page.setSessionManager(this)
        page.url = url

        isFullscreen = page.fullscreen
        container.children = List(page.realContent)
        scrollpane.vvalue = 0.0
        if(oldView != null && oldView != page) {
          oldView.onClose()
          oldView.setSessionManager(null)
          markPageCollectable(oldView, page)
        }
        THIS.page = page

        if(pushState ) {
          historyForward = Nil
          if(historyCurrent != null) {
            historyBackward = historyCurrent :: historyBackward
          }
          historyCurrent = HistoryEntry(url, page.title)
        }
        Response.page(page)
    }
  }
  val container = new StackPane()
  val scrollpane: ScrollPane = if(System.getProperty("routing.scrollpane") != null) {
    ReflectionUtil.callNew(System.getProperty("routing.scrollpane"))().asInstanceOf[ScrollPane]
  } else new ScrollPane()

  customizeScrollpane(scrollpane)
  def customizeScrollpane(scrollpane: ScrollPane): Unit = {
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
      window.asInstanceOf[Stage].title <-- (if(page != null) page.title else "")
    }
  }

  def start(): Response = {
    gotoURL("/", pushState = true)
  }
}
