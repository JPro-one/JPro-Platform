package one.jpro.platform.routing.filter.container

import one.jpro.platform.routing.{Filter, Request, Response, View}
import org.slf4j.{Logger, LoggerFactory}
import simplefx.all._

import java.util.function.Supplier

object ContainerFilter {

  private lazy val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  private val factoryKey = new Object() // It's correct, that this is outside the method.
  def create(supplier: Supplier[Container], clazz: Class[_]): Filter = {
    create(new ContainerFactory {
      override def isContainer(x:  Node): Boolean = {
        x.getProperties.get(factoryKey) == clazz
      }
      override def createContainer(): _root_.javafx.scene.Node = {
        val res = supplier.get().asInstanceOf[Node]
        assert(res.getClass == clazz, s"The class of the container must be $clazz but was ${res.getClass}")
        res.getProperties.put(factoryKey, clazz)
        res
      }
      override def setContent(c:  _root_.javafx.scene.Node, x:  _root_.javafx.scene.Node): Unit = {
        c.asInstanceOf[Container].setContent(x)
      }
      override def getContent(c:  _root_.javafx.scene.Node): _root_.javafx.scene.Node = {
        c.asInstanceOf[Container].getContent()
      }
      override def setRequest(c:  _root_.javafx.scene.Node, r:  Request): Unit = {
        c.asInstanceOf[Container].setRequest(r)
      }
    })
  }

  def create[A <: javafx.scene.Node](containerLogic: ContainerFactory): Filter = route => request => {
    var container: Node = null
    val request2: Request = {
      val oldContentV = request.getOldContent().get()
      logger.debug(s"oldContentV = $oldContentV")
      if (oldContentV != null && containerLogic.isContainer(oldContentV)) {
        logger.debug(s"Found old container")
        container = oldContentV
        request.mapContent(x => containerLogic.getContent(container))
      } else {
        logger.debug(s"No old container")
        request
      }
    }
    val r = route(request2)
    Response(r.future.map {
      case view: View =>
        if (container == null) {
          container = containerLogic.createContainer()
        }
        view.mapContent(x => {
          containerLogic.setRequest(container, request)
          containerLogic.setContent(container, view.realContent)
          container
        })
      case x => x
    })
  }
}
