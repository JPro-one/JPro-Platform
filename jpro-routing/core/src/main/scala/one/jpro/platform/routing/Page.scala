package one.jpro.platform.routing

import one.jpro.platform.routing.sessionmanager.SessionManager
import simplefx.all
import simplefx.all._

object Page {
  def fromNode(node: javafx.scene.Node): Page = RouteUtils.pageFromNode(node)
}
abstract class Page extends ResponseResult { THIS =>
  /** The page title; used for the browser tab and for SEO. May be `null`. */
  def title: String
  /** The page description; used for SEO and by the `AppCrawler`. */
  def description: String
  /** The URL this page was created for; set by the router. May be `null` before routing. */
  var url: String = null
  /** Whether the page is rendered for a mobile client; set by the router. */
  var isMobile: Boolean = false

  private var sessionManager: SessionManager = null
  /** Returns the [[SessionManager]] for this page, or `null` if it has not been set yet. */
  def getSessionManager(): SessionManager = sessionManager
  /** Sets the [[SessionManager]] for this page (normally called by the router). */
  def setSessionManager(x: SessionManager): Unit = {
    sessionManager = x
  }

  /** The page content node, computed once from [[content]] and cached. */
  lazy val realContent: Node = content
  /** Builds the page's content node. Implement this to provide the page UI. */
  protected def content: Node
  /** Whether the scroll position should be saved and restored when navigating back. */
  def saveScrollPosition = true
  /** Whether the page fills the whole area (true) or is scrollable (false). Override to change. */
  def fullscreen = false
  /** Called when the user navigates away from this page. Override to release resources. */
  def onClose(): Unit = {}
  /** The currently active sub-page, or `null` if there is none. */
  def subPage(): Page = null

  override def toString: String = s"Page(title: $title, url: $url, description: $description)"

  /**
   * Only overwrite this method, if you handle the url-change by yourself.
   * @param x path
   * @return whether the page handles the url change
   */
  def handleRequest(x: Request): Boolean = false

  /** Returns a copy of this page with the content mapped through {@code f}. */
  def mapContent(f: Node => Node): Page = new DelegatingPage(THIS) {
    override protected def content: Node = f(THIS.realContent)
  }

  /** Returns a copy of this page with a different title. */
  def withTitle(title: String): Page = {
    val _title = title
    new DelegatingPage(THIS) { override def title: String = _title }
  }

  /** Returns a copy of this page with a different description. */
  def withDescription(description: String): Page = {
    val _description = description
    new DelegatingPage(THIS) { override def description: String = _description }
  }

  /** Returns a copy of this page with a different fullscreen setting. */
  def withFullscreen(fullscreen: Boolean): Page = {
    val _fullscreen = fullscreen
    new DelegatingPage(THIS) { override def fullscreen: Boolean = _fullscreen }
  }
}

/**
 * A page that forwards every property to {@code original}. Used by the {@code with*}
 * copy methods and {@code mapContent}: subclass it and override the one member to change.
 */
private[routing] class DelegatingPage(original: Page) extends Page {
  override def title: String = original.title
  override def description: String = original.description
  override protected def content: Node = original.realContent
  override def fullscreen: Boolean = original.fullscreen
  override def saveScrollPosition: Boolean = original.saveScrollPosition
  override def handleRequest(x: Request): Boolean = original.handleRequest(x)
  override def onClose(): Unit = original.onClose()
  override def subPage(): Page = original
  override def setSessionManager(x: SessionManager): Unit = {
    super.setSessionManager(x)
    original.setSessionManager(x)
  }
}

