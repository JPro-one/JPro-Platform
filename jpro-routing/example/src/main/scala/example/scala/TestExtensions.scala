package example.scala


import one.jpro.platform.routing.Route.{get, redirect}
import com.jpro.webapi.WebAPI
import simplefx.all._
import simplefx.core._
import simplefx.experimental._

import java.util.function.Supplier
import one.jpro.platform.routing.extensions.linkheader.LinkHeaderFilter.Link
import fr.brouillard.oss.cssfx.CSSFX
import one.jpro.platform.routing.{Filters, LinkUtil, Redirect, Request, Response, Route, RouteNode, RouteUtils}
import one.jpro.platform.routing.extensions.linkheader.LinkHeaderFilter
import one.jpro.platform.routing.filter.container.ContainerFactory
import one.jpro.platform.routing.sessionmanager.SessionManager

class TestExtensionsApp(stage: Stage) extends RouteNode(stage) {
  stylesheets <++ "/com/jpro/routing/extensions/linkheader/css/linkheader.css"
  
  setRoute(
    Route.empty() /* StartRoute? */
      .and(redirect("/", "/home"))
      .and(get("/home", (r) => Response.node(new Label("HOME"))))
      .and(get("/secret", (r) => Response.node(new Label("SECRET"))))
      .filter(LinkHeaderFilter.create(Link("HOME","/home"), Link("SECRET","/secret")))
  )

  CSSFX.start()
}


object TestExtensions extends App
@SimpleFXApp class TestExtensions {
  val app = new TestExtensionsApp(stage)
  if(WebAPI.isBrowser) {
    root = app
  } else {
    scene = new Scene(app, 1400,800)
  }
  app.start(SessionManager.getDefault(app,stage))
}


