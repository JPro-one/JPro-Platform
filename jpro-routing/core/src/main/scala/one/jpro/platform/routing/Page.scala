package one.jpro.platform.routing

import one.jpro.platform.routing.sessionmanager.SessionManager
import simplefx.all
import simplefx.all._

object Page {
  def fromNode(node: javafx.scene.Node): Page = RouteUtils.pageFromNode(node)
}
abstract class Page extends ResponseResult { THIS =>
  def title: String
  def description: String
  var url: String = null
  var isMobile: Boolean = false

  private var sessionManager: SessionManager = null
  def getSessionManager(): SessionManager = sessionManager
  def setSessionManager(x: SessionManager): Unit = {
    sessionManager = x
  }

  lazy val realContent: Node = content
  protected def content: Node
  def saveScrollPosition = true
  def fullscreen = false
  def onClose(): Unit = {}
  def subPage(): Page = null

  override def toString: String = s"Page(title: $title, url: $url, description: $description)"

  /**
   * Only overwrite this method, if you handle the url-change by yourself.
   * @param x path
   * @return whether the page handles the url change
   */
  def handleRequest(x: Request): Boolean = false
  def mapContent(f: Node => Node): Page = new Page {
    override def title: String = THIS.title

    override def description: String = THIS.description

    override def content: all.Node = f(THIS.realContent)

    override def fullscreen: Boolean = THIS.fullscreen

    override def setSessionManager(x: SessionManager): Unit = {
      super.setSessionManager(x)
      THIS.setSessionManager(x)
    }

    override def subPage(): Page = THIS
  }
}

