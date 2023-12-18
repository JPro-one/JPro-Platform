package one.jpro.platform.routing

import simplefx.all

object Filters {
  def FullscreenFilter(fullscreenValue: Boolean): Filter = { route => { request =>
      val r = route.apply(request)

      Response(r.future.map {
        case x: View =>
          new View {
            override def title: String = x.title
            override def description: String = x.description
            override def content: all.Node = x.realContent

            override def fullscreen: Boolean = fullscreenValue
          }
        case x => x
      })
    }
  }

}
