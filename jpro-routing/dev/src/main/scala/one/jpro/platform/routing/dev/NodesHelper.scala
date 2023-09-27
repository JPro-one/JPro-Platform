package one.jpro.platform.routing.dev

import simplefx.core._
import simplefx.all._
import simplefx.experimental._
import simplefx.util.Predef.extension
object NodesHelper {
  @extension class ExtNode(node: Node) {
    val isParent = node.isInstanceOf[Parent]
    lazy val parent = node.asInstanceOf[Parent]

    @Bind var treeSize: Int = <-- {
      if(!isParent) {
        1
      } else {
        1 + parent.childrenUnmodifiable.map(_.treeSize).sum
      }
    }
    @Bind var visibleTreeSize: Int = <--{
      if(!visible) {
        0
      } else if(!isParent) {
        1
      } else {
        1 + parent.childrenUnmodifiable.map(_.visibleTreeSize).sum
      }
    }
  }

}
