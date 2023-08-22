package one.jpro.routing

import one.jpro.routing.filter.container.ContainerFactory
import simplefx.all
import simplefx.all._
import simplefx.core._
import simplefx.experimental._
import java.util.function.Function

import java.util.function.Supplier

object RouteUtils {

  @deprecated
  val EmptyRoute: Route = (x) => null

  def redirect(path: String, to: String): Route = get(path, (r) => Redirect(to))

  def getFuture(path: String, f: Function[Request, FXFuture[Response]]): Route = (request: Request) => if (request.path == path) f.apply(request) else FXFuture.unit(null)

  def get(path: String, f: Function[Request, Response]): Route = (request: Request) => if (request.path == path) FXFuture.unit(f.apply(request)) else null

  def getNodeFuture(path: String, node: Function[Request, FXFuture[Node]]): Route = (request: Request) => if (request.path == path) node.apply(request).map(node => viewFromNode(node)) else FXFuture.unit(null)

  def getNode(path: String, node: Function[Request, Node]): Route = (request: Request) => if (request.path == path) FXFuture.unit(viewFromNode(node.apply(request))) else null


  def transitionFilter(seconds: Double): Filter = route => { request => {
    route.apply(request).map{
      case x: View =>
        val oldNode = request.oldContent.get()
        val newNode = x.realContent
        val t = (seconds s)
        if(oldNode == null) {
          x
        } else {
          oldNode.opacity = 1.0
          newNode.opacity = 0.0
          oldNode.opacity := 0.0 in t
          newNode.opacity := 1.0 in t
          val res = new StackPane(oldNode,newNode)
          in(t) --> {res.children = List(newNode)}
          x.mapContent(x => res)
        }
      case x => x
    }
  }}
  def sideTransitionFilter(seconds: Double): Filter = route => { request => {
    route.apply(request).map{
      case x: View =>
        val oldNode = request.oldContent.get()
        val newNode = x.realContent
        val t = (seconds s)
        if(oldNode == null) {
          x.mapContent(x => x)
        } else {
          val startTime: Time = systemTime
          def timeLeft: Time = (startTime + (seconds * second)) - time
          def progress: Double = 1.0 - (timeLeft / (seconds * second))
          val res = new StackPane(oldNode,newNode)
          val finishedB: B[Boolean] = Bindable(false)
          when(!finishedB && timeLeft > (0 s)) ==> {
            oldNode.translateX <-- (-progress * res.width)
            newNode.translateX <-- ((1 - progress) * res.width)
          }
          onceWhen(timeLeft <= (0. s)) --> {
            oldNode.translateX = 0
            newNode.translateX = 0
            finishedB := true
          }
          in(t) --> {res.children = List(newNode)}
          x.mapContent(x => res)
        }
      case x => x
    }
  }}

  def mapViewFilter(request: Request, f: Node => Node): View = ???

  def viewFromNode(x: Node): View = new View {
    override def title: String = "view-from-node"
    override def description: String = ""
    override def content: all.Node = x
  }




  abstract class SFXContainerFactory extends ContainerFactory {
    def isContainer(x: Node): Boolean
    override def setContent(c: Node, x: Node): Unit = c.asInstanceOf[MyContainer].content = x
    override def getContent(c: Node): Node = {
      c.asInstanceOf[MyContainer].content
    }
    override def setRequest(c: Node, r: Request): Unit = c.asInstanceOf[MyContainer].request = r

    type MyContainer <: Container with Node
    trait Container { x: Node =>
      @Bind var request: Request = null
      @Bind var content: Node = null
    }

  }
}
