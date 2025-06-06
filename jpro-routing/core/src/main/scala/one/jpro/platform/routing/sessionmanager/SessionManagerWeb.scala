package one.jpro.platform.routing.sessionmanager

import com.jpro.webapi.WebAPI
import one.jpro.platform.routing.{Redirect, Response, ResponseResult, RouteNode, View}
import org.slf4j.{Logger, LoggerFactory}
import simplefx.all._


class SessionManagerWeb(val webApp: RouteNode, val webAPI: WebAPI) extends SessionManager { THIS =>
  assert(webApp != null, "webApp must not be null!")

  private lazy val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  val container = new StackPane
  webApp <++ container

  def goBack(): Unit = {
    webAPI.js().eval("history.go(-1);")
  }

  def goForward(): Unit = {
    webAPI.js().eval("history.go(1);")
  }

  if(webAPI != null) { // somtetimes webAPI is null, for example when crawling
    webAPI.addInstanceCloseListener(() => {
      // if the session only has redirects, the view is null
      if (THIS.view != null) {
        THIS.view.onClose()
        THIS.view.setSessionManager(null)
        markViewCollectable(THIS.view)
      }
    })
  }

  def gotoURL(_url: String, x: ResponseResult, pushState: Boolean): Response = {
    assert(x != null, "Response was null for url: " + _url)
    val url = _url
    x match {
      case Redirect(url) =>
        if(isExternal(url)) {
          this.asInstanceOf[SessionManagerWeb].webAPI.js().eval(s"""window.location.href = "$url";""")
          Response.fromResult(x)
        } else {
          gotoURL(url)
        }
      case view: View =>
        this.url = url
        view.setSessionManager(this)
        view.url = url

        view.isMobile = webAPI.isMobile

        container.children = List(view.realContent)
        if(THIS.view != null && THIS.view != view) {
          THIS.view.onClose()
          THIS.view.setSessionManager(null)
          markViewCollectable(THIS.view, view)
        }
        THIS.view = view


        if(pushState) {
          //webAPI.js().eval(s"""var doc = document.documentElement;
          //                        |history.replaceState({
          //                        |marker: "goto",
          //                        |scrollTop: (window.pageYOffset || doc.scrollTop)  - (doc.clientTop || 0)
          //                        |}, null, null);
          //                        |""".stripMargin)
          webAPI.js().eval(s"""history.pushState(null, null, "${view.url.replace("\"","\\\"")}");""")
        }
        val initialState = if(view.saveScrollPosition) "{saveScroll: true}" else "{saveScroll: false}"

        webAPI.js().eval(
          """var scrollY = 0;
            |if(history.state != null) {
            |  scrollY = history.state.scrollTop || 0;
            |}
            |scroll(0,scrollY)
          """.stripMargin)
        webAPI.js().eval(s"""document.getElementsByTagName("jpro-app")[0].sfxelem.setFXHeight(${!view.fullscreen})""")
        webAPI.js().eval(s"""document.title = "${Option(view.title).getOrElse("").replace("\"","\\\"")}";""")
        webAPI.js().eval(s"""document.querySelector('meta[name="description"]').setAttribute("content", "${Option(view.description).getOrElse("").replace("\"","\\\"")}");""")
        webAPI.js().eval(s"history.replaceState($initialState, null, null)")
        Response.fromResult(x)
    }
  }

  def gotoFullEncodedURL(x: String, pushState: Boolean = true): Response = {
    // We no longer decode - we should only process proper URLs
    // If the URL is not proper, we will get a warning when creating the Request.
    gotoURL(x, pushState)
  }

  def start(): Response = {
    logger.debug("registering popstate")
    webAPI.registerJavaFunction("popstatejava", (s: String) => {
      gotoFullEncodedURL(s.drop(1).dropRight(1).replace("\\\"", "\""))
    })
    webAPI.registerJavaFunction("jproGotoURL", (s: String) => {
      gotoURL(s.drop(1).dropRight(1).replace("\\\"", "\""))
    })

    webAPI.js().eval(
      s"""var scheduled = false
         |window.addEventListener("scroll", function(e) {
         |  if(!scheduled) {
         |    window.setTimeout(function(){
         |      scheduled = false;
         |      var doc = document.documentElement;
         |      if(history.state != null && history.state.saveScroll) {
         |        history.replaceState({
         |          marker: "pop",
         |          saveScroll: true,
         |          scrollTop: (window.pageYOffset || doc.scrollTop)  - (doc.clientTop || 0)
         |        }, null, null);
         |      }
         |    },300);
         |  }
         |  scheduled = true;
         |});
         |""".stripMargin)
    // Safari scrollsUp on popstate, when going back form external page (when scrollRestoration is manual)
    // when this happens, the ws-connection get's canceled by safari, which tells us,
    // that we have to move back to the saved scrollPosition.
    // we have to check, whether the ws is still alive, shortly after popstate.
    // we have to save the old scrollY immediately, so we remember it faster, than the safari resets it.
    webAPI.js().eval("""
                           |window.addEventListener('popstate', function(e) {
                           |  window.setTimeout(function(){console.log("popstate called!")},3000);
                           |  var scrollY = 0;
                           |  if(history.state != null) {
                           |    scrollY = history.state.scrollTop || 0;
                           |  }
                           |  window.setTimeout(function(){
                           |    if(!document.getElementsByTagName("jpro-app")[0].jproimpl.isConnected) {
                           |      window.setTimeout(function(){console.log("resetting scroly (wasn't connected")},3000);
                           |      console.log("scrollY: " + scrollY);
                           |      scroll(0,scrollY);
                           |    }
                           |  }, 1);
                           |  jpro.popstatejava(location.href);
                           |});""".stripMargin)
    webAPI.js().eval(
      // Back off, browser, I got this...
      """if ('scrollRestoration' in history) {
        |  history.scrollRestoration = 'manual';
        |}
      """.stripMargin)

    gotoFullEncodedURL(webAPI.getBrowserURL, false)
  }
}
