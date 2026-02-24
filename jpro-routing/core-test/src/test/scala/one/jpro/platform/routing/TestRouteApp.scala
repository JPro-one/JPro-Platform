package one.jpro.platform.routing

import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.junit.jupiter.api.{BeforeAll, Test}
import simplefx.core._

import scala.concurrent.Await

object TestRouteApp {
    @BeforeAll
    def setup(): Unit = {
        Await.result(simplefx.cores.initializeCore(), (second))
    }
}
class TestRouteApp {

    @Test
    def testAccessingSessionManager1(): Unit = inFX {
        new RouteApp {
            override def createRoute(): Route = {
                assert(getSessionManager() != null)
                Route.get("/", r => Response.node(new StackPane()))
            }
        }.start(new Stage())
    }

    @Test
    def testAccessingSessionManager2(): Unit = inFX {
        new RouteApp {
            override def createRoute(): Route = {
                assert(LinkUtil.getSessionManager(getRouteNode()) != null)
                Route.get("/", r => Response.node(new StackPane()))
            }
        }.start(new Stage())
    }

    @Test
    def testAccessingSessionManager3(): Unit = inFX {
        new RouteApp {
            override def createRoute(): Route = {
                assert(getRouteNode().getSessionManager() != null)
                Route.get("/", r => Response.node(new StackPane()))
            }
        }.start(new Stage())
    }
}
