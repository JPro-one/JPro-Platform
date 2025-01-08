package one.jpro.platform.routing

import com.jpro.webapi.WebAPI
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.stage.{Stage, StageStyle}
import one.jpro.platform.routing.sessionmanager.SessionManager
import simplefx.core._
import simplefx.all._
import simplefx.experimental.FXFuture

abstract class RouteApp extends Application {

  private var _stage: Stage = null
  private var routeNode: RouteNode = null
  private var _scene: Scene = null
  private var _sessionManager: SessionManager = null
  def stageStyle(): StageStyle = StageStyle.DECORATED
  def getStage(): Stage = _stage
  def getScene(): Scene = _scene
  def getSessionManager(): SessionManager = _sessionManager
  def getRouteNode(): RouteNode = routeNode
  def getWebAPI(): WebAPI = if(WebAPI.isBrowser) WebAPI.getWebAPI(getStage()) else null

  override def start(stage: Stage): Unit = {
    startFuture(stage)
  }
  def startFuture(stage: Stage): Response = {
    _stage = stage
    stage.initStyle(stageStyle())
    routeNode = new RouteNode(stage)

    // Add node between RouteNode and Scene, so Popups work correctly with ScenicView
    val root = new StackPane(routeNode)

    _scene = new Scene(root, 1400, 800)
    stage.setScene(_scene)
    _sessionManager = SessionManager.getDefault(routeNode, stage)
    routeNode.setRoute(createRoute())
    stage.show()
    routeNode.start(_sessionManager)
  }

  def createRoute(): Route
}
