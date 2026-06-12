package one.jpro.platform.routing

import com.jpro.webapi.WebAPI
import simplefx.core._
import simplefx.all._
import simplefx.util.Predef._

import java.net.URI
import one.jpro.platform.routing.sessionmanager.SessionManager
import org.slf4j.{Logger, LoggerFactory}

/**
 * Static methods to make nodes act as links and to navigate programmatically.
 * Prefer {@code setLink} over click handlers — links support SEO on the web
 * (a real anchor element is created) and give the user hover feedback.
 *
 * The node-based methods only work while the node is part of the scene graph
 * of the running routing application.
 */
object LinkUtil {

  private var openLinkExternalFun: String => Unit = { link =>
    import one.jpro.platform.utils.OpenLink
    OpenLink.openURL(link)
  }
  /** Overrides how external links are opened on desktop (default: the system browser). */
  def setOpenLinkExternalFun(x: String => Unit): Unit = {
    openLinkExternalFun = x
  }

  /** Returns whether the given string parses as a URI. */
  def isValidLink(x: String): Boolean = {
    try {
      val uri = new URI(x)
      true
    } catch {
      case _: Throwable => false
    }
  }

  /** Returns the SessionManager of the application the given node belongs to. */
  def getSessionManager(node: Node): SessionManager = {
    val sm = SessionManagerContext.getContext(node)
    assert(sm != null, "SessionManager was null")
    sm
  }

  /**
   * Makes the node a link to the given URL. Internal URLs (starting with "/", "./"
   * or "../") navigate within the application; other URLs leave it.
   */
  def setLink(node: Node, url: String): Unit = {
    setLink(node,url,None)
  }
  /**
   * Makes the node a link with a description (shown as tooltip in the browser,
   * also used for accessibility).
   */
  def setLink(node: Node, url: String, text: String): Unit = {
    setLink(node,url,Some(text))
  }
  /**
   * Makes the node a link.
   *
   * @param node     the node to turn into a link
   * @param url      the target; internal URLs start with "/", "./" or "../"
   * @param text     optional description (browser tooltip, accessibility)
   * @param external whether the link opens in a new tab / external browser
   */
  def setLink(node: Node, url: String, text: Option[String] = None, external: Boolean = false): Unit = {
    assert(url != "", s"Empty link: ''")
    assert(isValidLink(url), s"Invalid link: '$url''")
    node.getProperties.put("link",url)
    text.map {desc =>
      node.getProperties.put("description",desc)
    }
    if(url == null || url.startsWith("/") || url.startsWith("./") || url.startsWith("../")) {
      setLinkInternalPush(node,url, text, external)
    } else {
      setLinkInternalNoPush(node,url, text, external)
    }
  }

  /** Makes the node a link that opens in a new tab / external browser. */
  def setExternalLink(node: Node, url: String): Unit = {
    setLink(node,url,None, true)
  }
  /** Makes the node an external link with a description. */
  def setExternalLink(node: Node, url: String, text: String): Unit = {
    setLink(node,url,Option(text), true)
  }
  /** Makes the node a link that pushes a new entry onto the browser history. */
  def setLinkInternalPush(node: Node, url: String, text: Option[String] = None, external: Boolean = false) = {
    node.cursor = javafx.scene.Cursor.HAND
    setLinkSimple(url, text, true, external)(node)
  }
  /** Makes the node a link that navigates without adding a browser history entry. */
  def setLinkInternalNoPush(node: Node, url: String, text: Option[String] = None, external: Boolean = false) = {
    node.cursor = javafx.scene.Cursor.HAND
    setLinkSimple(url, text, false, external)(node)
  }

  /** Navigates back in the history of the application the given node belongs to. */
  def goBack(node: Node): Unit = {
    SessionManagerContext.getContext(node).goBack()
  }

  /** Navigates forward in the history of the application the given node belongs to. */
  def goForward(node: Node): Unit = {
    SessionManagerContext.getContext(node).goForward()
  }
  /** Navigates the application the given node belongs to, to the given URL. */
  def gotoPage(node: Node, url: String) = {
    LinkUtil.getSessionManager(node).gotoURL(url)
  }
  /** Navigates the application of the given SessionManager to the given URL. */
  def gotoPage(sessionManager: SessionManager, url: String): Unit = {
    sessionManager.gotoURL(url)
  }
  /** Returns the URL currently shown by the application the given node belongs to. */
  def getCurrentPage(node: Node): String = {
    LinkUtil.getSessionManager(node).url
  }
  /** Reloads the current page without adding a browser history entry. */
  def refresh(node: Node): Unit = {
    val man = LinkUtil.getSessionManager(node)
    assert(man.url != null, "current url was null")
    man.gotoURL(man.url, false)
  }

