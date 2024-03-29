package example.scala

import one.jpro.platform.routing.Route.{get, redirect}
import com.jpro.webapi.WebAPI
import one.jpro.platform.routing.{Filters, LinkUtil, Request, Response, Route, RouteNode, RouteUtils}
import one.jpro.platform.routing.sessionmanager.SessionManager
import simplefx.all._
import simplefx.core._
import one.jpro.platform.routing.dev.DevFilter

object HeaderFactory extends RouteUtils.SFXContainerFactory {

  override def isContainer(x: Node): Boolean = x.isInstanceOf[MyContainer]
  override def createContainer() = new MyContainer
  class MyContainer extends VBox with Container {

    this <++ new Label {
      text <-- (if(request != null) request.getOriginalPath() else "---")
    }
    this <++ new StackPane {
      javafx.scene.layout.VBox.setVgrow(this, Priority.ALWAYS)
      children <-- (if(content != null) List(content) else Nil)
    }
  }
}

class ColorTransition(stage: Stage) extends RouteNode(stage) {
  private def format(v: Double) = {
    val in = Integer.toHexString((v * 255).round.toInt)
    if (in.length == 1) "0" + in
    else in
  }
  def toHexString(value: Color): String = "#" + (format(value.getRed) + format(value.getGreen) + format(value.getBlue) + format(value.getOpacity)).toUpperCase
  def gen(x: String, next: String, color: Color): Response = Response.node(new StackPane {
    LinkUtil.setLink(this,next)
    this <++ new Label(x) {
      style = "-fx-font-size: 36px;"
    }
    style = s"-fx-background-color: ${toHexString(color)};"
  })
  /* Util rename into LinkUtil */
  setRoute(
    Route.empty() /* StartRoute? */
      .and(redirect("/", "/green"))
      .and(get("/green", r => gen("Green","/red", Color.GREEN)))
      .and(get("/red", r => gen("Red", "/blue", Color.RED)))
      .and(get("/blue", r => gen("Blue", "/yellow", Color.BLUE)))
      .and(get("/yellow", r => gen("Yellow", "/red", Color.YELLOW)))
      .path("/colors",
        Route.empty()
          .and(get("/green", r => gen("Green","./red", Color.GREEN)))
          .and(get("/red", r => gen("Red", "./green", Color.RED)))
      )
      // Alternative names: with, apply, map, use, modify, wrap, transform
      // enhancer, operations
      .filter(Filters.FullscreenFilter(true))
      .filter(RouteUtils.sideTransitionFilter(1))
      .filter(DevFilter.create())
      //.filter(RouteUtils.containerFilter(HeaderFactory) compose RouteUtils.containerFilter(HeaderFactory))
      //.filter(RouteUtils.containerFilter(HeaderFactory))
  )
}


object ColorTransitionApp extends App
@SimpleFXApp class ColorTransitionApp {
  val app = new ColorTransition(stage)
  if(WebAPI.isBrowser) {
    root = app
  } else {
    scene = new Scene(app, 1400,800)
  }
  app.start(SessionManager.getDefault(app,stage))
}
