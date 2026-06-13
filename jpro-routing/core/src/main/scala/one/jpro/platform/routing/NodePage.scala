package one.jpro.platform.routing

import simplefx.all._

trait NodePage extends Page { this: Node =>
  final override def content: NodePage with Node = this
}