  private object LinkDesktop {

    private lazy val logger: Logger = LoggerFactory.getLogger(getClass.getName)

    @extension
    class ExtendNodeWithLink(node: Node) {
      def setNewLink(link: String, text: Option[String], pushState: Boolean, external: Boolean): Unit = {
        if (link != null && !isValidLink(link)) {
          logger.warn("Warning, link is not valid: " + link)
        }
        this.pushState = pushState
        this.external = external
        this.link = link
        this.text = text
      }

      @Bind var external: Boolean = true
      @Bind var pushState: Boolean = false
      @Bind var link: String = ""
      @Bind var text: Option[String] = None

      if(!WebAPI.isBrowser) {
        node.onMouseClicked --> { e =>
          def isExternalLink(x: String) = x.startsWith("http") || x.startsWith("mailto")
          if(e.isStillSincePress) {
            if(isExternalLink(link)) {
              openLinkExternalFun(link)
            } else {
              LinkUtil.getSessionManager(node).gotoURL(link)
            }
          }
        }
      }
    }
  }

  private object LinkJPro {

    private lazy val logger: Logger = LoggerFactory.getLogger(getClass.getName)

    @extension
    class ExtendNodeWithLink(node: Node) {

      def setNewLink(link: String, text: Option[String], pushState: Boolean, external: Boolean): Unit = {
        if (link != null && !isValidLink(link)) {
          logger.warn("Warning, link is not valid: " + link)
        }

        this.pushState = pushState
        this.external = external
        this.link = link
        this.text = text
      }

      @Bind var external: Boolean = true
      @Bind var pushState: Boolean = false
      @Bind var link: String = ""
      @Bind var text: Option[String] = None

      WebAPI.getWebAPI(node, webapi => {

        val aElem = webapi.wrapNode("a", node)

        val divBox = webapi.executeScriptWithVariable("document.createElement('div')");
        webapi.executeScript(
          s"""
             |${aElem.getName}.style.pointerEvents = 'auto';
             |${aElem.getName}.appendChild(${divBox.getName});
             |${divBox.getName}.style.display = 'block';
             |${divBox.getName}.style.position = 'absolute';
             |""".stripMargin)
        node.labWH --> { wh =>
          // set WH to a
          webapi.executeScript(
            s"""
               |${divBox.getName}.style.width = '${wh._1}px';
               |${divBox.getName}.style.height = '${wh._2}px';
               |""".stripMargin)
        }


        @Bind var linkAndPush = <-- (link, pushState)
        linkAndPush --> { case(link, pushState) =>
          def script = if (pushState) {
            s"""${aElem.getName}.onclick =  function(event) {
               |  if(!event.shiftKey && !event.ctrlKey && !event.metaKey) {
               |    jpro.jproGotoURL(\"${link.replace("\"", "\\\"")}\"); event.preventDefault();
               |  }
               |};""".stripMargin
          } else s"${aElem.getName}.onclick = null;"
          webapi.executeScript(
            s"""${aElem.getName}.href = '${link.replace(" ", "%20").replace("'", "\\'")}';
               |$script
               |""".stripMargin)
        }

        external --> { external =>
          if(external) {
            webapi.executeScript(
              s"""${aElem.getName}.target = '_blank';""".stripMargin)
          } else {
            webapi.executeScript(
              s"""${aElem.getName}.removeAttribute("target");""".stripMargin)
          }
        }
        text --> { x =>
          // have to escape ' in text
          if(x.isDefined) {
            val escapedText = x.get.replace("'","\\'")
            webapi.executeScript(
              s"""${aElem.getName}.title = '$escapedText';""".stripMargin)
          } else {
            webapi.executeScript(
              s"""${aElem.getName}.removeAttribute("title");""".stripMargin)
          }
        }

      })
    }

  }


  private def setLinkSimple(url: String, text: Option[String], pushState: Boolean, external: Boolean)(theNode: Node) = {
    if(url == null) {
      throw new NullPointerException("url was null")
    }
    if(text == Some(null)) {
      throw new NullPointerException("text was provided but null")
    }
    if(!WebAPI.isBrowser) {
      import LinkDesktop._
      theNode.setNewLink(url,text,pushState,
        external)
    } else {
      import LinkJPro._
      theNode.setNewLink(url, text, pushState,
        external)
    }
  }

  def setImageViewDescription(view: ImageView, description: String): Unit = {
    view.setAccessibleRoleDescription(description)
  }
}
