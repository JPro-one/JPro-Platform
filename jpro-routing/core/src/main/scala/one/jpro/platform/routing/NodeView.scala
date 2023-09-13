package one.jpro.platform.routing

import simplefx.all._

trait NodeView extends View { this: Node =>
  final override def content: NodeView with Node = this
}
