package one.jpro.platform.routing

import javafx.scene.control.Label
import javafx.scene.image.{Image, ImageView}
import javafx.scene.layout.{StackPane, VBox}
import javafx.scene.text.{Text, TextAlignment, TextFlow}

class NotFoundPage extends StackPane {

  getStylesheets.add("/one/jpro/platform/routing/css/not-found-page.css")
  getStyleClass.add("not-found-page")

  val image = new ImageView(new Image(getClass.getResource("/one/jpro/platform/routing/images/404.png").toExternalForm))
  val flow = new TextFlow()
  val title = new Label("We couldn’t find a page here.")
  val text = new Text("\n\nIt may have been moved or be temporarily unavailable.\nPlease double-check the URL, or go ")
  val link = new Text("back to home")
  val dot = new Text(".")

  image.setFitWidth(500)
  image.setFitHeight(175)

  flow.getStyleClass.add("not-found-page-text")
  title.getStyleClass.add("not-found-page-title")

  flow.getChildren.addAll(title, text, link, dot)
  flow.setTextAlignment(TextAlignment.CENTER)

  LinkUtil.setLink(link, "/")

  getChildren.add(new VBox(image, flow) {
    getStyleClass.add("not-found-page-container")
  })
}
