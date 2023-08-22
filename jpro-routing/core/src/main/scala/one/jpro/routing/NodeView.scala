package one.jpro.routing

import simplefx.core._
import simplefx.all._

trait NodeView extends View { this: Node =>
  final override def content = this
}
